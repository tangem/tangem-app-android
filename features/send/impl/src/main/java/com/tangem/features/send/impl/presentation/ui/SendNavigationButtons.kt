package com.tangem.features.send.impl.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.shareText
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
        SendingText(uiState = uiState, isVisible = isSendingState)
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
    val showProgress = uiState.amountState?.isFeeLoading == true

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
    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = !isEditingDisabled && isCorrectScreen && !isFromConfirmation,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
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
        }
        TangemButton(
            text = stringResource(buttonTextId),
            icon = buttonIcon,
            enabled = isButtonEnabled,
            onClick = {
                if (isSendingState) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                buttonClick()
            },
            showProgress = showProgress,
            modifier = Modifier.fillMaxWidth(),
            colors = TangemButtonsDefaults.primaryButtonColors,
        )
    }
}

@Composable
private fun SendingText(uiState: SendUiState, isVisible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = slideInVertically().plus(fadeIn()),
        exit = slideOutVertically().plus(fadeOut()),
        label = "Animate show sending state text",
    ) {
        val amountState = uiState.amountState
        val feeState = uiState.feeState
        val fiatRate = feeState?.rate
        val fiatAmount = amountState?.amountTextField?.fiatAmount
        val feeFiat = fiatRate?.let { feeState.fee?.amount?.value?.multiply(it) }
        val sendingFiat = feeFiat?.let { fiatAmount?.value?.plus(it) }

        if (feeFiat != null && sendingFiat != null) {
            val sendingValue = BigDecimalFormatter.formatFiatAmount(
                fiatAmount = sendingFiat,
                fiatCurrencyCode = feeState.appCurrency.code,
                fiatCurrencySymbol = feeState.appCurrency.symbol,
            )
            val feeValue = BigDecimalFormatter.formatFiatAmount(
                fiatAmount = feeFiat,
                fiatCurrencyCode = feeState.appCurrency.code,
                fiatCurrencySymbol = feeState.appCurrency.symbol,
            )
            Text(
                text = stringResource(id = R.string.send_summary_transaction_description, sendingValue, feeValue),
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun SendDoneButtons(
    txUrl: String,
    onExploreClick: () -> Unit,
    onShareClick: () -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isVisible && txUrl.isNotBlank(),
        modifier = modifier,
        enter = slideInVertically().plus(fadeIn()),
        exit = slideOutVertically().plus(fadeOut()),
        label = "Animate show sent state buttons",
    ) {
        Row(modifier = Modifier.padding(TangemTheme.dimens.spacing12)) {
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
        -> if (currentState.isFromConfirmation) {
            R.string.common_continue to uiState.clickIntents::onNextClick
        } else {
            R.string.common_next to uiState.clickIntents::onNextClick
        }
        SendUiStateType.Send -> when {
            isSuccess -> R.string.common_close
            isSending -> R.string.send_sending
            else -> R.string.common_send
        } to uiState.clickIntents::onSendClick
    }
}

private fun isButtonEnabled(currentState: SendUiCurrentScreen, uiState: SendUiState): Boolean {
    return when (currentState.type) {
        SendUiStateType.Amount -> uiState.amountState?.isPrimaryButtonEnabled ?: false
        SendUiStateType.Recipient -> uiState.recipientState?.isPrimaryButtonEnabled ?: false
        SendUiStateType.Fee -> uiState.feeState?.isPrimaryButtonEnabled ?: false
        SendUiStateType.Send -> uiState.sendState?.isPrimaryButtonEnabled ?: false
        else -> true
    }
}