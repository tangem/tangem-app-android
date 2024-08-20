package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

class ShouldSaveAccessCodesUseCase(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(): Boolean = settingsRepository.shouldSaveAccessCodes()
}