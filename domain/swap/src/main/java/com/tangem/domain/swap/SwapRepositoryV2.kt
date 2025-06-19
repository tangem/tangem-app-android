package com.tangem.domain.swap

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet

interface SwapRepositoryV2 {

    /**
     * Express swap pairs, both direct and reversed
     *
     * @param userWallet selected user wallet
     * @param initialCurrency currency being swapped (either to or from)
     * @param cryptoCurrencyStatusList list of currencies might be swapped
     */
    suspend fun getPairs(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyStatusList: List<CryptoCurrencyStatus>,
    ): List<SwapPairModel>

    /** Express getPairs request variant without providers request */
    suspend fun getPairsOnly(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyList: List<CryptoCurrency>,
    ): List<SwapPairModel>
}