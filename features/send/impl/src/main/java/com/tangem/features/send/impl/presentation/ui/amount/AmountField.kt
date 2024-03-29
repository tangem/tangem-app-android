package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.defaultFormat
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.send.impl.presentation.state.fields.SendTextField

@Composable
internal fun AmountField(sendField: SendTextField.AmountField, isFiat: Boolean, isEnabled: Boolean) {
    val decimalFormat = rememberDecimalFormat()
    val (primaryValue, secondaryValue) = if (isFiat) {
        sendField.fiatValue to sendField.value
    } else {
        sendField.value to sendField.fiatValue
    }

    val (primaryAmount, secondaryAmount) = if (isFiat) {
        sendField.fiatAmount to sendField.cryptoAmount
    } else {
        sendField.cryptoAmount to sendField.fiatAmount
    }

    AmountTextField(
        value = primaryValue,
        decimals = primaryAmount.decimals,
        symbol = primaryAmount.currencySymbol,
        onValueChange = sendField.onValueChange,
        keyboardOptions = sendField.keyboardOptions,
        keyboardActions = sendField.keyboardActions,
        textStyle = TangemTheme.typography.h2.copy(
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        ),
        isEnabled = isEnabled,
        placeholderAlignment = TopCenter,
        modifier = Modifier
            .padding(
                top = TangemTheme.dimens.spacing24,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            ),
    )

    Box(
        modifier = Modifier
            .padding(
                top = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            ),
    ) {
        val text = if (sendField.isFiatUnavailable) {
            BigDecimalFormatter.EMPTY_BALANCE_SIGN
        } else {
            "${secondaryValue.ifEmpty { decimalFormat.defaultFormat() }} ${secondaryAmount.currencySymbol}"
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