/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.model

import com.kangrio.gstore.data.room.download.Download
import com.kangrio.gstore.data.room.notification.AppNotification

sealed class NotificationEntry {

    abstract val id: String
    abstract val timestamp: Long

    data class PendingInstall(val download: Download) : NotificationEntry() {
        override val id get() = "install:${download.packageName}"
        override val timestamp get() = download.downloadedAt
    }

    data class Remote(val notification: AppNotification) : NotificationEntry() {
        override val id get() = notification.id
        override val timestamp get() = notification.timestamp
    }
}
