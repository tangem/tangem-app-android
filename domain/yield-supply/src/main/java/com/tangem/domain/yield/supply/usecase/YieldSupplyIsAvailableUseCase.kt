package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository

class YieldSupplyIsAvailableUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        if (cryptoCurrency.network.derivationPath is Network.DerivationPath.Joint) return false

        return yieldSupplyRepository.isYieldSupplySupported(userWalletId, cryptoCurrency)
    }
}