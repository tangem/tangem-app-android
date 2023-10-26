package com.tangem.domain.balancehiding

import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

class ListenToFlipsUseCase(
    private val flipDetector: DeviceFlipDetector,
    private val balanceHidingRepository: BalanceHidingRepository,
) {

    suspend operator fun invoke(): Flow<Unit> {
        return if (balanceHidingRepository.getBalanceHidingSettings().isHidingEnabledInSettings) {
            flipDetector.getDeviceFlipFlow().onEach {
                val balanceHidingSettings = balanceHidingRepository.getBalanceHidingSettings()

                balanceHidingRepository.storeBalanceHidingSettings(
                    balanceHidingSettings.copy(
                        isBalanceHidden = !balanceHidingSettings.isBalanceHidden,
                    ),
                )
            }
        } else {
            emptyFlow()
        }
    }
}