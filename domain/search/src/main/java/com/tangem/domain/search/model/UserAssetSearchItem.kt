package com.tangem.domain.search.model

sealed interface UserAssetSearchItem {

    data class Single(val entry: UserAssetSearchEntry) : UserAssetSearchItem

    data class Grouped(
        val tokenName: String,
        val tokenSymbol: String,
        val tokenIconUrl: String?,
        val entries: List<UserAssetSearchEntry>,
    ) : UserAssetSearchItem
}