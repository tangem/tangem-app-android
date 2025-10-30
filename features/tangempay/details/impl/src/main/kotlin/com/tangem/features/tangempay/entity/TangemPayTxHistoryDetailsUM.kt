package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.ColorReference
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayTxHistoryDetailsUM(
    val title: TextReference,
    val iconState: ImageReference,
    val transactionTitle: TextReference,
    val transactionSubtitle: TextReference,
    val transactionAmount: String,
    val transactionAmountColor: ColorReference,
    val labelState: LabelUM?,
    val notification: NotificationConfig?,
    val buttons: ImmutableList<ButtonState>,
    val dismiss: () -> Unit,
) {

    data class ButtonState(val text: TextReference, val onClick: () -> Unit, val startIcon: ImageReference.Res? = null)
}