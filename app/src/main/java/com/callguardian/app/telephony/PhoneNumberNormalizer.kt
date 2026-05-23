package com.callguardian.app.telephony

import android.content.Context
import com.callguardian.app.core.model.CountryDialingInfo
import com.callguardian.app.core.model.SupportedCountries
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.PhoneNumberUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneNumberNormalizer @Inject constructor(
    @ApplicationContext private val context: Context?,
) {
    constructor() : this(null)

    private val phoneNumberUtil = PhoneNumberUtil.getInstance()

    fun normalize(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ANONYMOUS
        val cleaned = rawNumber.trim()
        return when {
            cleaned.isAnonymousToken() -> ANONYMOUS
            else -> normalizeDialable(cleaned)
        }
    }

    private fun normalizeDialable(rawNumber: String): String {
        val dialable = rawNumber.filter { it.isDigit() || it == '+' }
        if (dialable.isBlank()) return ANONYMOUS

        val internationalCandidate = when {
            dialable.startsWith("00") -> "+${dialable.drop(2)}"
            else -> dialable
        }
        parsedToE164(internationalCandidate)?.let { return it }

        val localRegion = homeRegionIso()
        val localDialCode = SupportedCountries.firstOrNull { it.iso == localRegion }?.dialCode
        val localCandidate = when {
            internationalCandidate.startsWith("+") -> internationalCandidate
            localDialCode != null && internationalCandidate.startsWith("0") -> "$localDialCode$internationalCandidate"
            localDialCode != null && internationalCandidate.firstOrNull()?.isDigit() == true -> "$localDialCode$internationalCandidate"
            else -> internationalCandidate
        }
        return parsedToE164(localCandidate, localRegion) ?: localCandidate
    }

    private fun parsedToE164(number: String, region: String = homeRegionIso()): String? = try {
        val parsedNumber = phoneNumberUtil.parse(number, region)
        phoneNumberUtil.format(parsedNumber, PhoneNumberFormat.E164)
    } catch (_: NumberParseException) {
        null
    }

    private fun homeRegionIso(): String {
        return DeviceCountryResolver.homeRegionIso(context)
    }

    private fun String.isAnonymousToken(): Boolean {
        val normalized = trim().lowercase()
        return normalized in setOf("anonymous", "anonimo", "unknown", "private", "restricted", "unavailable")
    }

    fun countryFor(normalizedNumber: String): CountryDialingInfo? {
        if (normalizedNumber == ANONYMOUS) return null
        val parsedRegion = try {
            val parsedNumber = phoneNumberUtil.parse(normalizedNumber, homeRegionIso())
            phoneNumberUtil.getRegionCodeForNumber(parsedNumber)
        } catch (_: NumberParseException) {
            null
        }
        parsedRegion?.let { region ->
            SupportedCountries.firstOrNull { it.iso == region }?.let { return it }
        }
        return SupportedCountries
            .sortedByDescending { it.dialCode.length }
            .firstOrNull { normalizedNumber.startsWith(it.dialCode) }
    }

    fun isForeign(normalizedNumber: String): Boolean {
        if (normalizedNumber == ANONYMOUS) return false
        val country = countryFor(normalizedNumber)
        return country != null && country.iso != homeRegionIso()
    }

    companion object {
        const val ANONYMOUS = "ANONYMOUS"
    }
}
