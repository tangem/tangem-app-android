package com.tangem.feature.swap.models.states

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.feature.swap.models.SwapPermissionState

data class GivePermissionBottomSheetConfig(
    val data: SwapPermissionState.ReadyForRequest,
    val onCancel: () -> Unit,
) : TangemBottomSheetConfigContent