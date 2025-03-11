package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.SettingsRepository

class SetGooglePayAvailabilityUseCase(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(isAvailable: Boolean) = Either.catch {
        settingsRepository.setGooglePayAvailability(isAvailable)
    }
}