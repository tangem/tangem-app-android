package com.tangem.domain.search.model

import com.tangem.domain.models.portfolio.UserAssetEntry

sealed interface UserAssetSearchItem {

    data class Single(val entry: UserAssetEntry) : UserAssetSearchItem

    data class Grouped(
        val tokenName: String,
        val tokenSymbol: String,
        val tokenIconUrl: String?,
        val entries: List<UserAssetEntry>,
    ) : UserAssetSearchItem
}