package com.tangem.features.earn.flow.impl.ui.state

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.buttons.predefined.PredefinedPercentButtonUM
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * There is no fee field: the fee block is the send feature's `FeeSelectorBlockComponent`, passed to the screen
 * content directly. Fee problems arrive as [notifications], and any `NotificationUM.Error` disables [primary].
 */
internal data class EarnAmountUM(
    val title: TextReference,
    val amountState: AmountState,
    val clickIntents: AmountScreenClickIntents,
    val percents: ImmutableList<PredefinedPercentButtonUM>,
    val notifications: ImmutableList<NotificationUM>,
    val primary: TangemButtonUM,
)