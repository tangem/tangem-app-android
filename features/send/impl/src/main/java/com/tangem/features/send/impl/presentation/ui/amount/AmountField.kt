package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.job

@Composable
internal fun AmountField(sendField: SendTextField.AmountField, isEnabled: Boolean, appCurrencyCode: String) {
    val decimalFormat = rememberDecimalFormat()
    val isFiatValue = sendField.isFiatValue
    val currencyCode = if (isFiatValue) appCurrencyCode else null
    val (primaryAmount, primaryValue) = if (isFiatValue) {
        sendField.fiatAmount to sendField.fiatValue
    } else {
        sendField.cryptoAmount to sendField.value
    }
    val requester = remember { FocusRequester() }
    var isEnabledProxy by remember { mutableStateOf(isEnabled) }

    // Fix animation from amount screen to summary screen ([REDACTED_TASK_KEY])
    LaunchedEffect(key1 = isEnabled) {
        if (isEnabled) {
            delay(timeMillis = 700)
        }
        isEnabledProxy = isEnabled
    }

    AmountTextField(
        value = primaryValue,
        decimals = primaryAmount.decimals,
        visualTransformation = AmountVisualTransformation(
            decimals = primaryAmount.decimals,
            symbol = primaryAmount.currencySymbol,
            currencyCode = currencyCode,
            decimalFormat = decimalFormat,
        ),
        onValueChange = sendField.onValueChange,
        keyboardOptions = sendField.keyboardOptions,
        keyboardActions = sendField.keyboardActions,
        textStyle = TangemTheme.typography.h2.copy(
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        ),
        isEnabled = isEnabledProxy,
        isAutoResize = true,
        modifier = Modifier
            .focusRequester(requester)
            .padding(
                top = TangemTheme.dimens.spacing24,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            )
            .requiredHeightIn(min = TangemTheme.dimens.size32),
    )

    LaunchedEffect(key1 = Unit) {
        this.coroutineContext.job.invokeOnCompletion {
            requester.requestFocus()
        }
    }

    AmountSecondary(sendField, appCurrencyCode)
}

@Composable
private fun AmountSecondary(sendField: SendTextField.AmountField, appCurrencyCode: String) {
    val secondaryAmount = if (sendField.isFiatValue) sendField.cryptoAmount else sendField.fiatAmount
    Box(
        modifier = Modifier
            .padding(
                top = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            ),
    ) {
        val text = if (sendField.isFiatValue) {
            BigDecimalFormatter.formatCryptoAmount(
                cryptoAmount = secondaryAmount.value,
                cryptoCurrency = secondaryAmount.currencySymbol,
                decimals = secondaryAmount.decimals,
            )
        } else {
            BigDecimalFormatter.formatFiatAmount(
                fiatAmount = secondaryAmount.value,
                fiatCurrencySymbol = secondaryAmount.currencySymbol,
                fiatCurrencyCode = appCurrencyCode,
            )
        }
        Text(
            text = text,
            style = TangemTheme.typography.caption2.copy(textDirection = TextDirection.ContentOrLtr),
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(BottomCenter)
                .padding(bottom = TangemTheme.dimens.spacing32),
        )
        AmountFieldError(
            isError = sendField.isError,
            error = sendField.error,
            modifier = Modifier
                .align(BottomCenter)
                .padding(bottom = TangemTheme.dimens.spacing12),
        )
    }
}

@Composable
private fun AmountFieldError(isError: Boolean, error: TextReference, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isError,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Text(
            text = error.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.warning,
            textAlign = TextAlign.Center,
        )
    }
}