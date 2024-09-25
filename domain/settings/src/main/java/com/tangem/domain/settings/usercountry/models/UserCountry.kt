package com.tangem.domain.settings.usercountry.models

/**
 * User country
 *
 * @author Andrew Khokhlov on 12/09/2024
 */
sealed class UserCountry(open val code: String) {

    data object Russia : UserCountry("ru")

    data class Other(override val code: String) : UserCountry(code)
}
