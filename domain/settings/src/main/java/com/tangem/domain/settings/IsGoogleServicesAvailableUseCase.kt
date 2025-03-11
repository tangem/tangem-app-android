package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.SettingsRepository

class IsGoogleServicesAvailableUseCase(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke() = Either.catch {
        settingsRepository.isGoogleServicesAvailability()
    }
}