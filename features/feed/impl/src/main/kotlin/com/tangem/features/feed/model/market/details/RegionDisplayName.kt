package com.tangem.features.feed.model.market.details

import java.util.Locale

/**
 * Turns the country code the app sends as `country` into a name a user can read.
 *
 * Naming the region is only worth it when the platform knows the name: an unresolvable code would
 * otherwise reach the banner as raw letters, so callers fall back to the region-less wording instead.
 */
internal object RegionDisplayName {

    // an unknown region resolves to a localized "Unknown Region" rather than to the code, so the code has
    // to be checked against the ISO list instead of the resolved name
    private val isoCountries: Set<String> = Locale.getISOCountries().toSet()

    fun resolve(countryCode: String, locale: Locale = Locale.getDefault()): String? {
        val code = countryCode.trim().uppercase(Locale.ROOT).takeIf { it in isoCountries } ?: return null

        return Locale.Builder().setRegion(code).build()
            .getDisplayCountry(locale)
            .takeIf { it.isNotEmpty() && it != code }
    }
}