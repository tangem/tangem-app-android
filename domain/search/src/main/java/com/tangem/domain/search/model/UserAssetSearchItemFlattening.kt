package com.tangem.domain.search.model

import com.tangem.domain.models.portfolio.UserAssetEntry

/**
 * Undoes the use case's per-token grouping: search shows every holding as its own row inside its
 * wallet/account section, so the grouped shape has nothing to render.
 *
 * Token order (fiat descending) is preserved, which after regrouping by wallet leaves rows inside each
 * account section in fiat-descending order.
 *
 * TODO: [TWI-1608] once the legacy search screen is deleted with the toggle, have
 *  GetSearchResultsUseCase return the entries directly and drop UserAssetSearchItem.Grouped.
 */
fun List<UserAssetSearchItem>.toEntries(): List<UserAssetEntry> = flatMap { item ->
    when (item) {
        is UserAssetSearchItem.Single -> listOf(item.entry)
        is UserAssetSearchItem.Grouped -> item.entries
    }
}