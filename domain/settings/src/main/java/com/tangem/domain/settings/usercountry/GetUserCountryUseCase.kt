package com.tangem.domain.settings.usercountry

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.UserCountryError

/**
 * Get user country code use case
 *
 * @property settingsRepository settings repository
 *
* [REDACTED_AUTHOR]
 */
class GetUserCountryUseCase(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(): Either<UserCountryError, UserCountry> {
        return either {
            val userCountryCode = catch(
                block = { settingsRepository.getUserCountryCodeSync() },
                catch = { raise(UserCountryError.DataError) },
            )

            ensureNotNull(userCountryCode) { UserCountryError.NotSetup }

            userCountryCode
        }
    }
}
