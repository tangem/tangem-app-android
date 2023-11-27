package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * Never to show wallets scroll preview
 *
 * @property settingsRepository settings repository
 *
 * @author Andrew Khokhlov on 24/11/2023
 */
class NeverToShowWalletsScrollPreview(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke() = settingsRepository.setWalletScrollPreviewAvailability(isEnabled = false)
}
