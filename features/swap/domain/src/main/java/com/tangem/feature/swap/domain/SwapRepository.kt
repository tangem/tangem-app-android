package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel

interface SwapRepository {

    suspend fun getRates(currencyId: String, tokenIds: List<String>): Map<String, Double>

    suspend fun getExchangeableTokens(networkId: String): List<Currency>

    suspend fun findBestQuote(
        networkId: String,
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
    ): AggregatedSwapDataModel<QuoteModel>

    /**
     * Returns address of 1inch router that must be trusted
     *
     * @return address
     */
    suspend fun addressForTrust(networkId: String): String

    /**
     * Generate "data" for calling contract in order to allow 1inch spend funds
     *
     * @param tokenAddress token you want to exchange
     * @param amount number of tokens is allowed. By default infinite
     */
    suspend fun dataToApprove(networkId: String, tokenAddress: String, amount: String? = null): ApproveModel

    /**
     * Get the number of tokens that the 1inch router is allowed to spend
     *
     * @param tokenAddress Token address you want to exchange
     * @param walletAddress address for which you want to check
     *
     * @return amount of tokens allowed to spend
     */
    suspend fun checkTokensSpendAllowance(
        networkId: String,
        tokenAddress: String,
        walletAddress: String,
    ): AggregatedSwapDataModel<String>

    @Suppress("LongParameterList")
    suspend fun prepareSwapTransaction(
        networkId: String,
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
        fromWalletAddress: String,
        slippage: Int,
    ): AggregatedSwapDataModel<SwapDataModel>

    /**
     * Returns a tangem fee for swap in percents
     * Example: 0.35%
     */
    fun getTangemFee(): Double
}
