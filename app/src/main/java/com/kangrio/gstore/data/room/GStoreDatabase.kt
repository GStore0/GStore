/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kangrio.gstore.data.room.account.Account
import com.kangrio.gstore.data.room.account.AccountConverter
import com.kangrio.gstore.data.room.account.AccountDao
import com.kangrio.gstore.data.room.account.AppAccountBinding
import com.kangrio.gstore.data.room.account.AppAccountBindingDao
import com.kangrio.gstore.data.room.download.Download
import com.kangrio.gstore.data.room.download.DownloadConverter
import com.kangrio.gstore.data.room.download.DownloadDao
import com.kangrio.gstore.data.room.exodus.TrackerDao
import com.kangrio.gstore.data.room.exodus.TrackerEntity
import com.kangrio.gstore.data.room.favourite.Favourite
import com.kangrio.gstore.data.room.favourite.FavouriteDao
import com.kangrio.gstore.data.room.notification.AppNotification
import com.kangrio.gstore.data.room.notification.NotificationDao
import com.kangrio.gstore.data.room.review.LocalReview
import com.kangrio.gstore.data.room.review.ReviewDao
import com.kangrio.gstore.data.room.update.IgnoredUpdate
import com.kangrio.gstore.data.room.update.IgnoredUpdateDao
import com.kangrio.gstore.data.room.update.Update
import com.kangrio.gstore.data.room.update.UpdateDao

@Database(
    entities = [
        Download::class,
        Favourite::class,
        Update::class,
        IgnoredUpdate::class,
        LocalReview::class,
        Account::class,
        AppAccountBinding::class,
        TrackerEntity::class,
        AppNotification::class
    ],
    version = 13,
    exportSchema = true
)
@TypeConverters(DownloadConverter::class, AccountConverter::class)
abstract class GStoreDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun updateDao(): UpdateDao
    abstract fun ignoredUpdateDao(): IgnoredUpdateDao
    abstract fun reviewDao(): ReviewDao
    abstract fun accountDao(): AccountDao
    abstract fun appAccountBindingDao(): AppAccountBindingDao
    abstract fun trackerDao(): TrackerDao
    abstract fun notificationDao(): NotificationDao
}
