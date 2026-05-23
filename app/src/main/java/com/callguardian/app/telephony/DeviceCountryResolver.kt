package com.callguardian.app.telephony

import android.content.Context
import android.telephony.TelephonyManager
import com.callguardian.app.core.model.CountryDialingInfo
import com.callguardian.app.core.model.SupportedCountries
import java.util.Locale

object DeviceCountryResolver {
    fun homeRegionIso(context: Context?): String {
        val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return listOf(
            runCatching { telephonyManager?.simCountryIso }.getOrNull(),
            runCatching { telephonyManager?.networkCountryIso }.getOrNull(),
            Locale.getDefault().country,
        ).firstOrNull { !it.isNullOrBlank() }?.uppercase(Locale.US) ?: DEFAULT_REGION
    }

    fun defaultDialingCountry(context: Context?): CountryDialingInfo =
        SupportedCountries.firstOrNull { it.iso == homeRegionIso(context) }
            ?: SupportedCountries.firstOrNull { it.iso == DEFAULT_REGION }
            ?: SupportedCountries.first()

    private const val DEFAULT_REGION = "IT"
}
