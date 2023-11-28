package com.tangem.feature.swap.domain

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*

interface SwapInteractor {

    suspend fun getTokensDataState(currency: CryptoCurrency): TokensDataStateExpress

    fun initDerivationPathAndNetwork(derivationPath: String?, network: Network)

    /**
     * On search token, locally search tokens in previously loaded list to swap
     * searching in names and symbols
     *
     * @param networkId networkId for tokens
     * @param searchQuery string query for search
     * @return [FoundTokensStateExpress] that contains list of tokens matching condition query
     */
    suspend fun searchTokens(networkId: String, searchQuery: String): FoundTokensStateExpress

    /**
     * Gives permission to swap, this starts scan card process
     *
     * @param networkId network in which selected token
     * @param permissionOptions data to give permissions
     */
    @Throws(IllegalStateException::class)
    suspend fun givePermissionToSwap(networkId: String, permissionOptions: PermissionOptions): TxState

    /**
     * Find best quote for given tokens to swap
     * under the hood calls different methods to receive data, depends on permission for given token
     *
     * @param networkId network for tokens
     * @param fromToken [Currency] from which want to swap
     * @param toToken [Currency] that receive after swap
     * @param amountToSwap amount you want to swap
     * @param selectedFee selected fee to swap
     * @return
     */
    @Throws(IllegalStateException::class)
    suspend fun findBestQuote(
        networkId: String,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        providers: List<SwapProvider>,
        amountToSwap: String,
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
     * @return [TxState]
     */
    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun onSwap(
        swapProvider: SwapProvider,
        networkId: String,
        swapData: SwapDataModel?,
        currencyToSend: CryptoCurrencyStatus,
        currencyToGet: CryptoCurrencyStatus,
        amountToSwap: String,
        fee: TxFee,
    ): TxState

    suspend fun updateQuotesStateWithSelectedFee(
        state: SwapState.QuotesLoadedState,
        selectedFee: FeeType,
        fromToken: CryptoCurrencyStatus,
        amountToSwap: String,
        networkId: String,
    ): SwapState.QuotesLoadedState

    /**
     * Returns token in wallet balance
     *
     * @param networkId
     * @param token
     */
    fun getTokenBalance(networkId: String, token: CryptoCurrency): SwapAmount

    fun isAvailableToSwap(networkId: String): Boolean

    fun getSelectedWallet(): UserWallet?
}