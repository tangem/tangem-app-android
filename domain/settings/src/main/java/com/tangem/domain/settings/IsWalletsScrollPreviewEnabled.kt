package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * Checks if wallets scroll preview is enabled
 *
 * @property settingsRepository settings repository
 *
 * @author Andrew Khokhlov on 24/11/2023
 */
class IsWalletsScrollPreviewEnabled(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(): Boolean = settingsRepository.isWalletScrollPreviewEnabled()
}
