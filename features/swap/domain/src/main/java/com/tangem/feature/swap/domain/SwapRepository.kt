package com.tangem.feature.swap.domain

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.*
import java.math.BigDecimal

interface SwapRepository {

    suspend fun getPairs(initialCurrency: LeastTokenInfo, currencyList: List<CryptoCurrency>): List<SwapPairLeast>

    suspend fun getRates(currencyId: String, tokenIds: List<String>): Map<String, Double>

    suspend fun getExchangeableTokens(networkId: String): List<Currency>

    suspend fun findBestQuote(
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: String,
        providerId: Int,
        rateType: RateType,
    ): AggregatedSwapDataModel<QuoteModel>

    /**
     * Returns address of 1inch router that must be trusted
     *
     * @return address
     */
    suspend fun addressForTrust(networkId: String): String

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

    @Throws(IllegalStateException::class)
    suspend fun getAllowance(
        userWalletId: UserWalletId,
        networkId: String,
        derivationPath: String?,
        tokenDecimalCount: Int,
        tokenAddress: String,
    ): BigDecimal

    @Throws(IllegalStateException::class)
    suspend fun getApproveData(
        userWalletId: UserWalletId,
        networkId: String,
        derivationPath: String?,
        currency: CryptoCurrency,
        amount: BigDecimal?,
    ): String

    suspend fun getExchangeQuote(
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: String,
        providerId: Int,
        rateType: RateType,
    ) : ExchangeQuote
}
