package com.tangem.common.ui.bottomsheets.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class GiveTxPermissionBottomSheetConfig(
    val data: GiveTxPermissionState.ReadyForRequest,
    val onCancel: () -> Unit,
) : TangemBottomSheetConfigContent