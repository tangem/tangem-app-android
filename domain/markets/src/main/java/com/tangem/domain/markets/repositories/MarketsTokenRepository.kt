package com.tangem.domain.markets.repositories

import com.tangem.domain.markets.*
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

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
        token: RawMarketToken,
        network: TokenMarketInfo.Network,
        account: Account.CryptoPortfolio,
    ): CryptoCurrency?

    /**
     * Get token exchanges
     *
     * @param tokenId token id
     */
    suspend fun getTokenExchanges(tokenId: CryptoCurrency.RawID): List<TokenMarketExchange>

    /**
     * Get coin categories with their sectors
     *
     * Not cached: the answer carries the region restrictions, so a kept copy would outlive the country it
     * was taken for. Every call goes to the network.
     *
     * @param country ISO 3166-1 alpha-2 code the [CoinCategory.isRestricted] flag is evaluated for. Pass
     *                `null` when it is unknown — the categories then arrive unrestricted
     */
    suspend fun getCoinCategories(country: String?): List<CoinCategory>

    /**
     * Get one page of market categories with their aggregated market data
     *
     * @param timestamp pagination snapshot token: `null` for the first page, then the value the
     *                  previous page was fetched with, so the list stays stable while paging
     * @param featured  when `true`, only categories curated as featured are returned
     */
    suspend fun getMarketCategories(
        currency: String,
        offset: Int,
        limit: Int,
        timestamp: Long? = null,
        featured: Boolean? = null,
    ): MarketCategoryPage

    /** Get a paginated flow of market categories. Categories never update in place, so `TUpdate` is [Nothing] */
    fun getMarketCategoryListFlow(
        batchingContext: MarketCategoryListBatchingContext,
        firstBatchSize: Int,
        nextBatchSize: Int,
    ): MarketCategoryListBatchFlow
}