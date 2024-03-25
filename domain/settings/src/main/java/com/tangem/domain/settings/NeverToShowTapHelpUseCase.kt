package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * Never to show tap help
 *
 * @property settingsRepository settings repository
 */
class NeverToShowTapHelpUseCase(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke() = settingsRepository.setSendTapHelpPreviewAvailability(isEnabled = false)
}