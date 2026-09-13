package com.tangem.utils

import java.util.Locale

private val ALPHA_2_REGEX = Regex("[A-Za-z]{2}")
private const val UNKNOWN_REGION = "ZZ"

object CountryNames {

    fun getDisplayName(countryCode: String?, locale: Locale = Locale.getDefault()): String {
        val trimmed = countryCode?.trim().orEmpty()
        if (!ALPHA_2_REGEX.matches(trimmed)) return trimmed

        val region = trimmed.uppercase(Locale.ROOT)
        if (region == UNKNOWN_REGION) return trimmed

        val displayName = Locale.Builder().setRegion(region).build().getDisplayCountry(locale)

        return if (displayName.isBlank() || displayName == region) trimmed else displayName
    }
}