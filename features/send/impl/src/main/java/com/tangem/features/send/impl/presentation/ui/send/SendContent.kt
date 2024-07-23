package com.tangem.features.send.impl.presentation.ui.send

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.amountScreen.ui.AmountBlock
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.previewdata.ConfirmStatePreviewData
import com.tangem.features.send.impl.presentation.state.previewdata.SendStatesPreviewData
import com.tangem.features.send.impl.presentation.ui.common.notifications
import kotlinx.coroutines.delay

private const val TAP_HELP_KEY = "TAP_HELP_KEY"
private const val BLOCKS_KEY = "BLOCKS_KEY"
private const val TAP_HELP_ANIMATION_DELAY = 500L

@Suppress("LongMethod")
@Composable
internal fun SendContent(uiState: SendUiState) {
    val sendState = uiState.sendState ?: return
    val isClickDisabled = sendState.isSending || sendState.isSuccess
    LazyColumn(
        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        blocks(uiState)
        tapHelp(isDisplay = sendState.showTapHelp)
        notifications(notifications = sendState.notifications, isClickDisabled = isClickDisabled)
    }
}

private fun LazyListScope.blocks(uiState: SendUiState) {
    val amountState = uiState.amountState
    val recipientState = uiState.recipientState ?: return
    val feeState = uiState.feeState ?: return
    val sendState = uiState.sendState ?: return
    val isSuccess = sendState.isSuccess
    val isClickDisabled = sendState.isSending || isSuccess
    val timestamp = sendState.transactionDate

    item(key = BLOCKS_KEY) {
        Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12)) {
            AnimatedVisibility(
                visible = isSuccess,
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
            ) {
                TransactionDoneTitle(
                    titleRes = R.string.sent_transaction_sent_title,
                    date = timestamp,
                )
            }
            RecipientBlock(
                recipientState = recipientState,
                isClickDisabled = isClickDisabled,
                isEditingDisabled = uiState.isEditingDisabled,
                onClick = uiState.clickIntents::showRecipient,
            )
            AmountBlock(
                amountState = amountState,
                isClickDisabled = isClickDisabled,
                isEditingDisabled = uiState.isEditingDisabled,
                onClick = uiState.clickIntents::showAmount,
            )
            FeeBlock(
                feeState = feeState,
                isClickDisabled = isClickDisabled,
                onClick = uiState.clickIntents::showFee,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.tapHelp(isDisplay: Boolean, modifier: Modifier = Modifier) {
    item(key = TAP_HELP_KEY) {
        val animationState = remember { MutableTransitionState(false) }

        LaunchedEffect(key1 = isDisplay) {
            delay(TAP_HELP_ANIMATION_DELAY)
            animationState.targetState = isDisplay
        }

        AnimatedVisibility(
            visibleState = animationState,
            label = "Tap Help Animation",
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
            ).plus(fadeIn()),
            exit = slideOutVertically().plus(fadeOut()),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .padding(top = TangemTheme.dimens.spacing20),
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
                    style = TangemTheme.typography.body2,
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

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendContent_Preview(@PreviewParameter(SendContentPreviewProvider::class) uiState: SendUiState) {
    TangemThemePreview {
        SendContent(
            uiState = uiState,
        )
    }
}

private class SendContentPreviewProvider : PreviewParameterProvider<SendUiState> {
    override val values: Sequence<SendUiState>
        get() = sequenceOf(
            SendStatesPreviewData.uiState,
            SendStatesPreviewData.uiState.copy(sendState = ConfirmStatePreviewData.sendDoneState),
        )
}
// endregion