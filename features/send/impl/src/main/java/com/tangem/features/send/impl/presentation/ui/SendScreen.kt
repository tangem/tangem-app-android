package com.tangem.features.send.impl.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendUiCurrentScreen
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.ui.amount.SendAmountContent
import com.tangem.features.send.impl.presentation.ui.fee.SendSpeedAndFeeContent
import com.tangem.features.send.impl.presentation.ui.recipient.SendRecipientContent
import com.tangem.features.send.impl.presentation.ui.send.SendContent
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun SendScreen(uiState: SendUiState, currentStateFlow: StateFlow<SendUiCurrentScreen>) {
    val currentState = currentStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sendState = uiState.sendState ?: return
    BackHandler { uiState.clickIntents.onBackClick() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .background(color = TangemTheme.colors.background.tertiary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val titleRes = when (currentState.value.type) {
            SendUiStateType.Amount -> resourceReference(R.string.send_amount_label)
            SendUiStateType.Recipient -> resourceReference(R.string.send_recipient_label)
            SendUiStateType.Fee -> resourceReference(R.string.common_fee_selector_title)
            SendUiStateType.Send -> if (!sendState.isSuccess) {
                resourceReference(R.string.send_summary_title, wrappedList(uiState.cryptoCurrencySymbol))
            } else {
                null
            }
            else -> null
        }
        val isSending = currentState.value.type == SendUiStateType.Send && !uiState.sendState.isSuccess
        val subtitleRes = if (isSending) {
            uiState.amountState?.walletName
        } else {
            null
        }
        val iconRes = if (currentState.value.type == SendUiStateType.Recipient) {
            R.drawable.ic_qrcode_scan_24
        } else {
            null
        }

        AppBarWithBackButtonAndIcon(
            text = titleRes?.resolveReference(),
            subtitle = subtitleRes,
            onBackClick = uiState.clickIntents::popBackStack,
            onIconClick = uiState.clickIntents::onQrCodeScanClick,
            backIconRes = R.drawable.ic_close_24,
            iconRes = iconRes,
            backgroundColor = TangemTheme.colors.background.tertiary,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
        Box(modifier = Modifier.weight(1f)) {
            SendScreenContent(
                uiState = uiState,
                currentState = currentState.value,
            )
            SendNavigationButtons(
                uiState = uiState,
                currentState = currentState.value,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    SendEventEffect(
        event = uiState.event,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SendScreenContent(uiState: SendUiState, currentState: SendUiCurrentScreen, modifier: Modifier = Modifier) {
    var lastState by remember { mutableIntStateOf(currentState.type.ordinal) }
    val direction = remember(currentState.type.ordinal) {
        if (lastState < currentState.type.ordinal) {
            AnimatedContentTransitionScope.SlideDirection.Start
        } else {
            AnimatedContentTransitionScope.SlideDirection.End
        }
    }
    AnimatedContent(
        targetState = currentState,
        label = "Send Scree Navigation",
        modifier = modifier,
        transitionSpec = {
            lastState = currentState.type.ordinal
            slideIntoContainer(towards = direction, animationSpec = tween())
                .togetherWith(slideOutOfContainer(towards = direction, animationSpec = tween()))
        },
    ) { state ->
        when (state.type) {
            SendUiStateType.Amount -> SendAmountContent(
                amountState = uiState.amountState,
                isBalanceHiding = uiState.isBalanceHidden,
                clickIntents = uiState.clickIntents,
            )
            SendUiStateType.Recipient -> SendRecipientContent(
                uiState = uiState.recipientState,
                clickIntents = uiState.clickIntents,
            )
            SendUiStateType.Fee -> SendSpeedAndFeeContent(
                state = uiState.feeState,
                clickIntents = uiState.clickIntents,
            )
            SendUiStateType.Send -> SendContent(uiState)
            else -> Unit
        }
    }
}
