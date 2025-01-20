package com.tangem.core.ui.components.bottomsheets.message

import androidx.annotation.DrawableRes
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

data class MessageBottomSheetUM(
    @DrawableRes val iconResId: Int?,
    val title: TextReference?,
    val message: TextReference,
    val primaryAction: ActionUM?,
    val secondaryAction: ActionUM?,
) : TangemBottomSheetConfigContent {

    data class ActionUM(
        val text: TextReference,
        val enabled: Boolean = true,
        val onClick: () -> Unit,
    )
}