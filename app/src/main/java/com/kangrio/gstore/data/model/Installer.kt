/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.model

/**
 * Supported installers for Aurora Store
 */
enum class Installer {
    SESSION,
    NATIVE,
    ROOT,
    SERVICE,
    AM,
    SHIZUKU,
    MICROG
}
