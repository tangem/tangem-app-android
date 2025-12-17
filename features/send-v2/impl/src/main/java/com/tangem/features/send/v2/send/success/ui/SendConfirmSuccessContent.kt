package com.tangem.features.send.v2.send.success.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.amountScreen.ui.AmountBlock
import com.tangem.common.ui.navigationButtons.NavigationButtonsBlockV2
import com.tangem.core.ui.components.Fade
import com.tangem.core.ui.components.SpacerH
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
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(ANIMATION_DELAY)
        isVisible = true
    }

    val height = ANIMATION_OFFSET.toPx().toInt()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { height },
        ).plus(fadeIn()),
        exit = slideOutVertically().plus(fadeOut()),
        label = "Animate success content",
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(TangemTheme.colors.background.tertiary),
            ) {
                SuccessContent(
                    sendUM = sendUM,
                    destinationBlockComponent = destinationBlockComponent,
                    modifier = Modifier.fillMaxHeight(),
                )
                Fade(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    backgroundColor = TangemTheme.colors.background.tertiary,
                )
            }
            NavigationButtonsBlockV2(
                navigationUM = sendUM.navigationUM,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
            )
        }
    }
}

@Composable
private fun SuccessContent(
    sendUM: SendUM,
    destinationBlockComponent: SendDestinationBlockComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
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
        SpacerH(16.dp)
    }
}

private const val ANIMATION_DELAY = 600L
private val ANIMATION_OFFSET = (-40).dp