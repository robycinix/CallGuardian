package com.callguardian.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callguardian.app.ui.components.ScreenHeader
import com.callguardian.app.ui.components.SwipeToDeleteContainer
import com.callguardian.app.viewmodel.LogsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(viewModel: LogsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedEventIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var expandedEventIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmClearAll by remember { mutableStateOf(false) }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Svuotare il registro?") },
            text = { Text("Tutti gli eventi salvati verranno eliminati dal database locale.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllEvents()
                        selectedEventIds = emptySet()
                        confirmClearAll = false
                    },
                ) {
                    Text("Elimina tutto")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text("Annulla")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Registro eventi",
                subtitle = "Ogni chiamata valutata diventa una traccia leggibile e ripulibile.",
            )
        }
        if (state.events.isNotEmpty()) {
            item {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { viewModel.cleanupEventLogs() }) {
                        Text("Pulisci scaduti")
                    }
                    OutlinedButton(
                        enabled = selectedEventIds.isNotEmpty(),
                        onClick = {
                            viewModel.deleteEvents(selectedEventIds)
                            selectedEventIds = emptySet()
                        },
                    ) {
                        Text("Elimina selezionati")
                    }
                    IconButton(onClick = { confirmClearAll = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Elimina tutti gli eventi")
                    }
                }
            }
        }
        if (state.events.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.54f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Nessuna chiamata bloccata finora. Sei al sicuro!", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Quando CallGuardian valutera una chiamata, qui vedrai data, azione e motivo.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        items(state.events, key = { it.id }) { event ->
            val expanded = event.id in expandedEventIds
            SwipeToDeleteContainer(
                onDelete = {
                    viewModel.deleteEvent(event)
                    selectedEventIds = selectedEventIds - event.id
                    expandedEventIds = expandedEventIds - event.id
                },
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = event.id in selectedEventIds,
                        onCheckedChange = { checked ->
                            selectedEventIds = if (checked) {
                                selectedEventIds + event.id
                            } else {
                                selectedEventIds - event.id
                            }
                        },
                    )
                    EventRow(
                        title = event.contactName ?: event.phoneNumber,
                        subtitle = if (expanded) {
                            val numberDetail = event.contactName?.let { "${event.phoneNumber} - " }.orEmpty()
                            "$numberDetail${event.action.displayName()} - ${event.reason}"
                        } else {
                            val numberDetail = event.contactName?.let { "${event.phoneNumber} - " }.orEmpty()
                            "$numberDetail${event.action.displayName()} - ${event.reason}"
                        },
                        timeMillis = event.timestampMillis,
                        action = event.action,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                expandedEventIds = if (expanded) {
                                    expandedEventIds - event.id
                                } else {
                                    expandedEventIds + event.id
                                }
                            },
                        compact = !expanded,
                        actions = {
                            IconButton(onClick = {
                                viewModel.deleteEvent(event)
                                selectedEventIds = selectedEventIds - event.id
                                expandedEventIds = expandedEventIds - event.id
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Elimina evento")
                            }
                        },
                        detail = {
                            DecisionExplanation(
                                reason = event.reason,
                                score = event.score,
                                risk = event.riskLevel.displayName(),
                                matchedRuleId = event.matchedRuleId,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionExplanation(
    reason: String,
    score: Int,
    risk: String,
    matchedRuleId: Long?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Perché questa decisione", fontWeight = FontWeight.SemiBold)
            Text(reason, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Rischio: ${risk.lowercase()} - punteggio $score - regola ${matchedRuleId ?: "nessuna"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
