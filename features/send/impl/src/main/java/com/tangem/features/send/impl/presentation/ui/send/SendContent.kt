package com.tangem.features.send.impl.presentation.ui.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.state.SendUiState
import kotlinx.collections.immutable.ImmutableList

private const val TAP_HELP_KEY = "TAP_HELP_KEY"
private const val BLOCKS_KEY = "BLOCKS_KEY"

@Suppress("LongMethod")
@Composable
internal fun SendContent(uiState: SendUiState) {
    val sendState = uiState.sendState
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        blocks(uiState)
        tapHelp(isDisplay = sendState.notifications.isEmpty() && !uiState.sendState.isSuccess)
        notifications(sendState.notifications)
    }
}

private fun LazyListScope.blocks(uiState: SendUiState) {
    val amountState = uiState.amountState ?: return
    val recipientState = uiState.recipientState ?: return
    val feeState = uiState.feeState ?: return
    val sendState = uiState.sendState
    val isSuccess = sendState.isSuccess
    val timestamp = sendState.transactionDate

    item(key = BLOCKS_KEY) {
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12)) {
            AnimatedVisibility(
                visible = isSuccess,
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
            ) {
                TransactionDoneTitle(
                    titleRes = R.string.sent_transaction_sent_title,
                    date = timestamp,
                )
            }
            RecipientBlock(
                recipientState = recipientState,
                isSuccess = isSuccess,
                isEditingDisabled = uiState.isEditingDisabled,
                onClick = uiState.clickIntents::showRecipient,
            )
            AmountBlock(
                amountState = amountState,
                isSuccess = isSuccess,
                isEditingDisabled = uiState.isEditingDisabled,
                onClick = uiState.clickIntents::showAmount,
            )
            FeeBlock(
                feeState = feeState,
                isSuccess = isSuccess,
                onClick = uiState.clickIntents::showFee,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.tapHelp(isDisplay: Boolean, modifier: Modifier = Modifier) {
    if (isDisplay) {
        item(key = TAP_HELP_KEY) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxWidth()
                    .animateItemPlacement(),
            ) {
                val background = TangemTheme.colors.button.secondary
                Icon(
                    painter = painterResource(id = R.drawable.send_hint_shape_12),
                    tint = TangemTheme.colors.button.secondary,
                    contentDescription = null,
                    modifier = Modifier,
                )
                Text(
                    text = stringResource(id = R.string.send_summary_tap_hint),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.secondary,
                    modifier = Modifier
                        .clip(TangemTheme.shapes.roundedCornersXMedium)
                        .background(background)
                        .padding(
                            horizontal = TangemTheme.dimens.spacing14,
                            vertical = TangemTheme.dimens.spacing12,
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.notifications(configs: ImmutableList<SendNotification>, modifier: Modifier = Modifier) {
    items(
        items = configs,
        key = { it::class.java },
        contentType = { it::class.java },
        itemContent = {
            Notification(
                config = it.config,
                modifier = modifier.animateItemPlacement(),
                containerColor = when (it) {
                    is SendNotification.Warning.HighFeeError -> TangemTheme.colors.background.action
                    else -> TangemTheme.colors.button.disabled
                },
                iconTint = when (it) {
                    is SendNotification.Error -> TangemTheme.colors.icon.warning
                    is SendNotification.Warning -> null
                },
            )
        },
    )
}
