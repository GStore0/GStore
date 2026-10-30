/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.composition

import androidx.compose.runtime.compositionLocalOf
import com.kangrio.gstore.data.model.NetworkStatus

/**
 * CompositionLocal carrying the current device network status. Provided once at the
 * activity root from a single [com.kangrio.gstore.data.providers.NetworkProvider] subscription,
 * so any screen can read `LocalNetworkStatus.current` without injecting the provider
 * or duplicating the flow collection.
 *
 * Uses [compositionLocalOf] (not static) so only readers recompose on change.
 */
val LocalNetworkStatus = compositionLocalOf { NetworkStatus.AVAILABLE }
