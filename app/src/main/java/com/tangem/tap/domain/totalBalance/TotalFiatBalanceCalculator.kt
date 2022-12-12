package com.tangem.tap.domain.totalBalance

import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletStoreModel
import java.math.BigDecimal

interface TotalFiatBalanceCalculator {

    /**
     * Calculate total fiat balance for list of [WalletStoreModel]
     * @param prevAmount Previous amount, used in [TotalFiatBalance.Refreshing]
     * @param walletStores List of [WalletStoreModel] to calculate fiat amount
     * @param initial Initial [TotalFiatBalance] state, used when list of [WalletStoreModel] is empty
     * @return [TotalFiatBalance] with state found with the [WalletStoreModel] list
     * */
    suspend fun calculate(
        prevAmount: BigDecimal?,
        walletStores: List<WalletStoreModel>,
        initial: TotalFiatBalance,
    ): TotalFiatBalance

    /**
     * Same as [TotalFiatBalanceCalculator.calculate] but returns null if list of [WalletStoreModel] is empty
     * @param prevAmount Previous amount, used in [TotalFiatBalance.Refreshing]
     * @param walletStores List of [WalletStoreModel] to calculate fiat amount
     * @return [TotalFiatBalance] with state found with the [WalletStoreModel] list
     * */
    suspend fun calculateOrNull(
        prevAmount: BigDecimal?,
        walletStores: List<WalletStoreModel>,
    ): TotalFiatBalance?

    companion object
}
