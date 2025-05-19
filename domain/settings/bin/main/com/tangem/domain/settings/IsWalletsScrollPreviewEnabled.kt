package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

/**
 * Checks if wallets scroll preview is enabled
 *
 * @property settingsRepository settings repository
 *
[REDACTED_AUTHOR]
 */
class IsWalletsScrollPreviewEnabled(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(): Boolean = settingsRepository.isWalletScrollPreviewEnabled()
}