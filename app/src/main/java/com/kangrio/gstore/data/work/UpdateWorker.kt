/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.work

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kangrio.gstore.Constants
import com.kangrio.gstore.extensions.TAG
import com.kangrio.gstore.extensions.isGrapheneOS
import com.kangrio.gstore.extensions.isHyperOS
import com.kangrio.gstore.extensions.isIgnoringBatteryOptimizations
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.kangrio.gstore.BuildConfig
import com.kangrio.gstore.data.helper.DownloadHelper
import com.kangrio.gstore.data.helper.UpdateHelper
import com.kangrio.gstore.data.installer.AppInstaller
import com.kangrio.gstore.data.model.BuildType
import com.kangrio.gstore.data.model.SelfUpdate
import com.kangrio.gstore.data.model.UpdateMode
import com.kangrio.gstore.data.network.HttpClient
import com.kangrio.gstore.data.providers.AccountProvider
import com.kangrio.gstore.data.providers.AuthProvider
import com.kangrio.gstore.data.providers.BlacklistProvider
import com.kangrio.gstore.data.providers.GoogleAccountTokenProvider
import com.kangrio.gstore.data.room.update.IgnoredUpdateDao
import com.kangrio.gstore.data.room.update.Update
import com.kangrio.gstore.data.room.update.UpdateDao
import com.kangrio.gstore.data.room.update.isIgnoredBy
import com.kangrio.gstore.patch.UpdateWorkerPatch
import com.kangrio.gstore.patch.data.GitHubRelease
import com.kangrio.gstore.patch.morphe.MorpheApp
import com.kangrio.gstore.patch.morphe.MorphePatcherBridge
import com.kangrio.gstore.util.CertUtil
import com.kangrio.gstore.util.NotificationUtil
import com.kangrio.gstore.util.PackageUtil
import com.kangrio.gstore.util.Preferences
import com.kangrio.gstore.util.Preferences.PREFERENCE_SELF_UPDATE_ENABLED
import com.kangrio.gstore.util.Preferences.PREFERENCE_UPDATES_AUTO
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * A worker that drives periodic app-update checks. The repeat interval is configurable
 * by the user, defaulting to 3 hours with a flex time of 30 minutes.
 *
 * Aurora Store's own update is fetched from the bundled release/nightly feed and added
 * to the regular update list (see [getSelfUpdate]); from there it reuses the standard
 * download + install pipeline. It is never auto-installed silently — the user triggers
 * it from the Updates tab.
 *
 * Avoid using this worker directly and prefer using [UpdateHelper] instead.
 * @see AuthWorker
 */
@HiltWorker
class UpdateWorker @AssistedInject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val blacklistProvider: BlacklistProvider,
    private val updateDao: UpdateDao,
    private val ignoredUpdateDao: IgnoredUpdateDao,
    private val downloadHelper: DownloadHelper,
    private val authProvider: AuthProvider,
    tokenProvider: GoogleAccountTokenProvider,
    private val appDetailsHelper: AppDetailsHelper,
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : AuthWorker(authProvider, tokenProvider, context, workerParams) {

    companion object {
        private const val NOTIFICATION_ID = 100
    }

    /**
     * `true` when the build supports self-update ([PackageUtil.isSelfUpdateSupported])
     * and the user hasn't opted out via the Settings toggle. Read each check so flipping
     * the preference takes effect on the next run.
     */
    private val canSelfUpdate: Boolean
        get() = PackageUtil.isSelfUpdateSupported(context) &&
            Preferences.getBoolean(context, PREFERENCE_SELF_UPDATE_ENABLED, true)

    private val isAuroraOnlyFilterEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_AURORA_ONLY, false)

    private val isFDroidFilterEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_FILTER_FDROID)

    private val isExtendedUpdateEnabled: Boolean
        get() = Preferences.getBoolean(context, Preferences.PREFERENCE_UPDATES_EXTENDED)

    override suspend fun doWork(): Result {
        super.doWork()

        Log.i(TAG, "Checking for app updates")
        val updateMode = UpdateMode.entries[
            inputData.getInt(
                UpdateHelper.UPDATE_MODE,
                Preferences.getInteger(
                    context,
                    PREFERENCE_UPDATES_AUTO,
                    UpdateMode.CHECK_AND_INSTALL.ordinal
                )
            )
        ]

        if (updateMode == UpdateMode.DISABLED || !AccountProvider.isLoggedIn(context)) {
            Log.i(TAG, "Updates are disabled, bailing out!")
            return Result.failure()
        }

        if (!authProvider.isSavedAuthDataValid()) {
            Log.i(TAG, "AuthData is not valid, retrying later!")
            return Result.retry()
        }

        try {
            // Ignored updates are still written to the database so the Updates screen can
            // list them under "Ignored"; they are dropped here so they reach neither the
            // notification nor the auto-install below.
            val ignoreRules = ignoredUpdateDao.ignoredUpdates().first().associateBy {
                it.packageName
            }

            val allUpdates = checkUpdates()
                .also { updateDao.insertUpdates(it) }
                .filter { if (!isExtendedUpdateEnabled) it.hasValidCert else true }
                .filterNot { it.isIgnoredBy(ignoreRules) }

            // Incompatible updates (e.g. system app updates on HyperOS / GrapheneOS) are
            // surfaced in the UI but excluded from notifications and auto-install.
            val updates = allUpdates.filterNot { it.isIncompatible }

            if (updates.isEmpty() || updateMode == UpdateMode.CHECK_ONLY) {
                Log.i(TAG, "Found ${updates.size} updates")
                return Result.success()
            }

            // Notify and exit if we are only checking for updates or if battery optimizations are enabled
            if (updateMode == UpdateMode.CHECK_AND_NOTIFY ||
                !context.isIgnoringBatteryOptimizations()
            ) {
                Log.i(TAG, "Found  ${updates.size} updates, notifying!")
                notifyUpdates(updates)
                return Result.success()
            }

            // Clean the update list to prepare for installing. Aurora Store's own update
            // is never installed silently — the user triggers it from the Updates tab.
            val filteredUpdates = updates
                .filter { it.hasValidCert }
                .filterNot { it.isSelfUpdate(context) }
                .partition {
                    AppInstaller.canInstallSilently(context, it.packageName, it.targetSdk)
                }

            // Notify about apps that cannot be auto-updated
            if (filteredUpdates.second.isNotEmpty()) {
                Log.i(
                    TAG,
                    "Found  ${updates.size} updates out of which ${filteredUpdates.second.size} cannot be auto-updated"
                )
                notifyUpdates(filteredUpdates.second)
            }

            // Trigger download for apps if they can be auto-updated
            filteredUpdates.first.forEach { downloadHelper.enqueueUpdate(it) }

            return Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to fetch updates", exception)
            return Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        NotificationUtil.getUpdateNotification(context)
    )

    /**
     * Checks and returns updates for all possible apps
     */
    private suspend fun checkUpdates(): List<Update> {
        return withContext(Dispatchers.IO) {
            val packages = PackageUtil.getAllValidPackages(context)
                .filterNot { blacklistProvider.isBlacklisted(it.packageName) }
                .filter { if (!isExtendedUpdateEnabled) it.applicationInfo!!.enabled else true }

            // Filter out packages based on user's preferences
            val installerFilters = Preferences.getStringSet(
                context,
                Preferences.PREFERENCE_FILTER_INSTALLERS
            )
            val filteredPackages = if (isAuroraOnlyFilterEnabled) {
                packages.filter { CertUtil.isAuroraStoreApp(context, it.packageName) }
            } else {
                packages.filterNot { pkg ->
                    if (isFDroidFilterEnabled && CertUtil.isFDroidApp(context, pkg.packageName)) {
                        return@filterNot true
                    }
                    if (installerFilters.isNotEmpty()) {
                        val installer = PackageUtil.getInstallerPackageName(
                            context,
                            pkg.packageName
                        )
                        installer != null && installer in installerFilters
                    } else {
                        false
                    }
                }
            }.map { it.packageName }

            // HyperOS and GrapheneOS block third-party updates of pristine system apps.
            val osBlocksSystemAppUpdates = isHyperOS || isGrapheneOS
            val pristineSystemPackages: Set<String> = if (osBlocksSystemAppUpdates) {
                packages.filter {
                    val flags = it.applicationInfo?.flags ?: 0
                    flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                        flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
                }.map { it.packageName }.toSet()
            } else {
                emptySet()
            }

            val updates = appDetailsHelper.getAppByPackageName(filteredPackages)
                .filter { it.displayName.isNotEmpty() }
                .filter { PackageUtil.isUpdatable(context, it.packageName, it.versionCode) }
                .toMutableList()

            val updatesNew = updates
                .mapNotNull { getMorpheVersion(it) }
                .filter { PackageUtil.isUpdatable(context, it.packageName, it.versionCode) }
            updates.clear()
            updates.addAll(updatesNew)

            // Aurora Store's own update comes from the feed, not Play. When one is
            // offered, add it; otherwise drop any stale self-update row. This is the
            // cleanup path for the row (nightly self-updates are exempt from
            // deleteInvalidUpdates, and the install event isn't delivered reliably when
            // the app replaces itself), so a previously shown self-update doesn't linger
            // after we've already updated to it.
            val selfUpdate = if (canSelfUpdate) getSelfUpdate() else null
            if (selfUpdate != null) {
                updates.add(selfUpdate)
            } else {
                updateDao.delete(context.packageName)
            }
            UpdateWorkerPatch.getMicroGUpdate(context)?.let { updates.addAll(it) }

            return@withContext updates.map {
                Update.fromApp(
                    context,
                    it,
                    isIncompatible = it.packageName in pristineSystemPackages
                )
            }.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
        }
    }

    /**
     * Fetches Aurora Store's own update from the bundled release/nightly feed and maps
     * it onto an [App] so it joins the regular update list. Nightly version codes never
     * bump, so newness is decided by the build timestamp there; release uses the version
     * code. Best-effort: any failure logs and yields no update.
     */
    private suspend fun getSelfUpdate(): App? = withContext(Dispatchers.IO) {
        val updateUrl = when (BuildType.CURRENT) {
            BuildType.RELEASE -> getSelfUpdateUrl(BuildType.CURRENT)
            BuildType.NIGHTLY -> getSelfUpdateUrl(BuildType.NIGHTLY)
            else -> {
                Log.i(TAG, "Self-updates are not available for this build!")
                return@withContext null
            }
        }

        try {
            val selfUpdate = httpClient.call(updateUrl).use {
                json.decodeFromString<SelfUpdate>(it.body.string())
            }

            val isNewer = when (BuildType.CURRENT) {
                BuildType.RELEASE -> selfUpdate.versionCode > BuildConfig.VERSION_CODE
                BuildType.NIGHTLY -> selfUpdate.timestamp > BuildConfig.BUILD_TIMESTAMP
                else -> false
            }

            if (isNewer && selfUpdate.downloadUrl.isNotBlank()) {
                return@withContext selfUpdate.toApp(context)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to check self-updates", exception)
        }

        Log.i(TAG, "No self-updates found!")
        return@withContext null
    }

    private fun notifyUpdates(updates: List<Update>) {
        with(context.getSystemService<NotificationManager>()!!) {
            notify(
                NOTIFICATION_ID,
                NotificationUtil.getUpdateNotification(context, updates)
            )
        }
    }

    private suspend fun getSelfUpdateUrl(buildType: BuildType): String = withContext(Dispatchers.IO) {
        try {
            val releases = httpClient.call(Constants.SELF_UPDATE_URL).use {
                json.decodeFromString<List<GitHubRelease>>(it.body.string())
            }

            when (buildType) {
                BuildType.RELEASE -> releases.first { !it.prerelease }.assets.first { it.name.endsWith("release_build.json") }.browser_download_url
                BuildType.NIGHTLY -> releases.first { it.prerelease }.assets.first { it.name.endsWith("nightly_build.json") }.browser_download_url
                else -> {
                    Log.i(TAG, "Self-updates are not available for this build!")
                    ""
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to check self-updates", e)
            ""
        }
    }

    private fun getMorpheVersion(app: App): App? {
        if (app.packageName !in MorpheApp.apps.keys) return app

        val compatibleVersions = MorphePatcherBridge.latestCompatibleVersion(app.packageName)
        if (compatibleVersions.isEmpty()) return null
        return app.copy(
            versionName = compatibleVersions.keys.first(),
            versionCode = compatibleVersions.values.first()
        )
    }
}
