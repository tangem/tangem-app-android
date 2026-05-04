package com.tangem.features.feed.model.market.list.state

internal data class SortByMenuUM(
    val selectedOption: SortByTypeUM,
    val onOptionClicked: (SortByTypeUM) -> Unit,
)