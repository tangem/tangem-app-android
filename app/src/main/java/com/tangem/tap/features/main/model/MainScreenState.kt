package com.tangem.tap.features.main.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.event.StateEvent

internal data class MainScreenState(
    val toast: StateEvent<Toast>,
    val modalNotification: TangemBottomSheetConfig?,
)