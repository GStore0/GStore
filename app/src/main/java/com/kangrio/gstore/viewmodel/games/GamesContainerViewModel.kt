/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.viewmodel.games

import androidx.lifecycle.ViewModel
import com.kangrio.gstore.data.providers.AuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GamesContainerViewModel @Inject constructor(val authProvider: AuthProvider) : ViewModel()
