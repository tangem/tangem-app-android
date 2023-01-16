package com.tangem.tap.domain.totalBalance

import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletStoreModel

interface TotalFiatBalanceCalculator {

    /**
     * Calculate total fiat balance for list of [WalletStoreModel]
     * @param walletStores List of [WalletStoreModel] to calculate fiat amount
     * @param initial Initial [TotalFiatBalance] state, used when list of [WalletStoreModel] is empty
     * @return [TotalFiatBalance] with state found with the [WalletStoreModel] list
     * */
    suspend fun calculate(walletStores: List<WalletStoreModel>, initial: TotalFiatBalance): TotalFiatBalance

    /**
     * Same as [TotalFiatBalanceCalculator.calculate] but returns null if list of [WalletStoreModel] is empty
     * @param walletStores List of [WalletStoreModel] to calculate fiat amount
     * @return [TotalFiatBalance] with state found with the [WalletStoreModel] list
     * */
    suspend fun calculateOrNull(walletStores: List<WalletStoreModel>): TotalFiatBalance?

    companion object
}
