package com.tangem.domain.balancehiding

import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class ListenToFlipsUseCase(
    private val flipDetector: DeviceFlipDetector,
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(): Flow<Unit> {
        return if (settingsRepository.getBalanceHidingSettings().isHidingEnabledInSettings) {
            flipDetector.deviceFlipEvents().onEach {
                val balanceHidingSettings = settingsRepository.getBalanceHidingSettings()

                settingsRepository.storeBalanceHidingSettings(
                    balanceHidingSettings.copy(
                        isBalanceHidden = !balanceHidingSettings.isBalanceHidden,
                    ),
                )
            }
        } else {
            flow { }
        }
    }
}
