/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.viewmodel.details

import androidx.lifecycle.ViewModel
import com.kangrio.gstore.data.ExodusRepository
import com.kangrio.gstore.data.model.ExodusTracker
import com.kangrio.gstore.data.model.Report
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExodusViewModel @Inject constructor(
    private val exodusRepository: ExodusRepository
) : ViewModel() {

    /**
     * Resolves the tracker ids of [report] to their details via the local tracker table,
     * best-effort (ids missing from the table resolve to a `Tracker #<id>` placeholder).
     */
    suspend fun resolveTrackers(report: Report): List<ExodusTracker> =
        exodusRepository.resolveTrackers(report.trackers)
}
