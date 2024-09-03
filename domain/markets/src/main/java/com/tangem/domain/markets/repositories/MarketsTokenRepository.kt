package com.tangem.domain.markets.repositories

import com.tangem.domain.markets.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface MarketsTokenRepository {

    fun getTokenListFlow(
        batchingContext: TokenListBatchingContext,
        firstBatchSize: Int,
        nextBatchSize: Int,
    ): TokenListBatchFlow

    suspend fun getChart(fiatCurrencyCode: String, interval: PriceChangeInterval, tokenId: String): TokenChart

    suspend fun getChartPreview(fiatCurrencyCode: String, interval: PriceChangeInterval, tokenId: String): TokenChart

    suspend fun getTokenInfo(fiatCurrencyCode: String, tokenId: String, languageCode: String): TokenMarketInfo

    suspend fun getTokenQuotes(fiatCurrencyCode: String, tokenId: String): TokenQuotes

    suspend fun createCryptoCurrency(
        userWalletId: UserWalletId,
        token: TokenMarketParams,
        network: TokenMarketInfo.Network,
    ): CryptoCurrency?
}
