package com.tangem.features.swap.v2.impl.sendviaswap.confirm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.footers.SendingText
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.notifications
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.swap.v2.impl.amount.SwapAmountBlockComponent
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
@Composable
internal fun SendWithSwapConfirmContent(
    sendWithSwapUM: SendWithSwapUM,
    amountBlockComponent: SwapAmountBlockComponent,
    sendDestinationBlockComponent: SendDestinationBlockComponent,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    sendNotificationsComponent: SendNotificationsComponent,
    sendNotificationsUM: ImmutableList<NotificationUM>,
    swapNotificationsComponent: SwapNotificationsComponent,
    swapNotificationsUM: ImmutableList<NotificationUM>,
    modifier: Modifier = Modifier,
) {
    val confirmUM = sendWithSwapUM.confirmUM as? ConfirmUM.Content

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            item(key = "SendWithSwapBlocks") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    amountBlockComponent.Content(Modifier)
                    sendDestinationBlockComponent.Content(Modifier)
                    feeSelectorBlockComponent.Content(Modifier.clip(RoundedCornerShape(16.dp)))
                }
            }
            if (confirmUM != null) {
                // tapHelp(isDisplay = confirmUM.showTapHelp) // todo
                with(swapNotificationsComponent) {
                    content(
                        state = swapNotificationsUM,
                        isClickDisabled = confirmUM.isTransactionInProcess,
                    )
                }
                with(sendNotificationsComponent) {
                    content(
                        state = sendNotificationsUM,
                        isClickDisabled = confirmUM.isTransactionInProcess,
                    )
                }
                notifications(
                    notifications = confirmUM.notifications,
                    isClickDisabled = confirmUM.isTransactionInProcess,
                )
            }
        }
        SpacerHMax()
        SendingText(footerText = confirmUM?.sendingFooter ?: TextReference.EMPTY)
    }
}