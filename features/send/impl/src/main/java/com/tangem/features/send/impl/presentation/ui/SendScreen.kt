package com.tangem.features.send.impl.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
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
            SendUiStateType.Amount -> R.string.send_amount_label
            SendUiStateType.Recipient -> R.string.send_recipient_label
            SendUiStateType.Fee -> R.string.common_fee_selector_title
            SendUiStateType.Send -> if (!uiState.sendState.isSuccess) R.string.send_confirm_label else null
            else -> null
        }
        val iconRes = if (currentState.value.type == SendUiStateType.Recipient) {
            R.drawable.ic_qrcode_scan_24
        } else {
            null
        }

        AppBarWithBackButtonAndIcon(
            text = titleRes?.let { stringResource(it) },
            onBackClick = uiState.clickIntents::popBackStack,
            onIconClick = uiState.clickIntents::onQrCodeScanClick,
            backIconRes = R.drawable.ic_close_24,
            iconRes = iconRes,
            backgroundColor = TangemTheme.colors.background.tertiary,
        )
        SendScreenContent(
            uiState = uiState,
            currentState = currentState.value,
            modifier = Modifier
                .weight(1f),
        )
        SendNavigationButtons(uiState, currentState.value)
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
