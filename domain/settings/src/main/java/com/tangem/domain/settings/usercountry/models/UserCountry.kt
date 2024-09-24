package com.tangem.domain.settings.usercountry.models

/**
 * User country
 *
[REDACTED_AUTHOR]
 */
sealed class UserCountry(open val code: String) {

    data object Russia : UserCountry("ru")

    data class Other(override val code: String) : UserCountry(code)
}