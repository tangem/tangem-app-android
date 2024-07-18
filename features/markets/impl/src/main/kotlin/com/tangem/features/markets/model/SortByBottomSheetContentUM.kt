package com.tangem.features.markets.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.markets.ui.entity.SortByTypeUM

data class SortByBottomSheetContentUM(
    val selectedOption: SortByTypeUM,
    val onOptionClicked: (SortByTypeUM) -> Unit,
) : TangemBottomSheetConfigContent