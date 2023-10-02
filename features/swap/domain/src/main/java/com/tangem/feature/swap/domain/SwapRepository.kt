package com.tangem.feature.swap.domain

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.models.data.AggregatedSwapDataModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import java.math.BigDecimal

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

    suspend fun getCryptoCurrency(userWallet: UserWallet, currency: Currency, network: Network): CryptoCurrency?

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
        currency: Currency,
        amount: BigDecimal?,
    ): String
}
