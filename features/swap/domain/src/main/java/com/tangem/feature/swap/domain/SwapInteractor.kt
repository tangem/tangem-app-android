package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.SwapApproveType
import com.tangem.feature.swap.domain.models.ui.*

interface SwapInteractor {

    fun initDerivationPath(derivationPath: String?)

    /**
     * Init tokens to swap, load tokens list available to swap for given network
     *
     * @param initialCurrency currency which to swap or receive
     * @return [TokensDataState] that contains info about all available to swap tokens for networkId
     * and preselected tokens which initially select to swap
     */
    suspend fun initTokensToSwap(initialCurrency: Currency): TokensDataState

    /**
     * On search token, locally search tokens in previously loaded list to swap
     * searching in names and symbols
     *
     * @param networkId networkId for tokens
     * @param searchQuery string query for search
     * @return [FoundTokensState] that contains list of tokens matching condition query
     */
    suspend fun searchTokens(networkId: String, searchQuery: String): FoundTokensState

    /**
     * Find specific token by id, null if not found
     *
     * @param id token id
     * @return [Currency] or null
     */
    fun findTokenById(id: String): Currency?

    /**
     * Gives permission to swap, this starts scan card process
     *
     * @param networkId network in which selected token
     * @param approveData tx data to give approve, it loaded from 1inch in findBestQuote if needed
     * @param forTokenContractAddress token contract address for which needs permission
     * @param fromToken which token will be swapping
     * @param approveType unlimited or tx amount approve
     */
    @Throws(IllegalStateException::class)
    suspend fun givePermissionToSwap(
        networkId: String,
        approveData: RequestApproveStateData,
        forTokenContractAddress: String,
        fromToken: Currency,
        approveType: SwapApproveType,
    ): TxState

    /**
     * Find best quote for given tokens to swap
     * under the hood calls different methods to receive data, depends on permission for given token
     *
     * @param networkId network for tokens
     * @param fromToken [Currency] from which want to swap
     * @param toToken [Currency] that receive after swap
     * @param amountToSwap amount you want to swap
     * @return
     */
    @Throws(IllegalStateException::class)
    suspend fun findBestQuote(
        networkId: String,
        fromToken: Currency,
        toToken: Currency,
        amountToSwap: String,
    ): SwapState

    /**
     * Starts swap transaction, perform sign transaction
     *
     * @param networkId network for tokens
     * @param swapStateData tx data to swap, contains data to sign
     * @param currencyToSend [Currency]
     * @param currencyToGet [Currency]
     * @param amountToSwap amount to swap
     * @return [TxState]
     */
    @Throws(IllegalStateException::class)
    suspend fun onSwap(
        networkId: String,
        swapStateData: SwapStateData,
        currencyToSend: Currency,
        currencyToGet: Currency,
        amountToSwap: String,
    ): TxState

    /**
     * Returns token in wallet balance
     *
     * @param networkId
     * @param token
     */
    fun getTokenBalance(networkId: String, token: Currency): SwapAmount

    fun isAvailableToSwap(networkId: String): Boolean
}