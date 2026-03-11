package com.tangem.features.feed.ui.earn.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class EarnFilterByTypeBottomSheetContentUM(
    val selectedOption: EarnFilterTypeUM,
    val onOptionClick: (EarnFilterTypeUM) -> Unit,
) : TangemBottomSheetConfigContent