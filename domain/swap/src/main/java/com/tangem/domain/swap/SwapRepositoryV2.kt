package com.tangem.domain.swap

import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.*
import java.math.BigDecimal

/**
 * Swap repository
 */
@Suppress("LongParameterList")
interface SwapRepositoryV2 {

    /**
     * Express swap pairs, both direct and reversed
     *
     * @param userWallet                selected user wallet
     * @param initialCurrency           currency being swapped (either to or from)
     * @param cryptoCurrencyStatusList  list of currencies might be swapped
     * @param filterProviderTypes       filters only specified provider types, if empty returns providers as is
     * @param swapTxType                swap tx type
     */
    suspend fun getPairs(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyStatusList: List<CryptoCurrencyStatus>,
        filterProviderTypes: List<ExpressProviderType>,
        swapTxType: SwapTxType,
    ): List<SwapPairModel>

    /**
     * Express getPairs request variant for all pairs of crypto currency supported by Tangem Wallet
     * Therefore return list may and will include currencies not added to user wallet.
     * Used in [Send With Swap]
     */
    suspend fun getSupportedPairs(
        userWallet: UserWallet,
        initialCurrency: CryptoCurrency,
        cryptoCurrencyList: List<CryptoCurrency>,
        filterProviderTypes: List<ExpressProviderType>,
        swapTxType: SwapTxType,
    ): List<SwapPairModel>

    /**
     * Returns swap quotes on selected pair
     *
     * @param userWallet            selected user wallet
     * @param fromCryptoCurrency    currency being swapped from
     * @param toCryptoCurrency      currency being swapped to
     * @param fromAmount            swap amount
     * @param provider              selected express provider
     * @param rateType              rate type
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
     * @param userWallet                selected user wallet
     * @param fromCryptoCurrencyStatus  currency status being swapped from
     * @param toCryptoCurrency          currency being swapped to
     * @param fromAmount                swap amount
     * @param toAddress                 destination address
     * @param expressProvider           selected swap provider
     * @param rateType                  selected provider rate type
     * @param expressOperationType      operation type swap or send with swap
     */
    suspend fun getSwapData(
        userWallet: UserWallet,
        fromCryptoCurrencyStatus: CryptoCurrencyStatus,
        toCryptoCurrency: CryptoCurrency,
        fromAmount: String,
        toAddress: String,
        expressProvider: ExpressProvider,
        rateType: ExpressRateType,
        expressOperationType: ExpressOperationType,
    ): SwapDataModel

    /**
     * Send ExpressApi info that swap transaction occurred
     *
     * @param userWallet                selected user wallet
     * @param fromCryptoCurrencyStatus  currency status being swapped from
     * @param toAddress                 swap destination address
     * @param txId                      transaction id in ExpressApi
     * @param txHash                    transaction hash in blockchain
     * @param txExtraId                 extra transaction id in ExpressApi
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
     * @param userWallet    selected user wallet
     * @param txId          transaction id in ExpressApi
     */
    suspend fun getExchangeStatus(userWallet: UserWallet, txId: String): SwapStatusModel
}