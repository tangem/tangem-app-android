package com.tangem.features.feed.model.search.converter

import com.tangem.common.ui.markets.models.MarketsListItemUM

internal data class MarketsListItemUMWithAppCurrency(
    val item: MarketsListItemUM,
    val appCurrencyCode: String,
    val appCurrencySymbol: String,
)