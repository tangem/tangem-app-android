package com.tangem.features.send.impl.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.rememberHapticFeedback
import com.tangem.core.ui.extensions.shareText
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendUiCurrentScreen
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType

@Composable
internal fun SendNavigationButtons(uiState: SendUiState, currentState: SendUiCurrentScreen) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = TangemTheme.dimens.spacing12),
    ) {
        SendSecondaryNavigationButton(
            uiState = uiState,
            currentState = currentState,
        )
        SendPrimaryNavigationButton(
            uiState = uiState,
            currentState = currentState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}

@Composable
private fun SendSecondaryNavigationButton(uiState: SendUiState, currentState: SendUiCurrentScreen) {
    val isEditingDisabled = uiState.isEditingDisabled
    val isFromConfirmation = currentState.isFromConfirmation
    val isCorrectScreen = currentState.type == SendUiStateType.Amount || currentState.type == SendUiStateType.Fee
    AnimatedVisibility(
        visible = !isEditingDisabled && isCorrectScreen && !isFromConfirmation,
        enter = expandHorizontally(expandFrom = Alignment.End),
        exit = shrinkHorizontally(shrinkTowards = Alignment.End),
    ) {
        Icon(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing16)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                .background(TangemTheme.colors.button.secondary)
                .clickable {
                    uiState.clickIntents.onPrevClick()
                }
                .padding(TangemTheme.dimens.spacing12),
            painter = painterResource(R.drawable.ic_back_24),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
    }
}

@Composable
private fun SendPrimaryNavigationButton(
    uiState: SendUiState,
    currentState: SendUiCurrentScreen,
    modifier: Modifier = Modifier,
) {
    val isSuccess = uiState.sendState.isSuccess
    val isSending = uiState.sendState.isSending
    val txUrl = uiState.sendState.txUrl

    val (buttonTextId, buttonClick) = getButtonData(
        currentState = currentState,
        isSuccess = isSuccess,
        uiState = uiState,
    )

    val isButtonEnabled = isButtonEnabled(
        currentState = currentState,
        uiState = uiState,
    )

    AnimatedContent(
        targetState = buttonTextId,
        label = "Update send screen state",
        modifier = modifier,
    ) { textId ->
        when {
            currentState.type == SendUiStateType.Send && !isSuccess -> {
                val hapticFeedback = rememberHapticFeedback(state = currentState, onAction = buttonClick)
                PrimaryButtonIconEnd(
                    text = stringResource(textId),
                    iconResId = R.drawable.ic_tangem_24,
                    enabled = isButtonEnabled,
                    onClick = hapticFeedback,
                    showProgress = isSending,
                )
            }
            currentState.type == SendUiStateType.Send && isSuccess -> {
                PrimaryButtonsDone(
                    textRes = textId,
                    txUrl = txUrl,
                    onExploreClick = uiState.clickIntents::onExploreClick,
                    onShareClick = uiState.clickIntents::onShareClick,
                    onDoneClick = buttonClick,
                    modifier = Modifier,
                )
            }
            else -> {
                PrimaryButton(
                    text = stringResource(textId),
                    enabled = isButtonEnabled,
                    onClick = buttonClick,
                )
            }
        }
    }
}

@Composable
private fun PrimaryButtonsDone(
    @StringRes textRes: Int,
    txUrl: String,
    onExploreClick: () -> Unit,
    onShareClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    Column(modifier = modifier) {
        if (txUrl.isNotBlank()) {
            Row {
                SecondaryButtonIconStart(
                    text = stringResource(id = R.string.common_explore),
                    iconResId = R.drawable.ic_web_24,
                    onClick = onExploreClick,
                    modifier = Modifier.weight(1f),
                )
                SpacerW12()
                SecondaryButtonIconStart(
                    text = stringResource(id = R.string.common_share),
                    iconResId = R.drawable.ic_share_24,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        context.shareText(txUrl)
                        onShareClick()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            SpacerH12()
        }
        PrimaryButton(
            text = stringResource(id = textRes),
            enabled = true,
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun getButtonData(
    uiState: SendUiState,
    currentState: SendUiCurrentScreen,
    isSuccess: Boolean,
): Pair<Int, () -> Unit> {
    return when (currentState.type) {
        SendUiStateType.None,
        SendUiStateType.Amount,
        SendUiStateType.Recipient,
        SendUiStateType.Fee,
        -> if (currentState.isFromConfirmation) {
            R.string.common_continue to uiState.clickIntents::onNextClick
        } else {
            R.string.common_next to uiState.clickIntents::onNextClick
        }
        SendUiStateType.Send -> if (isSuccess) {
            R.string.common_close
        } else {
            R.string.common_send
        } to uiState.clickIntents::onSendClick
    }
}

private fun isButtonEnabled(currentState: SendUiCurrentScreen, uiState: SendUiState): Boolean {
    return when (currentState.type) {
        SendUiStateType.Amount -> uiState.amountState?.isPrimaryButtonEnabled ?: false
        SendUiStateType.Recipient -> uiState.recipientState?.isPrimaryButtonEnabled ?: false
        SendUiStateType.Fee -> uiState.feeState?.isPrimaryButtonEnabled ?: false
        SendUiStateType.Send -> uiState.sendState.isPrimaryButtonEnabled
        else -> true
    }
}