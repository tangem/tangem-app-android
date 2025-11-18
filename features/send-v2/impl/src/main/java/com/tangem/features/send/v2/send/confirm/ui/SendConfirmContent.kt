package com.tangem.features.send.v2.send.confirm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.common.ui.tapHelp
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.subcomponents.amount.SendAmountBlockComponent
import com.tangem.features.send.v2.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.v2.subcomponents.notifications
import com.tangem.features.send.v2.subcomponents.notifications.DefaultSendNotificationsComponent
import kotlinx.collections.immutable.ImmutableList

private const val BLOCKS_KEY = "BLOCKS_KEY"

@Suppress("LongParameterList")
@Composable
internal fun SendConfirmContent(
    sendUM: SendUM,
    destinationBlockComponent: DefaultSendDestinationBlockComponent,
    amountBlockComponent: SendAmountBlockComponent,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    notificationsComponent: DefaultSendNotificationsComponent,
    notificationsUM: ImmutableList<NotificationUM>,
) {
    val confirmUM = sendUM.confirmUM as? ConfirmUM.Content

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TangemTheme.dimens.spacing16),
        ) {
            blocks(
                destinationBlockComponent = destinationBlockComponent,
                amountBlockComponent = amountBlockComponent,
                feeSelectorBlockComponent = feeSelectorBlockComponent,
            )
            if (confirmUM != null) {
                tapHelp(isDisplay = confirmUM.showTapHelp)
                with(notificationsComponent) {
                    content(
                        state = notificationsUM,
                        isClickDisabled = confirmUM.isSending,
                    )
                }
                notifications(
                    notifications = confirmUM.notifications,
                    isClickDisabled = confirmUM.isSending,
                )
            }
            item("SPACER_KEY") {
                SpacerH(16.dp)
            }
        }
    }
}

private fun LazyListScope.blocks(
    destinationBlockComponent: DefaultSendDestinationBlockComponent,
    amountBlockComponent: SendAmountBlockComponent,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
) {
    item(key = BLOCKS_KEY) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            amountBlockComponent.Content(modifier = Modifier)
            destinationBlockComponent.Content(modifier = Modifier)
            feeSelectorBlockComponent.Content(
                modifier = Modifier
                    .clip(TangemTheme.shapes.roundedCornersXMedium)
                    .background(TangemTheme.colors.background.action),
            )
        }
    }
}