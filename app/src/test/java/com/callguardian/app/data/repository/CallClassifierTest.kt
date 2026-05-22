package com.callguardian.app.data.repository

import com.callguardian.app.core.model.AnonymousMode
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.ProtectionLevel
import com.callguardian.app.core.model.RuleAction
import com.callguardian.app.core.model.RuleType
import com.callguardian.app.data.local.AppSettingsEntity
import com.callguardian.app.data.local.CountryRuleEntity
import com.callguardian.app.data.local.RuleEntity
import com.callguardian.app.telephony.PhoneNumberNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class CallClassifierTest {
    private val classifier = CallClassifier(PhoneNumberNormalizer())

    @Test
    fun anonymousBlockModeBlocksAnonymousCalls() {
        val decision = classifier.classify(
            rawNumber = null,
            settings = AppSettingsEntity(anonymousMode = AnonymousMode.BLOCK),
            rules = emptyList(),
            countryRule = null,
            existsInContacts = false,
            recentSimilarCalls = 0,
        )

        assertEquals(CallAction.BLOCKED, decision.action)
    }

    @Test
    fun anonymousRepeatedAttemptsModeAllowsAfterConfiguredThreshold() {
        val decision = classifier.classify(
            rawNumber = null,
            settings = AppSettingsEntity(
                anonymousMode = AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS,
                allowRepeatedAnonymousAfterAttempts = 2,
            ),
            rules = emptyList(),
            countryRule = null,
            existsInContacts = false,
            recentSimilarCalls = 2,
        )

        assertEquals(CallAction.ALLOWED, decision.action)
    }

    @Test
    fun foreignWarnOnlyModeWarnsInsteadOfBlocking() {
        val decision = classifier.classify(
            rawNumber = "+33123456789",
            settings = AppSettingsEntity(
                protectionLevel = ProtectionLevel.BALANCED,
                foreignCallMode = ForeignCallMode.WARN_ONLY,
            ),
            rules = emptyList(),
            countryRule = null,
            existsInContacts = false,
            recentSimilarCalls = 0,
        )

        assertEquals(CallAction.WARNED, decision.action)
    }

    @Test
    fun prefixRuleUsesConfiguredAction() {
        val decision = classifier.classify(
            rawNumber = "+390212345678",
            settings = AppSettingsEntity(protectionLevel = ProtectionLevel.BALANCED),
            rules = listOf(
                RuleEntity(
                    id = 42,
                    label = "Prefisso ufficio",
                    value = "+3902",
                    type = RuleType.PREFIX,
                    action = RuleAction.SILENCE,
                )
            ),
            countryRule = null,
            existsInContacts = false,
            recentSimilarCalls = 0,
        )

        assertEquals(CallAction.SILENCED, decision.action)
        assertEquals(42L, decision.matchedRuleId)
    }

    @Test
    fun rangeRuleMatchesNumbersWithinConfiguredBounds() {
        val decision = classifier.classify(
            rawNumber = "+39021234542",
            settings = AppSettingsEntity(protectionLevel = ProtectionLevel.BALANCED),
            rules = listOf(
                RuleEntity(
                    id = 43,
                    label = "Range call center",
                    value = "+39021234500-+39021234599",
                    type = RuleType.RANGE,
                    action = RuleAction.BLOCK,
                )
            ),
            countryRule = null,
            existsInContacts = false,
            recentSimilarCalls = 0,
        )

        assertEquals(CallAction.BLOCKED, decision.action)
        assertEquals(43L, decision.matchedRuleId)
    }

    @Test
    fun rangeRuleDoesNotMatchAdjacentNumbersOutsideBounds() {
        val decision = classifier.classify(
            rawNumber = "+39021234600",
            settings = AppSettingsEntity(protectionLevel = ProtectionLevel.BALANCED),
            rules = listOf(
                RuleEntity(
                    id = 44,
                    label = "Range call center",
                    value = "+39021234500-+39021234599",
                    type = RuleType.RANGE,
                    action = RuleAction.BLOCK,
                )
            ),
            countryRule = null,
            existsInContacts = false,
            recentSimilarCalls = 0,
        )

        assertEquals(CallAction.WARNED, decision.action)
    }

    @Test
    fun scheduledForeignModeWarnsOutsideConfiguredSchedules() {
        val decision = classifier.classify(
            rawNumber = "+33123456789",
            settings = AppSettingsEntity(
                protectionLevel = ProtectionLevel.BALANCED,
                foreignCallMode = ForeignCallMode.SCHEDULED,
            ),
            rules = emptyList(),
            countryRule = CountryRuleEntity("FR", "Francia", "+33", "", CountryStatus.BLOCKED),
            existsInContacts = false,
            recentSimilarCalls = 0,
        )

        assertEquals(CallAction.WARNED, decision.action)
    }

    @Test
    fun scheduledForeignModeBlocksBlockedCountriesDuringConfiguredSchedule() {
        val decision = classifier.classify(
            rawNumber = "+33123456789",
            settings = AppSettingsEntity(
                protectionLevel = ProtectionLevel.BALANCED,
                foreignCallMode = ForeignCallMode.SCHEDULED,
            ),
            rules = listOf(
                RuleEntity(
                    id = 99,
                    label = "Esteri 00:00-23:59",
                    value = "0-1439",
                    type = RuleType.SCHEDULE,
                    action = RuleAction.BLOCK,
                    startsAtMinute = 0,
                    endsAtMinute = 1439,
                )
            ),
            countryRule = CountryRuleEntity("FR", "Francia", "+33", "", CountryStatus.BLOCKED),
            existsInContacts = false,
            recentSimilarCalls = 0,
        )

        assertEquals(CallAction.BLOCKED, decision.action)
        assertEquals(99L, decision.matchedRuleId)
    }
}
