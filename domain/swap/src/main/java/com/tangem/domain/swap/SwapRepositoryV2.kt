package com.tangem.domain.swap

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.swap.models.SwapQuoteModel
import com.tangem.domain.swap.models.SwapStatusModel
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import java.math.BigDecimal

/**
 * Swap repository
 */
@Suppress("LongParameterList")
interface SwapRepositoryV2 {

    /**
     * Express swap pairs, both direct and reversed
     *
     * @param userWallet selected user wallet
     * @param initialCurrency currency being swapped (either to or from)
     * @param cryptoCurrencyStatusList list of currencies might be swapped
     * @param filterProviderTypes filters only specified provider types, if empty returns providers as is
     */
    suspend fun getPairs(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyStatusList: List<CryptoCurrencyStatus>,
        filterProviderTypes: List<ExpressProviderType>,
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
    suspend fun getSwapQuote(
        userWallet: UserWallet,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        fromAmount: BigDecimal,
        provider: ExpressProvider,
        rateType: ExpressRateType,
    ): SwapQuoteModel

    /**
     * Returns swap data [SwapDataModel] ready to sign and send on selected quote
     *
     * @param userWallet selected user wallet
     * @param fromCryptoCurrencyStatus currency status being swapped from
     * @param toCryptoCurrencyStatus currency status being swapped to
     * @param fromAmount swap amount
     * @param toAddress destination address (optional, if null send to self)
     * @param expressProvider selected swap provider
     * @param rateType selected provider rate type
     */
    suspend fun getSwapData(
        userWallet: UserWallet,
        fromCryptoCurrencyStatus: CryptoCurrencyStatus,
        toCryptoCurrencyStatus: CryptoCurrencyStatus,
        fromAmount: String,
        toAddress: String?,
        expressProvider: ExpressProvider,
        rateType: ExpressRateType,
    ): SwapDataModel

    /**
     * Send ExpressApi info that swap transaction occurred
     *
     * @param userWallet selected user wallet
     * @param fromCryptoCurrencyStatus currency status being swapped from
     * @param toAddress swap destination address
     * @param txId transaction id in ExpressApi
     * @param txHash transaction hash in blockchain
     * @param txExtraId extra transaction id in ExpressApi
     */
    suspend fun swapTransactionSent(
        userWallet: UserWallet,
        fromCryptoCurrencyStatus: CryptoCurrencyStatus,
        toAddress: String,
        txId: String,
        txHash: String,
        txExtraId: String?,
    )

    /**
     * Returns status [SwapStatusModel] on active swap
     *
     * @param userWallet selected user wallet
     * @param txId transaction id in ExpressApi
     */
    suspend fun getExchangeStatus(userWallet: UserWallet, txId: String): SwapStatusModel
}