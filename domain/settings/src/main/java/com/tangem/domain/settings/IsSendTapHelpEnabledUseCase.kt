package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * Checks if tap help is enabled
 *
 * @property settingsRepository settings repository
 */
class IsSendTapHelpEnabledUseCase(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(): Boolean = settingsRepository.isSendTapHelpPreviewEnabled()
}