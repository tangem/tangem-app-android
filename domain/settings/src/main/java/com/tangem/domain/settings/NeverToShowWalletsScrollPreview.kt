package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * Never to show wallets scroll preview
 *
 * @property settingsRepository settings repository
 *
[REDACTED_AUTHOR]
 */
class NeverToShowWalletsScrollPreview(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke() = settingsRepository.setWalletScrollPreviewAvailability(isEnabled = false)
}