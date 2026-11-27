package com.tangem.features.feed.ui.earn.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

internal data class EarnFilterByTypeBottomSheetContentUM(
    val options: ImmutableList<EarnFilterTypeUM>,
    val selectedOption: EarnFilterTypeUM,
    val onOptionClick: (EarnFilterTypeUM) -> Unit,
) : TangemBottomSheetConfigContent