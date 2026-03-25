package com.tangem.domain.search.model

import com.tangem.domain.markets.TokenMarket

data class SearchResult(
    val textHints: List<SearchTextHint>,
    val recentTokens: List<RecentSearchToken>,
    val userAssets: List<UserAssetSearchEntry>,
    val marketTokens: List<TokenMarket>,
)