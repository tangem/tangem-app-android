package com.tangem.domain.balancehiding

import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IsBalanceHiddenUseCase(
    private val balanceHidingRepository: BalanceHidingRepository,
) {

    operator fun invoke(): Flow<Boolean> {
        return balanceHidingRepository.getBalanceHidingSettingsFlow().map {
            it.isBalanceHidden
        }
    }
}