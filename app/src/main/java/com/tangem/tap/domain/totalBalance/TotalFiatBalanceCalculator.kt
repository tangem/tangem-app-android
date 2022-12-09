package com.tangem.tap.domain.totalBalance

import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletStoreModel
import java.math.BigDecimal

interface TotalFiatBalanceCalculator {
    suspend fun calculate(
        prevAmount: BigDecimal,
        walletStores: List<WalletStoreModel>,
    ): TotalFiatBalance

    companion object
}