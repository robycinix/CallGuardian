package com.callguardian.app.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberNormalizerTest {
    private val normalizer = PhoneNumberNormalizer()

    @Test
    fun nullBlankAndAnonymousTokensNormalizeToAnonymous() {
        assertEquals(PhoneNumberNormalizer.ANONYMOUS, normalizer.normalize(null))
        assertEquals(PhoneNumberNormalizer.ANONYMOUS, normalizer.normalize(" "))
        assertEquals(PhoneNumberNormalizer.ANONYMOUS, normalizer.normalize("Anonimo"))
        assertEquals(PhoneNumberNormalizer.ANONYMOUS, normalizer.normalize("private"))
    }

    @Test
    fun keepsItalianTrunkPrefixForLandlines() {
        assertEquals("+390212345678", normalizer.normalize("02 1234 5678"))
    }

    @Test
    fun normalizesItalianMobileWithoutInternationalPrefix() {
        assertEquals("+393471234567", normalizer.normalize("347 123 4567"))
    }

    @Test
    fun stripsCommonSeparatorsFromInternationalNumbers() {
        assertEquals("+393471234567", normalizer.normalize("+39.347/123-4567"))
    }

    @Test
    fun convertsDoubleZeroInternationalPrefix() {
        assertEquals("+33123456789", normalizer.normalize("0033 1 23 45 67 89"))
    }

    @Test
    fun resolvesForeignCountryFromSanitizedNumber() {
        val normalized = normalizer.normalize("+33 (1) 23 45 67 89")

        assertEquals("+33123456789", normalized)
        assertTrue(normalizer.isForeign(normalized))
    }
}
