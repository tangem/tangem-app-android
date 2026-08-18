package com.tangem.features.feed.nav

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PreselectedMarketsInterval
import com.tangem.domain.markets.PreselectedMarketsOrder
import com.tangem.domain.markets.PreselectedTokenDetailsSection
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.domain.news.model.NewsListConfig
import kotlinx.serialization.Serializable

/**
 * Marker of a destination hosted by the feed stack. Deliberately NOT sealed: a feature declares its
 * routes in its own module and contributes the matching screen via [FeedScreenFactory] — the feed
 * host resolves routes through the registry, never through an exhaustive `when`.
 *
 * The routes nested below are owned by the feed itself; they migrate to their features' api modules
 * as those features become standalone plug-ins.
 */
interface FeedRoute : Route {

    /** true → pushing an already-present route brings it to front instead of stacking a duplicate. */
    val isSingleInstance: Boolean get() = false

    @Serializable
    data class MarketsTokenList(
        val preselectedOrder: PreselectedMarketsOrder? = null,
        val preselectedInterval: PreselectedMarketsInterval? = null,
    ) : FeedRoute {

        // markets list and search can reach each other without stacking duplicates
        override val isSingleInstance: Boolean get() = true
    }

    @Serializable
    data class MarketsTokenDetails(
        val token: TokenMarketParams,
        val appCurrency: AppCurrency,
        val shouldShowPortfolio: Boolean,
        val analyticsParams: AnalyticsParams? = null,
        val preselectedSection: PreselectedTokenDetailsSection? = null,
        val shouldOpenExchanges: Boolean = false,
        val exchangesCount: Int? = null,
    ) : FeedRoute {

        @Serializable
        data class AnalyticsParams(
            val blockchain: String?,
            val source: String,
            val newsId: Int? = null,
        )
    }

    @Serializable
    data class NewsList(val preselectedCategoryId: Int? = null) : FeedRoute

    @Serializable
    data class NewsDetails(
        val articleId: Int,
        val source: String,
        val preselectedArticlesId: List<Int> = emptyList(),
        val paginationConfig: NewsListConfig? = null,
    ) : FeedRoute

    @Serializable
    data class Earn(
        val preselectedEarnType: PreselectedEarnType? = null,
        val preselectedNetworkId: String? = null,
    ) : FeedRoute

    /**
     * The feed's search screen, pushed by the feed host when the shared search bar gains focus.
     *
     * @property source analytics context of the entry point
     */
    @Serializable
    data class Search(val source: String) : FeedRoute {

        override val isSingleInstance: Boolean get() = true
    }

    @Serializable
    data object ForYou : FeedRoute {

        override val isSingleInstance: Boolean get() = true
    }

    @Serializable
    data class TokenSummary(
        val token: Token,
        val selectedTokenPeriodId: String? = null,
    ) : FeedRoute {

        /**
         * The token the summary is opened for. Mirrors the for-you token-summary contract as pure
         * data, so the route doesn't couple the feed api to the for-you module.
         */
        @Serializable
        sealed interface Token {

            /** Opened from a portfolio screen, where the full [CryptoCurrency] is available. */
            @Serializable
            data class Portfolio(val cryptoCurrency: CryptoCurrency) : Token

            /**
             * Opened from a market-review screen: no [CryptoCurrency] yet, only the raw id and
             * display data.
             *
             * @property networks networks the token can be added to, already filtered by the
             * caller; empty when unresolved, which leaves the token unaddable
             */
            @Serializable
            data class Market(
                val cryptoCurrencyRawId: CryptoCurrency.RawID,
                val symbol: String,
                val title: String,
                val tangemIconUrl: String,
                val networks: List<TokenMarketInfo.Network> = emptyList(),
            ) : Token
        }
    }
}