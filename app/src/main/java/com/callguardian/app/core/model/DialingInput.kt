package com.callguardian.app.core.model

fun resolveInternationalDialingInput(
    rawInput: String,
    selectedCountry: CountryDialingInfo,
): String {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank() || trimmed.isAnonymousDialingInput()) return trimmed
    if (trimmed.startsWith("+") || trimmed.startsWith("00")) return trimmed
    if (trimmed.none { it.isDigit() }) return trimmed
    return "${selectedCountry.dialCode}${trimmed.filter(Char::isDigit)}"
}

private fun String.isAnonymousDialingInput(): Boolean =
    lowercase() in setOf("anonymous", "anonimo", "unknown", "private", "restricted", "unavailable")
