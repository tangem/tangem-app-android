package com.tangem.features.staking.impl.presentation.state.bottomsheet

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.staking.impl.presentation.state.FeeState

internal data class TonInitializeAccountBottomSheetConfig(
    val title: TextReference,
    val message: TextReference,
    val footer: TextReference,
    val onButtonClick: () -> Unit,
    val isButtonEnabled: Boolean,
    val isButtonLoading: Boolean,
    val feeState: FeeState,
) : TangemBottomSheetConfigContent