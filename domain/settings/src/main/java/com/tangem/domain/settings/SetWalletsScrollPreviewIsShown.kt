package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

/**
[REDACTED_AUTHOR]
 */
class SetWalletsScrollPreviewIsShown(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke() = settingsRepository.setWalletScrollPreviewAvailability(isEnabled = false)
}