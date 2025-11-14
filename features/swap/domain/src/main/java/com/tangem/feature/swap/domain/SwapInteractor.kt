package com.tangem.feature.swap.domain

import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.PermissionOptions
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.*
import java.math.BigDecimal

interface SwapInteractor {

    suspend fun getTokensDataState(currency: CryptoCurrency, filterInitial: Boolean): TokensDataStateExpress

    /**
     * Gives permission to swap, this starts scan card process
     *
     * @param networkId network in which selected token
     * @param permissionOptions data to give permissions
     */
    @Throws(IllegalStateException::class)
    suspend fun givePermissionToSwap(networkId: String, permissionOptions: PermissionOptions): SwapTransactionState

    /**
     * Find best quote for given tokens to swap
     * under the hood calls different methods to receive data, depends on permission for given token
     *
     * @param fromToken         token from which want to swap
     * @param fromAccount       account from which swap will be made
     * @param toToken           token that receive after swap
     * @param toAccount         account to which receive token after swap
     * @param providers         list of providers to find quote
     * @param amountToSwap      amount you want to swap
     * @param reduceBalanceBy   amount to reduce from balance (used for fee calculation)
     * @param selectedFee       selected fee to swap
     * @return
     */
    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun findBestQuote(
        fromToken: CryptoCurrencyStatus,
        fromAccount: Account.CryptoPortfolio?,
        toToken: CryptoCurrencyStatus,
        toAccount: Account.CryptoPortfolio?,
        providers: List<SwapProvider>,
        amountToSwap: String,
        reduceBalanceBy: BigDecimal,
        selectedFee: FeeType = FeeType.NORMAL,
    ): Map<SwapProvider, SwapState>

    /**
     * Starts swap transaction, perform sign transaction
     *
     * @param networkId network for tokens
     * @param swapStateData tx data to swap, contains data to sign
     * @param currencyToSend [Currency]
     * @param currencyToGet [Currency]
     * @param amountToSwap amount to swap
     * @param fee for tx
     * @return [SwapTransactionState]
     */
    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun onSwap(
        swapProvider: SwapProvider,
        swapData: SwapDataModel?,
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        fromAccount: Account.CryptoPortfolio?,
        toAccount: Account.CryptoPortfolio?,
        amountToSwap: String,
        includeFeeInAmount: IncludeFeeInAmount,
        fee: TxFee,
        expressOperationType: ExpressOperationType,
    ): SwapTransactionState

    // suspend fun updateQuotesStateWithSelectedFee(
    //     state: SwapState.QuotesLoadedState,
    //     selectedFee: FeeType,
    //     fromToken: CryptoCurrencyStatus,
    //     amountToSwap: String,
    //     reduceBalanceBy: BigDecimal,
    // ): SwapState.QuotesLoadedState

    /**
     * Returns token in wallet balance
     *
     * @param token
     */
    fun getTokenBalance(token: CryptoCurrencyStatus): SwapAmount

    suspend fun getInitialCurrencyToSwap(
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
        isReverseFromTo: Boolean,
    ): CryptoCurrencyStatus?

    /**
     * Returns initial currency to swap as AccountSwapCurrency
     *
     * @param initialCryptoCurrency initial currency selected to swap
     * @param state current tokens data state
     * @param isReverseFromTo flag indicating the direction of the swap
     */
    suspend fun getInitialCurrencyToSwapV2(
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
        isReverseFromTo: Boolean,
    ): AccountSwapCurrency?

    fun getNativeToken(networkId: String): CryptoCurrency

    interface Factory {
        fun create(selectedWalletId: UserWalletId): SwapInteractor
    }
}