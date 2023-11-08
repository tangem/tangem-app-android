package com.tangem.tap.features.main.model

import androidx.annotation.DrawableRes
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference

internal data class ModalNotification(
    @DrawableRes val iconResId: Int,
    val title: TextReference,
    val message: TextReference,
    val primaryAction: ActionConfig,
    val secondaryAction: ActionConfig?,
) : TangemBottomSheetConfigContent