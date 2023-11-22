package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.fields.AmountVisualTransformation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.fields.SendTextField

@Composable
internal fun ColumnScope.AmountField(
    sendField: SendTextField.Amount,
    cryptoSymbol: String,
    fiatSymbol: String,
    isFiat: Boolean,
) {
    val value = if (isFiat) sendField.fiatValue else sendField.value
    val secondaryValue = if (!isFiat) sendField.fiatValue else sendField.value
    val symbol = if (isFiat) fiatSymbol else cryptoSymbol
    val secondarySymbol = if (!isFiat) fiatSymbol else cryptoSymbol

    AmountFieldInner(
        value = value,
        placeholder = sendField.placeholder,
        symbol = symbol,
        onValueChange = sendField.onValueChange,
        keyboardOptions = sendField.keyboardOptions,
        modifier = Modifier
            .align(CenterHorizontally)
            .padding(
                top = TangemTheme.dimens.spacing24,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            ),
    )

    Box(
        modifier = Modifier
            .align(CenterHorizontally)
            .padding(
                top = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            ),
    ) {
        Text(
            text = "$secondaryValue $secondarySymbol",
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
private fun AmountFieldInner(
    value: String,
    placeholder: TextReference,
    symbol: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .background(TangemTheme.colors.background.action),
        textStyle = TangemTheme.typography.h2.copy(
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        visualTransformation = AmountVisualTransformation(symbol),
        decorationBox = { innerTextField ->
            Box {
                if (value.isBlank()) {
                    Text(
                        text = "${placeholder.resolveReference()} $symbol",
                        style = TangemTheme.typography.h2,
                        color = TangemTheme.colors.text.disabled,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.TopCenter),
                    )
                }
                innerTextField()
            }
        },
    )
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