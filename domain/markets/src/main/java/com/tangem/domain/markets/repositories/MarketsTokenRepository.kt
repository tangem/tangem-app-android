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

    suspend fun getChart(
        fiatCurrencyCode: String,
        interval: PriceChangeInterval,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
    ): TokenChart

    suspend fun getChartPreview(
        fiatCurrencyCode: String,
        interval: PriceChangeInterval,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
    ): TokenChart

    suspend fun getTokenInfo(
        fiatCurrencyCode: String,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
        languageCode: String,
    ): TokenMarketInfo

    suspend fun getTokenQuotes(
        fiatCurrencyCode: String,
        tokenId: CryptoCurrency.RawID,
        tokenSymbol: String,
    ): TokenQuotes

    suspend fun createCryptoCurrency(
        userWalletId: UserWalletId,
        token: TokenMarketParams,
        network: TokenMarketInfo.Network,
    ): CryptoCurrency?

    /**
     * Get token exchanges
     *
     * @param tokenId token id
     */
    suspend fun getTokenExchanges(tokenId: CryptoCurrency.RawID): List<TokenMarketExchange>
}