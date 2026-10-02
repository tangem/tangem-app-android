package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import java.math.BigDecimal

class GetPredictionTopUpMinimumUseCase(
    private val requisitesStore: PredictionDepositRequisitesStore,
) {

    /** @return the bridge minimum in USD for the default deposit token, or `null` when none is known this session */
    suspend operator fun invoke(userWalletId: UserWalletId): BigDecimal? =
        requisitesStore.getSyncOrNull(userWalletId)?.default?.token?.minCheckoutUsd
}