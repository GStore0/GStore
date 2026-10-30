package com.kangrio.gstore.patch.compose.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangrio.gstore.patch.state.StepStatus
import com.kangrio.gstore.patch.util.Patcher
import kotlinx.coroutines.delay

@Composable
fun PatchProgressDialog(packageName: String, displayName: String) {
    val state = Patcher.progressState
    if (!state.isVisible || packageName != state.packageName) return

    // Auto-dismiss: short delay on success, longer on error so it's readable
    LaunchedEffect(state.isComplete, state.isError) {
        when {
            state.isComplete -> { delay(2000); state.dismiss() }
            state.isError -> { delay(5000); state.dismiss() }
        }
    }

    // Log lines collected from currentMessage (swap for a real log list if Patcher has one)
    val logs = remember(packageName) { mutableStateListOf<String>() }
    LaunchedEffect(state.currentMessage) {
        val msg = state.currentMessage
        if (msg.isNotBlank() && logs.lastOrNull() != msg) logs.add(msg)
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { if (logs.lastOrNull() != it) logs.add(it) }
    }

    val currentStep = state.steps.firstOrNull { it.status == StepStatus.RUNNING }
        ?: state.steps.lastOrNull { it.status == StepStatus.DONE }

    val stateLabel = when {
        state.isError -> "Failed"
        state.isComplete -> "Complete"
        else -> currentStep?.label ?: "Preparing"
    }

    AlertDialog(
        onDismissRequest = { if (state.isComplete || state.isError) state.dismiss() },
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Column {
                Text(
                    displayName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    when {
                        state.isError -> "Patching failed"
                        state.isComplete -> "Patching complete"
                        else -> "Patching..."
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        state.isError -> Icon(
                            Icons.Filled.Error, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        state.isComplete -> Icon(
                            Icons.Filled.CheckCircle, null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        currentStep != null -> CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        else -> Icon(
                            Icons.Filled.RadioButtonUnchecked, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        stateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Log box
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    val listState = rememberLazyListState()
                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { line ->
                            Text(
                                line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (line == state.errorMessage)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = null
    )
}