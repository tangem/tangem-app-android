package com.tangem.feature.swap.domain.api

import arrow.core.Either
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.domain.*
import java.math.BigDecimal

interface SwapRepository {

    suspend fun getPairs(initialCurrency: LeastTokenInfo, currencyList: List<CryptoCurrency>): PairsWithProviders

    /** Express getPairs request variant without providers request */
    suspend fun getPairsOnly(initialCurrency: LeastTokenInfo, currencyList: List<CryptoCurrency>): PairsWithProviders

    suspend fun getExchangeStatus(txId: String): Either<UnknownError, ExchangeStatusModel>

    @Suppress("LongParameterList")
    suspend fun findBestQuote(
        userWalletId: UserWalletId,
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: String,
        fromDecimals: Int,
        toDecimals: Int,
        providerId: String,
        rateType: RateType,
    ): Either<ExpressDataError, QuoteModel>

    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun getAllowance(
        userWalletId: UserWalletId,
        networkId: String,
        derivationPath: String?,
        tokenDecimalCount: Int,
        tokenAddress: String,
        spenderAddress: String,
    ): BigDecimal

    @Suppress("LongParameterList")
    suspend fun getExchangeData(
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        fromAddress: String,
        toNetwork: String,
        fromAmount: String,
        fromDecimals: Int,
        toDecimals: Int,
        providerId: String,
        rateType: RateType,
        toAddress: String,
        refundAddress: String? = null, // for cex only
        refundExtraId: String? = null, // for cex only
    ): Either<ExpressDataError, SwapDataModel>

    // TODO: Add target error handling, remove either ([REDACTED_JIRA])
    @Suppress("LongParameterList")
    suspend fun exchangeSent(
        txId: String,
        fromNetwork: String,
        fromAddress: String,
        payInAddress: String,
        txHash: String,
        payInExtraId: String?,
    ): Either<ExpressDataError, Unit>

    fun getNativeTokenForNetwork(networkId: String): CryptoCurrency
}