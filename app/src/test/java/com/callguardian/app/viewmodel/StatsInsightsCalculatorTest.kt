package com.callguardian.app.viewmodel

import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.RiskLevel
import com.callguardian.app.data.local.StatsEventEntity
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StatsInsightsCalculatorTest {
    @Test
    fun statsInsightsUseStatsEventsAsIndependentHistory() {
        val events = listOf(
            statsEvent(CallAction.BLOCKED, score = 92, countryIso = "FR", hour = 9),
            statsEvent(CallAction.WARNED, score = 55, countryIso = "FR", hour = 9),
            statsEvent(CallAction.ALLOWED, score = 10, countryIso = "IT", hour = 11),
        )

        val insights = events.toStatsInsights()

        assertEquals(3, insights.totalCalls)
        assertEquals(2, insights.nuisanceCalls)
        assertEquals(1, insights.blockedCalls)
        assertEquals(1, insights.warnedCalls)
        assertEquals(52, insights.averageRiskScore)
        assertEquals("09:00", insights.peakHour?.label)
        assertEquals(2, insights.peakHour?.total)
        assertEquals("FR", insights.aggressiveCountries.first().countryIso)
        assertEquals(2, insights.aggressiveCountries.first().total)
    }

    @Test
    fun emptyStatsHistoryProducesEmptyInsights() {
        val insights = emptyList<StatsEventEntity>().toStatsInsights()

        assertEquals(0, insights.totalCalls)
        assertEquals(0, insights.nuisanceCalls)
        assertEquals(0, insights.averageRiskScore)
        assertNotNull(insights.hourlyDistribution)
        assertEquals(24, insights.hourlyDistribution.size)
    }

    private fun statsEvent(
        action: CallAction,
        score: Int,
        countryIso: String?,
        hour: Int,
    ) = StatsEventEntity(
        timestampMillis = LocalDateTime.of(2026, 5, 20, hour, 15)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
        phoneNumber = "+33123456789",
        normalizedNumber = "+33123456789",
        action = action,
        riskLevel = if (score > 70) RiskLevel.LIKELY_SPAM else RiskLevel.SUSPICIOUS,
        score = score,
        reason = "test",
        countryIso = countryIso,
    )
}
