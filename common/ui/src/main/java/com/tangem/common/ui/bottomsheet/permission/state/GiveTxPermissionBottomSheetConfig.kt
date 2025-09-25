package com.tangem.common.ui.bottomsheet.permission.state

import androidx.annotation.DrawableRes
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

data class GiveTxPermissionBottomSheetConfig(
    val data: GiveTxPermissionState.ReadyForRequest,
    @DrawableRes val walletInteractionIcon: Int?,
    val onCancel: () -> Unit,
) : TangemBottomSheetConfigContent