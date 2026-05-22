package com.callguardian.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.data.local.CountryStatRow
import com.callguardian.app.data.local.EventLogEntity
import com.callguardian.app.data.local.StatsEventEntity
import com.callguardian.app.data.repository.GuardianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PeriodStat(
    val label: String,
    val total: Int,
)

data class CountryAggressionStat(
    val countryIso: String,
    val total: Int,
    val blocked: Int,
    val warned: Int,
    val silenced: Int,
    val aggressionScore: Int,
)

data class StatsInsights(
    val totalCalls: Int = 0,
    val nuisanceCalls: Int = 0,
    val blockedCalls: Int = 0,
    val warnedCalls: Int = 0,
    val silencedCalls: Int = 0,
    val averageRiskScore: Int = 0,
    val hourlyDistribution: List<PeriodStat> = emptyList(),
    val weekdayDistribution: List<PeriodStat> = emptyList(),
    val monthlyDistribution: List<PeriodStat> = emptyList(),
    val aggressiveCountries: List<CountryAggressionStat> = emptyList(),
    val peakHour: PeriodStat? = null,
    val peakDay: PeriodStat? = null,
    val worstMonth: PeriodStat? = null,
)

data class LogsUiState(
    val events: List<EventLogEntity> = emptyList(),
    val blockedToday: Int = 0,
    val topCountries: List<CountryStatRow> = emptyList(),
    val insights: StatsInsights = StatsInsights(),
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repository: GuardianRepository,
) : ViewModel() {
    val uiState: StateFlow<LogsUiState> = combine(
        repository.events,
        repository.statsEvents,
        repository.blockedToday,
        repository.topBlockedCountries,
    ) { events, statsEvents, blockedToday, countries ->
        LogsUiState(events, blockedToday, countries, statsEvents.toStatsInsights())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogsUiState())

    init {
        viewModelScope.launch { repository.initialize() }
    }

    fun deleteEvent(event: EventLogEntity) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }

    fun deleteEvents(eventIds: Collection<Long>) {
        viewModelScope.launch { repository.deleteEvents(eventIds) }
    }

    fun deleteAllEvents() {
        viewModelScope.launch { repository.deleteAllEvents() }
    }

    fun cleanupEventLogs() {
        viewModelScope.launch { repository.cleanupEventLogs() }
    }

    fun resetStats() {
        viewModelScope.launch { repository.resetStats() }
    }
}

internal fun List<StatsEventEntity>.toStatsInsights(): StatsInsights {
    val zoneId = ZoneId.systemDefault()
    val locale = Locale.ITALIAN
    val nuisanceEvents = filter { it.action != CallAction.ALLOWED }
    val blocked = count { it.action == CallAction.BLOCKED }
    val warned = count { it.action == CallAction.WARNED }
    val silenced = count { it.action == CallAction.SILENCED }
    val averageRisk = if (isEmpty()) 0 else map { it.score }.average().toInt()

    val hourlyCounts = nuisanceEvents.groupingBy { it.localDateTime(zoneId).hour }.eachCount()
    val hourlyDistribution = (0..23).map { hour ->
        PeriodStat("%02d:00".format(locale, hour), hourlyCounts[hour] ?: 0)
    }

    val weekdayCounts = nuisanceEvents.groupingBy { it.localDateTime(zoneId).dayOfWeek }.eachCount()
    val weekdayDistribution = DayOfWeek.entries.map { day ->
        PeriodStat(day.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar(Char::titlecase), weekdayCounts[day] ?: 0)
    }

    val monthCounts = nuisanceEvents.groupingBy { it.localDateTime(zoneId).month }.eachCount()
    val monthlyDistribution = Month.entries.map { month ->
        PeriodStat(month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar(Char::titlecase), monthCounts[month] ?: 0)
    }

    val aggressiveCountries = nuisanceEvents
        .filter { !it.countryIso.isNullOrBlank() }
        .groupBy { it.countryIso.orEmpty() }
        .map { (countryIso, events) ->
            val countryBlocked = events.count { it.action == CallAction.BLOCKED }
            val countryWarned = events.count { it.action == CallAction.WARNED }
            val countrySilenced = events.count { it.action == CallAction.SILENCED }
            CountryAggressionStat(
                countryIso = countryIso,
                total = events.size,
                blocked = countryBlocked,
                warned = countryWarned,
                silenced = countrySilenced,
                aggressionScore = countryBlocked * 3 + countrySilenced * 2 + countryWarned,
            )
        }
        .sortedWith(compareByDescending<CountryAggressionStat> { it.aggressionScore }.thenByDescending { it.total })
        .take(6)

    return StatsInsights(
        totalCalls = size,
        nuisanceCalls = nuisanceEvents.size,
        blockedCalls = blocked,
        warnedCalls = warned,
        silencedCalls = silenced,
        averageRiskScore = averageRisk,
        hourlyDistribution = hourlyDistribution,
        weekdayDistribution = weekdayDistribution,
        monthlyDistribution = monthlyDistribution,
        aggressiveCountries = aggressiveCountries,
        peakHour = hourlyDistribution.maxByOrNull { it.total }?.takeIf { it.total > 0 },
        peakDay = weekdayDistribution.maxByOrNull { it.total }?.takeIf { it.total > 0 },
        worstMonth = monthlyDistribution.maxByOrNull { it.total }?.takeIf { it.total > 0 },
    )
}

private fun StatsEventEntity.localDateTime(zoneId: ZoneId) =
    Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDateTime()
