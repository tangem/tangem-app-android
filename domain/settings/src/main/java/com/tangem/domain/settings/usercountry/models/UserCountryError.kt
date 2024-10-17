package com.tangem.domain.settings.usercountry.models

/**
 * User country error
 *
[REDACTED_AUTHOR]
 */
sealed interface UserCountryError {

    /** User country is still loading */
    data object NotSetup : UserCountryError

    /** Data error */
    data object DataError : UserCountryError
}