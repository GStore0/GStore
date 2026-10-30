/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.util

import android.content.Context
import com.kangrio.gstore.Constants
import com.kangrio.gstore.patch.util.FlavouredUtilPatch

object FlavouredUtil : IFlavouredUtil {

    override val defaultDispensers = setOf(Constants.URL_DISPENSER)

    override fun promptMicroGInstall(context: Context): Boolean = FlavouredUtilPatch.promptMicroGInstall(context)
}
