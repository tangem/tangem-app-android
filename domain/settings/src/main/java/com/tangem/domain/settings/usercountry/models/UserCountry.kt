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
}

fun UserCountry?.needApplyFCARestrictions(): Boolean {
    val local = Locale.getDefault().country
    if (local == GB_COUNTRY.code) return true
    return this?.code.equals(GB_COUNTRY.code, ignoreCase = true)
}

val GB_COUNTRY = UserCountry.Other(Locale.UK.country)