package com.tangem.feature.swap.domain.api

import arrow.core.Either
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.domain.*
import java.math.BigDecimal

interface SwapRepository {

    suspend fun getPairs(initialCurrency: LeastTokenInfo, currencyList: List<CryptoCurrency>): PairsWithProviders

    /** Express getPairs request variant without providers request */
    suspend fun getPairsOnly(initialCurrency: LeastTokenInfo, currencyList: List<CryptoCurrency>): PairsWithProviders

    suspend fun getRates(currencyId: String, tokenIds: List<String>): Map<String, Double>

    suspend fun getExchangeStatus(txId: String): Either<UnknownError, ExchangeStatusModel>

    @Suppress("LongParameterList")
    suspend fun findBestQuote(
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: String,
        fromDecimals: Int,
        toDecimals: Int,
        providerId: String,
        rateType: RateType,
    ): Either<DataError, QuoteModel>

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
    @Throws(IllegalStateException::class)
    suspend fun getApproveData(
        userWalletId: UserWalletId,
        networkId: String,
        derivationPath: String?,
        currency: CryptoCurrency,
        amount: BigDecimal?,
        spenderAddress: String,
    ): String

    @Suppress("LongParameterList")
    suspend fun getExchangeData(
        fromContractAddress: String,
        fromNetwork: String,
        toContractAddress: String,
        toNetwork: String,
        fromAmount: String,
        fromDecimals: Int,
        toDecimals: Int,
        providerId: String,
        rateType: RateType,
        toAddress: String,
        refundAddress: String? = null, // for cex only
        refundExtraId: String? = null, // for cex only
    ): Either<DataError, SwapDataModel>

    fun getNativeTokenForNetwork(networkId: String): CryptoCurrency
}