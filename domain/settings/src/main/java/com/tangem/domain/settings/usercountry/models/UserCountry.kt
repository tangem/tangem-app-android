package com.tangem.domain.settings.usercountry.models

import java.util.Locale

/**
 * User country
 *
[REDACTED_AUTHOR]
 */
sealed class UserCountry(open val code: String) {

    data object Russia : UserCountry("ru")

    data class Other(override val code: String) : UserCountry(code)

    companion object {

        /** Creates a country from a raw [code], normalizing its case */
        fun from(code: String): UserCountry {
            val normalizedCode = code.lowercase(Locale.ROOT)

            return if (normalizedCode == Russia.code) Russia else Other(code = normalizedCode)
        }

        /**
         * Creates a country from the device locale region — what the system considers the user location.
         *
         * Used as the last-resort substitute when the country has not been resolved from the backend.
         */
        fun fromDeviceLocale(): UserCountry = from(code = Locale.getDefault().country)
    }
}

/**
 * Whether FCA restrictions apply to [this] country.
 *
 * The resolved country wins over the device locale: the locale is only a substitute while the country is
 * still unknown.
 */
fun UserCountry?.needApplyFCARestrictions(): Boolean {
    val code = this?.code ?: Locale.getDefault().country

    return code.equals(GB_COUNTRY.code, ignoreCase = true)
}

/** Kept in the raw uppercase ISO form: it is compared against [Locale] regions, not against resolved countries */
val GB_COUNTRY = UserCountry.Other(Locale.UK.country)