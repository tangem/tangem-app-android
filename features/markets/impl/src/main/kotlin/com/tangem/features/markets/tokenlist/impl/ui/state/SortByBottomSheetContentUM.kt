package com.tangem.features.markets.tokenlist.impl.ui.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class SortByBottomSheetContentUM(
    val selectedOption: SortByTypeUM,
    val onOptionClicked: (SortByTypeUM) -> Unit,
) : TangemBottomSheetConfigContent
