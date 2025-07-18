package com.tangem.features.swap.v2.impl.sendviaswap.confirm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.swap.v2.impl.amount.SwapAmountBlockComponent
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
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
    modifier: Modifier = Modifier,
) {
    val confirmUM = sendWithSwapUM.confirmUM as? ConfirmUM.Content

    Column(modifier = modifier) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            item(key = "SendWithSwapAmountBlock") {
                amountBlockComponent.Content(Modifier)
            }
            item(key = "SendWithSwapDestinationBlock") {
                sendDestinationBlockComponent.Content(Modifier)
            }
            item(key = "SendWithSwapFeeBLock") {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                ) {
                    feeSelectorBlockComponent.Content(Modifier)
                }
            }
            if (confirmUM != null) {
                // tapHelp(isDisplay = confirmUM.showTapHelp) // todo
                with(sendNotificationsComponent) {
                    content(
                        state = sendNotificationsUM,
                        isClickDisabled = confirmUM.isTransactionInProcess,
                    )
                }
                // notifications(
                //     notifications = confirmUM.notifications,
                //     isClickDisabled = confirmUM.isTransactionInProcess,
                // )
            }
        }
        SpacerHMax()
        // todo
        // SendingText(footerText = confirmUM?.sendingFooter ?: TextReference.EMPTY)
    }
}