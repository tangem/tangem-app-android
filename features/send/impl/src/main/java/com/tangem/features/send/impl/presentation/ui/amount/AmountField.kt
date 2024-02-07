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
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.defaultFormat
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.send.impl.presentation.state.fields.SendTextField

@Composable
internal fun AmountField(sendField: SendTextField.AmountField, isFiat: Boolean) {
    val decimalFormat = rememberDecimalFormat()
    val (primaryValue, secondaryValue) = if (isFiat) {
        sendField.fiatValue to sendField.value
    } else {
        sendField.value to sendField.fiatValue
    }

    val (primaryAmount, secondaryAmount) = if (!isFiat) {
        sendField.cryptoAmount to sendField.fiatAmount
    } else {
        sendField.fiatAmount to sendField.cryptoAmount
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
        val text = "${secondaryValue.ifEmpty { decimalFormat.defaultFormat() }}  ${secondaryAmount.currencySymbol}"
        Text(
            text = text,
            style = TangemTheme.typography.caption2,
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
