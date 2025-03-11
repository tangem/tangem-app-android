package com.tangem.features.send.impl.presentation.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.amountScreen.AmountScreenContent
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendUiCurrentScreen
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.previewdata.ConfirmStatePreviewData
import com.tangem.features.send.impl.presentation.state.previewdata.SendStatesPreviewData
import com.tangem.features.send.impl.presentation.ui.fee.SendSpeedAndFeeContent
import com.tangem.features.send.impl.presentation.ui.recipient.SendRecipientContent
import com.tangem.features.send.impl.presentation.ui.send.SendContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex

@Composable
internal fun SendScreen(uiState: SendUiState, currentState: SendUiCurrentScreen) {
    val snackbarHostState = remember { SnackbarHostState() }
    val onBackClick = uiState.clickIntents::onBackClick.takeIf {
        uiState.sendState?.isSending != true
    } ?: {}
    BackHandler(onBack = onBackClick)
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SendAppBar(
            uiState = uiState,
            currentState = currentState,
        )
        SendScreenContent(
            uiState = uiState,
            currentState = currentState,
            modifier = Modifier.weight(1f),
        )
        SendNavigationButtons(
            uiState = uiState,
            currentState = currentState,
        )
    }

    SendEventEffect(
        event = uiState.event,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SendAppBar(uiState: SendUiState, currentState: SendUiCurrentScreen) {
    val (titleRes, subtitleRes) = when (currentState.type) {
        SendUiStateType.Amount,
        SendUiStateType.EditAmount,
        -> resourceReference(R.string.send_amount_label) to null
        SendUiStateType.Recipient,
        SendUiStateType.EditRecipient,
        -> resourceReference(R.string.send_recipient_label) to null
        SendUiStateType.Fee,
        SendUiStateType.EditFee,
        -> resourceReference(R.string.common_fee_selector_title) to null
        SendUiStateType.Send -> if (uiState.sendState?.isSuccess == false) {
            resourceReference(R.string.send_summary_title, wrappedList(uiState.cryptoCurrencyName)) to
                (uiState.amountState as? AmountState.Data)?.title
        } else {
            null to null
        }
        else -> null to null
    }
    val iconRes = if (currentState.type == SendUiStateType.Recipient) {
        R.drawable.ic_qrcode_scan_24
    } else {
        null
    }
    val backIcon = when (currentState.type) {
        SendUiStateType.EditAmount,
        SendUiStateType.EditFee,
        SendUiStateType.EditRecipient,
        -> R.drawable.ic_back_24
        else -> R.drawable.ic_close_24
    }
    AppBarWithBackButtonAndIcon(
        text = titleRes?.resolveReference(),
        subtitle = subtitleRes?.resolveReference(),
        onBackClick = uiState.clickIntents::onCloseClick,
        onIconClick = uiState.clickIntents::onQrCodeScanClick,
        backIconRes = backIcon,
        iconRes = iconRes,
        backgroundColor = TangemTheme.colors.background.tertiary,
        modifier = Modifier.height(TangemTheme.dimens.size56),
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SendScreenContent(uiState: SendUiState, currentState: SendUiCurrentScreen, modifier: Modifier = Modifier) {
    var currentStateProxy by remember { mutableStateOf(currentState) }
    var isTransitionAnimationRunning by remember { mutableStateOf(false) }

    // Prevent quick screen changes to avoid some of the transition animation distortions
    LaunchedEffect(currentState) {
        snapshotFlow { isTransitionAnimationRunning }
            .withIndex()
            .map { (index, running) ->
                if (running && index != 0) {
                    delay(timeMillis = 200)
                }
                running
            }
            .first { !it }

        currentStateProxy = currentState
    }
    // Restrict pressing the back button while screen transition is running to avoid most of the animation distortions
    BackHandler(enabled = isTransitionAnimationRunning) {}

    // Box is needed to fix animation with resizing of AnimatedContent
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStateProxy,
            contentAlignment = Alignment.TopCenter,
            label = "Send Scree Navigation",
            transitionSpec = {
                val direction = if (initialState.type.ordinal < targetState.type.ordinal) {
                    AnimatedContentTransitionScope.SlideDirection.Start
                } else {
                    AnimatedContentTransitionScope.SlideDirection.End
                }

                slideIntoContainer(towards = direction, animationSpec = tween())
                    .togetherWith(slideOutOfContainer(towards = direction, animationSpec = tween()))
            },
        ) { state ->
            isTransitionAnimationRunning = transition.targetState != transition.currentState

            when (state.type) {
                SendUiStateType.Amount -> AmountScreenContent(
                    amountState = uiState.amountState,
                    isBalanceHidden = uiState.isBalanceHidden,
                    clickIntents = uiState.clickIntents,
                    modifier = Modifier.background(TangemTheme.colors.background.tertiary),
                )
                SendUiStateType.EditAmount -> AmountScreenContent(
                    amountState = uiState.editAmountState,
                    isBalanceHidden = uiState.isBalanceHidden,
                    clickIntents = uiState.clickIntents,
                    modifier = Modifier.background(TangemTheme.colors.background.tertiary),
                )
                SendUiStateType.Recipient -> SendRecipientContent(
                    uiState = uiState.recipientState,
                    clickIntents = uiState.clickIntents,
                    isBalanceHidden = uiState.isBalanceHidden,
                )
                SendUiStateType.EditRecipient -> SendRecipientContent(
                    uiState = uiState.editRecipientState,
                    clickIntents = uiState.clickIntents,
                    isBalanceHidden = uiState.isBalanceHidden,
                )
                SendUiStateType.EditFee -> SendSpeedAndFeeContent(
                    state = uiState.editFeeState,
                    clickIntents = uiState.clickIntents,
                )
                SendUiStateType.Send -> SendContent(uiState)
                else -> Unit
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 736)
@Preview(showBackground = true, widthDp = 360, heightDp = 736, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendScreen_Preview(@PreviewParameter(SendScreenPreviewProvider::class) data: SendScreenPreview) {
    TangemThemePreview {
        SendScreen(
            uiState = data.uiState,
            currentState = data.currentState,
        )
    }
}

private class SendScreenPreviewProvider : PreviewParameterProvider<SendScreenPreview> {
    override val values: Sequence<SendScreenPreview>
        get() = sequenceOf(
            SendScreenPreview(
                uiState = SendStatesPreviewData.uiState,
                currentState = SendUiCurrentScreen(type = SendUiStateType.Recipient, isFromConfirmation = false),
            ),
            SendScreenPreview(
                uiState = SendStatesPreviewData.uiState,
                currentState = SendUiCurrentScreen(type = SendUiStateType.Amount, isFromConfirmation = false),
            ),
            SendScreenPreview(
                uiState = SendStatesPreviewData.uiState,
                currentState = SendUiCurrentScreen(type = SendUiStateType.Send, isFromConfirmation = false),
            ),
            SendScreenPreview(
                uiState = SendStatesPreviewData.uiState,
                currentState = SendUiCurrentScreen(type = SendUiStateType.EditFee, isFromConfirmation = true),
            ),
            SendScreenPreview(
                uiState = SendStatesPreviewData.uiState.copy(sendState = ConfirmStatePreviewData.sendDoneState),
                currentState = SendUiCurrentScreen(type = SendUiStateType.Send, isFromConfirmation = false),
            ),
        )
}

private data class SendScreenPreview(
    val uiState: SendUiState,
    val currentState: SendUiCurrentScreen,
)
// endregion