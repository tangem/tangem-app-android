package com.tangem.domain.balancehiding

import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GetBalanceHidingSettingsUseCase(
    private val balanceHidingRepository: BalanceHidingRepository,
) {

    operator fun invoke(): Flow<BalanceHidingSettings> {
        return balanceHidingRepository.getBalanceHidingSettingsFlow()
    }

    fun isBalanceHidden(): Flow<Boolean> = invoke()
        .map { it.isBalanceHidden }
        .distinctUntilChanged()
}