package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import java.math.BigDecimal

class GetPredictionTopUpMinimumUseCase(
    private val requisitesStore: PredictionDepositRequisitesStore,
) {

    /**
     * @param receiveCurrency the deposit token the top-up arrives in
     * @return the bridge minimum in USD for [receiveCurrency], or `null` when none is known this session
     */
    suspend operator fun invoke(userWalletId: UserWalletId, receiveCurrency: CryptoCurrency): BigDecimal? =
        requisitesStore.getSyncOrNull(userWalletId)
            ?.options
            ?.firstOrNull { it.asset.isAssetOf(receiveCurrency) }
            ?.target
            ?.token
            ?.minCheckoutUsd
}