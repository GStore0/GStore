/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.model

data class DownloadInfo(
    val progress: Int = 0,
    val bytesCopied: Long = 0,
    val speed: Long = 0
)
