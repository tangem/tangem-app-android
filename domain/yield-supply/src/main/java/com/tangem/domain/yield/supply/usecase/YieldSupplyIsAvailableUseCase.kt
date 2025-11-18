package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository

class YieldSupplyIsAvailableUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return yieldSupplyRepository.isYieldSupplySupported(userWalletId, cryptoCurrency)
    }
}