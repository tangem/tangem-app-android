package com.tangem.domain.balance_hiding

import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class ListenToFlipsUseCase(
    private val flipDetector: DeviceFlipDetector,
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(): Flow<Unit> {
        return flipDetector.deviceFlipEvents().onEach {
            val balanceHidingSettings = settingsRepository.getBalanceHidingSettings()

            settingsRepository.storeBalanceHidingSettings(balanceHidingSettings.copy(
                isBalanceHidden = !balanceHidingSettings.isBalanceHidden
            ))
        }
    }
}
