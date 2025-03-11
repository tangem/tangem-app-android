package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

class ShouldShowMarketsTooltipUseCase(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(): Boolean = settingsRepository.shouldShowMarketsTooltip()

    suspend operator fun invoke(isShown: Boolean) = settingsRepository.setMarketsTooltipShown(isShown)
}