package com.tangem.domain.swap

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.swap.models.SwapQuoteModel
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import java.math.BigDecimal

/**
 * Swap repository
 */
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

    /**
     * Returns swap quotes on selected pair
     *
     * @param userWallet selected user wallet
     * @param fromCryptoCurrency currency being swapped from
     * @param toCryptoCurrency currency being swapped to
     * @param fromAmount swap amount
     * @param provider selected express provider
     * @param rateType rate type
     */
    @Suppress("LongParameterList")
    suspend fun getSwapQuote(
        userWallet: UserWallet,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        fromAmount: BigDecimal,
        provider: ExpressProvider,
        rateType: ExpressRateType,
    ): SwapQuoteModel
}