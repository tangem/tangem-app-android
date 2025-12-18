package com.tangem.features.feed.ui.market.list.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class SortByBottomSheetContentUM(
    val selectedOption: SortByTypeUM,
    val onOptionClicked: (SortByTypeUM) -> Unit,
) : TangemBottomSheetConfigContent