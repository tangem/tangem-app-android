package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.SettingsRepository

class IsGooglePayAvailableUseCase(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke() = Either.catch {
        settingsRepository.isGooglePayAvailability()
    }
}