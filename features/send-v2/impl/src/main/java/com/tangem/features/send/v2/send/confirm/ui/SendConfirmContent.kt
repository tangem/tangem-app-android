package com.tangem.features.send.v2.send.confirm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.footers.SendingText
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.common.ui.tapHelp
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.subcomponents.amount.SendAmountBlockComponent
import com.tangem.features.send.v2.subcomponents.destination.DefaultSendDestinationBlockComponent
import com.tangem.features.send.v2.subcomponents.fee.SendFeeBlockComponent
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
    feeBlockComponent: SendFeeBlockComponent,
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
                uiState = sendUM,
                destinationBlockComponent = destinationBlockComponent,
                amountBlockComponent = amountBlockComponent,
                feeBlockComponent = feeBlockComponent,
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
        }
        SendingText(footerText = confirmUM?.sendingFooter ?: TextReference.EMPTY)
    }
}

private fun LazyListScope.blocks(
    uiState: SendUM,
    destinationBlockComponent: DefaultSendDestinationBlockComponent,
    amountBlockComponent: SendAmountBlockComponent,
    feeBlockComponent: SendFeeBlockComponent,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
) {
    item(key = BLOCKS_KEY) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (uiState.isRedesignEnabled) {
                amountBlockComponent.Content(modifier = Modifier)
                destinationBlockComponent.Content(modifier = Modifier)
                feeSelectorBlockComponent.Content(
                    modifier = Modifier
                        .clip(TangemTheme.shapes.roundedCornersXMedium)
                        .background(TangemTheme.colors.background.action),
                )
            } else {
                TransactionDoneTitleAnimated(uiState)
                destinationBlockComponent.Content(modifier = Modifier)
                amountBlockComponent.Content(modifier = Modifier)
                feeBlockComponent.Content(modifier = Modifier)
            }
        }
    }
}

@Composable
internal fun TransactionDoneTitleAnimated(uiState: SendUM) {
    AnimatedVisibility(
        visible = uiState.confirmUM is ConfirmUM.Success,
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        val wrappedConfirmUM = remember(this) { uiState.confirmUM as ConfirmUM.Success }
        TransactionDoneTitle(
            title = resourceReference(R.string.sent_transaction_sent_title),
            subtitle = resourceReference(
                R.string.send_date_format,
                wrappedList(
                    wrappedConfirmUM.transactionDate.toTimeFormat(DateTimeFormatters.dateFormatter),
                    wrappedConfirmUM.transactionDate.toTimeFormat(),
                ),
            ),
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}