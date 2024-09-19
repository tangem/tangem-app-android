package com.tangem.domain.settings.usercountry.models

/**
 * User country error
 *
 * @author Andrew Khokhlov on 15/09/2024
 */
sealed interface UserCountryError {

    /** User country is still loading */
    data object NotSetup : UserCountryError

    /** Data error */
    data object DataError : UserCountryError
}
