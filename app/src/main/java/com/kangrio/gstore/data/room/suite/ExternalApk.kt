/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.room.suite

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.PlayFile
import com.kangrio.gstore.util.PackageUtil
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "externalApk")
data class ExternalApk(
    @PrimaryKey
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val displayName: String,
    val iconURL: String,
    val developerName: String,
    var fileList: List<PlayFile>
) : Parcelable {

    fun isInstalled(context: Context): Boolean = PackageUtil.isInstalled(context, packageName)
}
