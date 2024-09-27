package com.tangem.domain.settings.usercountry

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.UserCountryError

/**
 * Fetch user country use case
 *
 * @property settingsRepository settings repository
 *
 * @author Andrew Khokhlov on 12/09/2024
 */
class FetchUserCountryUseCase(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(): Either<UserCountryError, Unit> {
        return either {
            catch(
                block = { settingsRepository.fetchUserCountryCode() },
                catch = { raise(UserCountryError.DataError) },
            )
        }
    }
}
