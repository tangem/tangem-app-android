package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * Never to show tap help
 *
 * @property settingsRepository settings repository
 */
class NeverShowTapHelpUseCase(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke() = Either.catch {
        settingsRepository.setSendTapHelpPreviewAvailability(isEnabled = false)
    }
}