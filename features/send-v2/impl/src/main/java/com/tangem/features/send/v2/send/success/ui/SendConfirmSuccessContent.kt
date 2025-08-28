package com.tangem.features.send.v2.send.success.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.amountScreen.ui.AmountBlock
import com.tangem.common.ui.navigationButtons.NavigationButtonsBlockV2
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toPx
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.send.v2.common.ui.FeeBlock
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.ui.state.SendUM
import kotlinx.coroutines.delay

@Composable
internal fun SendConfirmSuccessContent(sendUM: SendUM, destinationBlockComponent: SendDestinationBlockComponent) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(ANIMATION_DELAY)
        visible = true
    }

    val height = ANIMATION_OFFSET.toPx().toInt()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { height },
        ).plus(fadeIn()),
        exit = slideOutVertically().plus(fadeOut()),
        label = "Animate success content",
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.tertiary),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .scrollable(
                        state = rememberScrollState(),
                        orientation = Orientation.Vertical,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sendUM.confirmUM is ConfirmUM.Success) {
                    TransactionDoneTitle(
                        title = resourceReference(R.string.sent_transaction_sent_title),
                        subtitle = resourceReference(
                            R.string.send_date_format,
                            wrappedList(
                                sendUM.confirmUM.transactionDate.toTimeFormat(DateTimeFormatters.dateFormatter),
                                sendUM.confirmUM.transactionDate.toTimeFormat(),
                            ),
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                AmountBlock(
                    amountState = sendUM.amountUM,
                    isClickDisabled = true,
                    isEditingDisabled = true,
                    onClick = {},
                )
                destinationBlockComponent.Content(modifier = Modifier)
                FeeBlock(feeSelectorUM = sendUM.feeSelectorUM)
                Spacer(Modifier.height(60.dp))
            }
            BottomFade(Modifier.align(Alignment.BottomCenter), TangemTheme.colors.background.tertiary)
            NavigationButtonsBlockV2(
                navigationUM = sendUM.navigationUM,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
            )
        }
    }
}

private const val ANIMATION_DELAY = 600L
private val ANIMATION_OFFSET = (-40).dp