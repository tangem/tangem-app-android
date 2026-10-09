package com.tangem.utils

import java.util.Locale

object CountryNames {

    private val isoCountries: Set<String> = Locale.getISOCountries().toSet()

    /**
     * Display name of [countryCode], or `null` when the platform has no country to name — callers then
     * leave the region out rather than printing a bare code.
     *
     * Membership in the ISO list is the guard, not the resolved name: CLDR also names regions that are
     * not countries (`XA` is "Pseudo-Accents", `EU` is "European Union"), so comparing the name against
     * the code lets those through.
     */
    fun resolveDisplayName(countryCode: String?, locale: Locale = Locale.getDefault()): String? {
        val region = countryCode?.trim()?.uppercase(Locale.ROOT)?.takeIf { it in isoCountries } ?: return null

        return Locale.Builder().setRegion(region).build()
            .getDisplayCountry(locale)
            .takeIf { it.isNotBlank() && it != region }
    }

    /** Same as [resolveDisplayName], but echoes [countryCode] back when there is no name to give. */
    fun getDisplayName(countryCode: String?, locale: Locale = Locale.getDefault()): String =
        resolveDisplayName(countryCode, locale) ?: countryCode?.trim().orEmpty()
}