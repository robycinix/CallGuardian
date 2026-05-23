package com.callguardian.app.telephony

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceCountryResolverTest {
    @Test
    fun usesLocaleCountryWhenTelephonyContextIsNotAvailable() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ITALY)
        try {
            assertEquals("IT", DeviceCountryResolver.homeRegionIso(null))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
