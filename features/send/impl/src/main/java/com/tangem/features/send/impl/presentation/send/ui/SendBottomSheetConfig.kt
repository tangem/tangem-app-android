package com.tangem.features.send.impl.presentation.send.ui

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class SendBottomSheetConfig(
    val currentState: SendStates,
) : TangemBottomSheetConfigContent