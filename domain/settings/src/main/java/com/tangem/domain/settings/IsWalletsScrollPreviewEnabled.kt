package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

/**
[REDACTED_AUTHOR]
 */
class IsWalletsScrollPreviewEnabled(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(): Boolean = settingsRepository.isWalletScrollPreviewEnabled()
}