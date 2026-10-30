package com.kangrio.gstore.patch.apps

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.kangrio.gstore.BuildConfig
import com.kangrio.gstore.data.installer.AppInstaller
import com.kangrio.gstore.data.model.Installer
import com.kangrio.gstore.patch.ConstantsPatch
import com.kangrio.gstore.patch.morphe.MorpheApp
import com.kangrio.gstore.patch.morphe.MorpheUtils
import com.kangrio.gstore.util.PackageUtil

object PackageUtilPatch {
    fun canRequestPackageInstalls(context: Context): Boolean {
        val currentInstaller = AppInstaller.getCurrentInstaller(context)
        return when (currentInstaller) {
            Installer.ROOT -> AppInstaller.hasRootAccess()
            Installer.AM -> AppInstaller.hasAppManager(context)
            Installer.SHIZUKU -> AppInstaller.hasShizukuOrSui(context) && AppInstaller.hasShizukuPerm(context)
            else -> false
        }
    }

    fun getPatchVersionCode(context: Context, packageName: String): Int {
        try {
            val packageInfo = PackageUtil.getPackageInfo(context, packageName, PackageManager.GET_META_DATA)
            return packageInfo.applicationInfo?.metaData?.getInt(ConstantsPatch.META_DATA_PATCH_VERSION_CODE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        } catch (_: Throwable) {
            return Int.MIN_VALUE
        }
    }

    fun isHasNewerPatch(context: Context, packageName: String): Boolean {
        return BuildConfig.PATCH_VERSION_CODE > getPatchVersionCode(context, packageName)
    }

    fun isUpdatable(context: Context, packageName: String, versionCode: Long): Boolean {
        return try {
            // prebuild apk patched with morphe-patches
            if (packageName in MorpheApp.apps.keys && isMorphePatch(context, packageName)) {
                val currentVersion = morpheVersion(context, packageName)
                return MorpheUtils.isLatestPatchAvailable(currentVersion)
            }

            // original apps
            try {
                if (isHasNewerPatch(context, packageName)) {
                    return true
                }

                val packageInfo = PackageUtil.getPackageInfo(context, packageName)
                return versionCode > PackageInfoCompat.getLongVersionCode(packageInfo)
            } catch (_: PackageManager.NameNotFoundException) {
                return false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun morpheVersion(context: Context, packageName: String): String {
        try {
            val packageInfo = PackageUtil.getPackageInfo(context, packageName, PackageManager.GET_META_DATA)
            return packageInfo.applicationInfo?.metaData?.getString("morphe_version", ) ?: ""
        } catch (_: Throwable) {
            return ""
        }
    }

    fun isMorphePatch(context: Context, packageName: String): Boolean {
        return try {
            val packageInfo = PackageUtil.getPackageInfo(context, packageName, PackageManager.GET_META_DATA)
            packageInfo.applicationInfo?.metaData?.containsKey("morphe_version") == true
        } catch (_: Throwable) {
            false
        }
    }
}