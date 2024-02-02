package com.tangem.features.send.impl.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.ui.amount.SendAmountContent
import com.tangem.features.send.impl.presentation.ui.fee.SendSpeedAndFeeContent
import com.tangem.features.send.impl.presentation.ui.recipient.SendRecipientContent
import com.tangem.features.send.impl.presentation.ui.send.SendContent

@Composable
internal fun SendScreen(uiState: SendUiState) {
    val currentState = uiState.currentState.collectAsStateWithLifecycle()
    val isSuccess = uiState.sendState.isSuccess
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
        val titleRes = when (currentState.value) {
            SendUiStateType.Amount -> R.string.send_amount_label
            SendUiStateType.Recipient -> R.string.send_recipient_label
            SendUiStateType.Fee -> R.string.common_fee_selector_title
            SendUiStateType.Send -> if (!isSuccess) R.string.send_confirm_label else null
        }
        val iconRes = if (currentState.value == SendUiStateType.Recipient) {
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
            currentState = currentState,
            modifier = Modifier
                .weight(1f),
        )
        SendNavigationButtons(uiState)
    }

    SendEventEffect(
        event = uiState.event,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun SendScreenContent(
    uiState: SendUiState,
    currentState: State<SendUiStateType>,
    modifier: Modifier = Modifier,
) {
    val recipientList = uiState.recipientList.collectAsLazyPagingItems()
    AnimatedContent(
        targetState = currentState.value,
        label = "Send Scree Navigation",
        modifier = modifier,
    ) { state ->
        when (state) {
            SendUiStateType.Amount -> SendAmountContent(
                uiState.amountState,
                uiState.clickIntents,
            )
            SendUiStateType.Recipient -> SendRecipientContent(
                uiState.recipientState,
                uiState.clickIntents,
                recipientList,
            )
            SendUiStateType.Fee -> SendSpeedAndFeeContent(
                uiState.feeState,
                uiState.clickIntents,
            )
            SendUiStateType.Send -> SendContent(uiState)
        }
    }
}