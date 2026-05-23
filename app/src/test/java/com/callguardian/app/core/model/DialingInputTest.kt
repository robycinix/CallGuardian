package com.callguardian.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DialingInputTest {
    private val italy = CountryDialingInfo("IT", "Italia", "+39", "")
    private val france = CountryDialingInfo("FR", "Francia", "+33", "")

    @Test
    fun localNumberGetsSelectedInternationalPrefix() {
        assertEquals("+393471234567", resolveInternationalDialingInput("347 123 4567", italy))
    }

    @Test
    fun internationalNumberTypedManuallyIsKept() {
        assertEquals("+442012345678", resolveInternationalDialingInput("+442012345678", italy))
        assertEquals("0033123456789", resolveInternationalDialingInput("0033123456789", italy))
    }

    @Test
    fun selectedCountryCanCreateForeignPattern() {
        assertEquals("+3320", resolveInternationalDialingInput("20", france))
    }

    @Test
    fun anonymousTokenIsNotPrefixed() {
        assertEquals("anonimo", resolveInternationalDialingInput("anonimo", italy))
    }
}
