package com.tangem.domain.balancehiding

import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow

class GetBalanceHidingSettingsUseCase(
    private val balanceHidingRepository: BalanceHidingRepository,
) {

    operator fun invoke(): Flow<BalanceHidingSettings> {
        return balanceHidingRepository.getBalanceHidingSettingsFlow()
    }
}