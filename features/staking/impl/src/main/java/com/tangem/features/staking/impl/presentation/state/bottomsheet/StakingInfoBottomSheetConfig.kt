package com.tangem.features.staking.impl.presentation.state.bottomsheet

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

data class StakingInfoBottomSheetConfig(
    val title: TextReference,
    val text: TextReference,
) : TangemBottomSheetConfigContent
