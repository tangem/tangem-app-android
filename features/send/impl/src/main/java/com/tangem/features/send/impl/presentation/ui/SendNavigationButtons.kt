package com.tangem.features.send.impl.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.ui.SendDoneButtons
import com.tangem.common.ui.amountScreen.utils.getCryptoReference
import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.core.ui.R
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.send.impl.presentation.state.SendUiCurrentScreen
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType

@Composable
internal fun SendNavigationButtons(
    uiState: SendUiState,
    currentState: SendUiCurrentScreen,
    modifier: Modifier = Modifier,
) {
    val sendState = uiState.sendState ?: return
    val isSuccess = sendState.isSuccess
    val isSendingState = currentState.type == SendUiStateType.Send && !isSuccess
    val isSentState = currentState.type == SendUiStateType.Send && isSuccess

    Column(
        modifier = modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        SendingText(
            uiState = uiState,
            isEditState = currentState.isFromConfirmation,
            isVisible = isSendingState,
        )
        SendDoneButtons(
            txUrl = sendState.txUrl,
            onExploreClick = uiState.clickIntents::onExploreClick,
            onShareClick = uiState.clickIntents::onShareClick,
            isVisible = isSentState,
        )
        SendNavigationButton(
            uiState = uiState,
            currentState = currentState,
            modifier = Modifier,
        )
    }
}

@Composable
private fun SendNavigationButton(
    uiState: SendUiState,
    currentState: SendUiCurrentScreen,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val sendState = uiState.sendState ?: return
    val isEditingDisabled = uiState.isEditingDisabled
    val isSuccess = sendState.isSuccess
    val isSending = sendState.isSending

    val isFromConfirmation = currentState.isFromConfirmation
    val isCorrectScreen = currentState.type == SendUiStateType.Amount || currentState.type == SendUiStateType.Fee
    val isSendingState = currentState.type == SendUiStateType.Send && !isSuccess && !isSending

    val (buttonTextId, buttonClick) = getButtonData(
        currentState = currentState,
        isSuccess = isSuccess,
        isSending = isSending,
        uiState = uiState,
    )
    val isButtonEnabled = isButtonEnabled(currentState, uiState)
    val buttonIcon = if (isSendingState) {
        TangemButtonIconPosition.End(R.drawable.ic_tangem_24)
    } else {
        TangemButtonIconPosition.None
    }

    Row(modifier = modifier) {
        AnimatedVisibility(
            visible = !isEditingDisabled && isCorrectScreen && !isFromConfirmation,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            Row {
                Icon(
                    painter = painterResource(R.drawable.ic_back_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                        .background(TangemTheme.colors.button.secondary)
                        .clickable { uiState.clickIntents.onPrevClick() }
                        .padding(TangemTheme.dimens.spacing12),
                )
                SpacerW12()
            }
        }
        TangemButton(
            text = stringResource(buttonTextId),
            icon = buttonIcon,
            enabled = isButtonEnabled,
            onClick = {
                if (isSendingState) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                buttonClick()
            },
            showProgress = false,
            modifier = Modifier.fillMaxWidth(),
            colors = TangemButtonsDefaults.primaryButtonColors,
        )
    }
}

@Composable
private fun SendingText(
    uiState: SendUiState,
    isEditState: Boolean,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    var isVisibleProxy by remember { mutableStateOf(isVisible) }
    val keyboard by keyboardAsState()

    // the text should appear when the keyboard is closed
    LaunchedEffect(isVisible, keyboard) {
        if (isVisible && keyboard is Keyboard.Opened) {
            return@LaunchedEffect
        }
        isVisibleProxy = isVisible
    }

    AnimatedVisibility(
        visible = isVisibleProxy,
        modifier = modifier,
        enter = slideInVertically() + fadeIn(),
        exit = fadeOut(tween(durationMillis = 300)),
        label = "Animate show sending state text",
    ) {
        val amountState = uiState.getAmountState(isEditState) as? AmountState.Data
        val feeState = uiState.getFeeState(isEditState)
        val fiatRate = feeState?.rate
        val fiatAmount = amountState?.amountTextField?.fiatAmount
        val feeFiat = fiatRate?.let { feeState.fee?.amount?.value?.multiply(it) }
        val sendingFiat = if (uiState.isSubtracted) {
            fiatAmount?.value
        } else {
            if (feeState?.isFeeConvertibleToFiat == true) {
                feeFiat?.let { fiatAmount?.value?.plus(it) }
            } else {
                fiatAmount?.value
            }
        }

        if (feeFiat != null && sendingFiat != null) {
            val sendingValue = BigDecimalFormatter.formatFiatAmount(
                fiatAmount = sendingFiat,
                fiatCurrencySymbol = feeState.appCurrency.symbol,
                fiatCurrencyCode = feeState.appCurrency.code,
            )
            val feeValue = if (feeState.isFeeConvertibleToFiat) {
                getFiatString(
                    value = feeState.fee?.amount?.value,
                    rate = feeState.rate,
                    appCurrency = feeState.appCurrency,
                )
            } else {
                getCryptoReference(feeState.fee?.amount, feeState.isFeeApproximate)?.resolveReference().orEmpty()
            }

            val textResource = remember(uiState) {
                resourceReference(
                    id = if (feeState.isFeeConvertibleToFiat) {
                        R.string.send_summary_transaction_description
                    } else {
                        R.string.send_summary_transaction_description_no_fiat_fee
                    },
                    formatArgs = wrappedList(sendingValue, feeValue),
                )
            }
            Text(
                text = textResource.resolveAnnotatedReference(),
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TangemTheme.dimens.spacing12),
            )
        }
    }
}

private fun getButtonData(
    uiState: SendUiState,
    currentState: SendUiCurrentScreen,
    isSuccess: Boolean,
    isSending: Boolean,
): Pair<Int, () -> Unit> {
    return when (currentState.type) {
        SendUiStateType.None,
        SendUiStateType.Amount,
        SendUiStateType.Recipient,
        SendUiStateType.Fee,
        -> R.string.common_next to { uiState.clickIntents.onNextClick() }
        SendUiStateType.EditFee,
        SendUiStateType.EditAmount,
        SendUiStateType.EditRecipient,
        -> R.string.common_continue to { uiState.clickIntents.onNextClick(isFromEdit = true) }
        SendUiStateType.Send -> when {
            isSuccess -> R.string.common_close
            isSending -> R.string.send_sending
            else -> R.string.common_send
        } to uiState.clickIntents::onSendClick
    }
}

private fun isButtonEnabled(currentState: SendUiCurrentScreen, uiState: SendUiState): Boolean {
    return when (currentState.type) {
        SendUiStateType.Amount -> uiState.amountState?.isPrimaryButtonEnabled
        SendUiStateType.Recipient -> uiState.recipientState?.isPrimaryButtonEnabled
        SendUiStateType.Fee -> uiState.feeState?.isPrimaryButtonEnabled
        SendUiStateType.Send -> uiState.sendState?.isPrimaryButtonEnabled
        SendUiStateType.EditAmount -> uiState.editAmountState?.isPrimaryButtonEnabled
        SendUiStateType.EditRecipient -> uiState.editRecipientState?.isPrimaryButtonEnabled
        SendUiStateType.EditFee -> uiState.editFeeState?.isPrimaryButtonEnabled
        else -> true
    } ?: false
}