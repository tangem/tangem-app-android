package com.tangem.feature.swap.models.states

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.domain.models.ui.FeeType
import kotlinx.collections.immutable.ImmutableList

data class ChooseFeeBottomSheetConfig(
    val selectedFee: FeeType,
    val onSelectFeeType: (FeeType) -> Unit,
    val feeItems: ImmutableList<FeeItemState.Content>,
    val readMoreUrl: String,
    val readMore: TextReference,
    val onReadMoreClick: (String) -> Unit,
) : TangemBottomSheetConfigContent