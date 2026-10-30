/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.room

import android.content.Context
import androidx.room.Room
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_10_11
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_11_12
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_12_13
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_1_2
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_2_3
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_3_4
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_4_5
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_5_6
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_6_7
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_7_8
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_8_9
import com.kangrio.gstore.data.room.MigrationHelper.MIGRATION_9_10
import com.kangrio.gstore.data.room.account.AccountConverter
import com.kangrio.gstore.data.room.account.AccountDao
import com.kangrio.gstore.data.room.account.AppAccountBindingDao
import com.kangrio.gstore.data.room.download.DownloadConverter
import com.kangrio.gstore.data.room.download.DownloadDao
import com.kangrio.gstore.data.room.exodus.TrackerDao
import com.kangrio.gstore.data.room.favourite.FavouriteDao
import com.kangrio.gstore.data.room.notification.NotificationDao
import com.kangrio.gstore.data.room.review.ReviewDao
import com.kangrio.gstore.data.room.update.IgnoredUpdateDao
import com.kangrio.gstore.data.room.update.UpdateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    private const val DATABASE = "aurora_database"

    @Singleton
    @Provides
    fun providesRoomInstance(
        @ApplicationContext context: Context,
        downloadConverter: DownloadConverter,
        accountConverter: AccountConverter
    ): GStoreDatabase = Room.databaseBuilder(context, GStoreDatabase::class.java, DATABASE)
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13
        )
        .addTypeConverter(downloadConverter)
        .addTypeConverter(accountConverter)
        .build()

    @Provides
    fun providesDownloadDao(gstoreDatabase: GStoreDatabase): DownloadDao =
        gstoreDatabase.downloadDao()

    @Provides
    fun providesFavouriteDao(gstoreDatabase: GStoreDatabase): FavouriteDao =
        gstoreDatabase.favouriteDao()

    @Provides
    fun providesUpdateDao(gstoreDatabase: GStoreDatabase): UpdateDao = gstoreDatabase.updateDao()

    @Provides
    fun providesIgnoredUpdateDao(gstoreDatabase: GStoreDatabase): IgnoredUpdateDao =
        gstoreDatabase.ignoredUpdateDao()

    @Provides
    fun providesReviewDao(gstoreDatabase: GStoreDatabase): ReviewDao = gstoreDatabase.reviewDao()

    @Provides
    fun providesAccountDao(gstoreDatabase: GStoreDatabase): AccountDao = gstoreDatabase.accountDao()

    @Provides
    fun providesAppAccountBindingDao(gstoreDatabase: GStoreDatabase): AppAccountBindingDao =
        gstoreDatabase.appAccountBindingDao()

    @Provides
    fun providesTrackerDao(gstoreDatabase: GStoreDatabase): TrackerDao = gstoreDatabase.trackerDao()

    @Provides
    fun providesNotificationDao(gstoreDatabase: GStoreDatabase): NotificationDao =
        gstoreDatabase.notificationDao()
}
