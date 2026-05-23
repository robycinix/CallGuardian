package com.callguardian.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callguardian.app.ui.components.ScreenHeader
import com.callguardian.app.ui.components.SectionCard
import com.callguardian.app.viewmodel.CountryAggressionStat
import com.callguardian.app.viewmodel.LogsViewModel
import com.callguardian.app.viewmodel.PeriodStat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(viewModel: LogsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val insights = state.insights
    val noActivityYet = insights.totalCalls == 0 && state.blockedToday == 0
    var confirmResetStats by remember { mutableStateOf(false) }

    if (confirmResetStats) {
        AlertDialog(
            onDismissRequest = { confirmResetStats = false },
            title = { Text(uiText("Azzerare le statistiche?", "Reset statistics?")) },
            text = { Text(uiText("Il registro eventi resta separato. Questa scelta cancella solo lo storico statistico e riparte da zero.", "The event log stays separate. This only clears the statistics history and starts again from zero.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetStats()
                        confirmResetStats = false
                    },
                ) { Text(uiText("Azzera statistiche", "Reset statistics")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetStats = false }) { Text(uiText("Annulla", "Cancel")) }
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
                title = uiText("Statistiche", "Statistics"),
                subtitle = uiText("Andamento, zone calde e pressione spam in tempo reale.", "Trends, hot spots, and spam pressure in real time."),
                trailing = {
                OutlinedButton(onClick = { confirmResetStats = true }) {
                    Text(uiText("Azzera", "Reset"))
                }
                },
            )
        }

        item {
            SectionCard(title = uiText("Quadro generale", "Overview")) {
                if (noActivityYet) {
                    EmptyProtectionState()
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile(uiText("Totale", "Total"), insights.totalCalls.toString(), Modifier.weight(1f))
                    StatTile(uiText("Noiose", "Nuisance"), insights.nuisanceCalls.toString(), Modifier.weight(1f))
                    StatTile(uiText("Indice", "Index"), insights.averageRiskScore.toString(), Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile(uiText("Bloccate", "Blocked"), insights.blockedCalls.toString(), Modifier.weight(1f))
                    StatTile(uiText("Avvisi", "Warnings"), insights.warnedCalls.toString(), Modifier.weight(1f))
                    StatTile(uiText("Mute", "Muted"), insights.silencedCalls.toString(), Modifier.weight(1f))
                }
                Text(uiText("Oggi bloccate: ${state.blockedToday}", "Blocked today: ${state.blockedToday}"), style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { (state.blockedToday / 20f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
            }
        }

        item {
            SectionCard(title = uiText("Quando diventano insistenti", "When they become persistent")) {
                InsightLine(uiText("Ora più calda", "Peak hour"), insights.peakHour?.let { uiText("${it.label} con ${it.total} chiamate", "${it.label} with ${it.total} calls") })
                InsightLine(uiText("Giorno peggiore", "Worst day"), insights.peakDay?.let { uiText("${it.label} con ${it.total} chiamate", "${it.label} with ${it.total} calls") })
                InsightLine(uiText("Mese peggiore", "Worst month"), insights.worstMonth?.let { uiText("${it.label} con ${it.total} chiamate", "${it.label} with ${it.total} calls") })
            }
        }

        item {
            SectionCard(title = uiText("Distribuzione per ora", "Hourly distribution")) {
                if (insights.hourlyDistribution.all { it.total == 0 }) {
                    EmptyChartState(uiText("Nessuna chiamata bloccata finora. Sei al sicuro!", "No blocked calls so far. You are safe."))
                } else {
                    BarChart(
                        stats = insights.hourlyDistribution,
                        barColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                    )
                    AxisLabels(insights.hourlyDistribution, every = 4)
                }
            }
        }

        item {
            SectionCard(title = uiText("Giorni più pesanti", "Heaviest days")) {
                HorizontalStatRows(insights.weekdayDistribution)
            }
        }

        item {
            SectionCard(title = uiText("Mesi peggiori", "Worst months")) {
                if (insights.monthlyDistribution.all { it.total == 0 }) {
                    EmptyChartState(uiText("Nessuno storico ancora: CallGuardian resta in ascolto.", "No history yet: CallGuardian is listening."))
                } else {
                    BarChart(
                        stats = insights.monthlyDistribution,
                        barColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                    )
                    AxisLabels(insights.monthlyDistribution, every = 2)
                }
            }
        }

        item {
            SectionCard(title = uiText("Zone più aggressive", "Most aggressive areas")) {
                if (insights.aggressiveCountries.isEmpty()) {
                    Text(uiText("Nessuna zona aggressiva rilevata nello storico statistiche.", "No aggressive area detected in the statistics history."))
                } else {
                    val maxScore = insights.aggressiveCountries.maxOf { it.aggressionScore }.coerceAtLeast(1)
                    insights.aggressiveCountries.forEach { stat ->
                        AggressiveCountryRow(stat, maxScore)
                    }
                }
            }
        }

        item {
            SectionCard(title = uiText("Categorie spam", "Spam categories")) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile(uiText("Stop", "Stop"), insights.blockedCalls.toString())
                    StatTile(uiText("Alert", "Alert"), insights.warnedCalls.toString())
                    StatTile(uiText("Mute", "Mute"), insights.silencedCalls.toString())
                }
            }
        }
    }
}

@Composable
private fun EmptyProtectionState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(uiText("Nessuna chiamata bloccata finora. Sei al sicuro!", "No blocked calls so far. You are safe."), fontWeight = FontWeight.SemiBold)
                Text(
                    uiText("Le statistiche si riempiranno automaticamente appena arriveranno eventi reali.", "Statistics will fill automatically as soon as real events arrive."),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyChartState(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(message, fontWeight = FontWeight.SemiBold)
            Text(
                uiText("Non devi fare nulla: la protezione registrera gli eventi quando servira.", "You do not need to do anything: protection will record events when needed."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InsightLine(label: String, value: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value ?: uiText("Dati insufficienti", "Not enough data"), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BarChart(
    stats: List<PeriodStat>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val peakColor = MaterialTheme.colorScheme.error
    val max = stats.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
    Canvas(modifier = modifier) {
        val gridStep = size.height / 4f
        repeat(5) { index ->
            val y = size.height - gridStep * index
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        val slotWidth = size.width / stats.size.coerceAtLeast(1)
        val barWidth = slotWidth * 0.56f
        stats.forEachIndexed { index, stat ->
            val barHeight = size.height * (stat.total.toFloat() / max)
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = if (stat.total == max && stat.total > 0) peakColor else barColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            )
        }
    }
}

@Composable
private fun AxisLabels(stats: List<PeriodStat>, every: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        stats.forEachIndexed { index, stat ->
            if (index % every == 0) {
                Text(stat.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HorizontalStatRows(stats: List<PeriodStat>) {
    val max = stats.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEach { stat ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stat.label, modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                    progress = { stat.total.toFloat() / max },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stat.total.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun AggressiveCountryRow(stat: CountryAggressionStat, maxScore: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stat.countryIso, fontWeight = FontWeight.SemiBold)
            Text(uiText("${stat.total} eventi, indice ${stat.aggressionScore}", "${stat.total} events, index ${stat.aggressionScore}"))
        }
        LinearProgressIndicator(
            progress = { (stat.aggressionScore.toFloat() / maxScore).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            uiText("Bloccate ${stat.blocked} - avvisi ${stat.warned} - silenziate ${stat.silenced}", "Blocked ${stat.blocked} - warnings ${stat.warned} - silenced ${stat.silenced}"),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
