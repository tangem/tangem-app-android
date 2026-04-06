package com.tangem.domain.search.model

data class SearchResult(
    val textHints: List<SearchTextHint>,
    val recentTokens: List<RecentSearchToken>,
    val userAssets: List<UserAssetSearchEntry>,
)