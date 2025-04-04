package com.tangem.common.ui.amountScreen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.rememberDecimalFormat
import kotlinx.coroutines.delay

@Deprecated("Use AmountField with clicks")
@Composable
internal fun AmountField(amountField: AmountFieldModel, appCurrencyCode: String) {
    val decimalFormat = rememberDecimalFormat()
    val isFiatValue = amountField.isFiatValue
    val currencyCode = if (isFiatValue) appCurrencyCode else null
    val (primaryAmount, primaryValue) = if (isFiatValue) {
        amountField.fiatAmount to amountField.fiatValue
    } else {
        amountField.cryptoAmount to amountField.value
    }
    val requester = remember { FocusRequester() }

    AmountTextField(
        value = primaryValue,
        decimals = primaryAmount.decimals,
        visualTransformation = AmountVisualTransformation(
            decimals = primaryAmount.decimals,
            symbol = primaryAmount.currencySymbol,
            currencyCode = currencyCode,
            decimalFormat = decimalFormat,
        ),
        onValueChange = amountField.onValueChange,
        keyboardOptions = amountField.keyboardOptions,
        keyboardActions = amountField.keyboardActions,
        textStyle = TangemTheme.typography.h2.copy(
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        ),
        isAutoResize = true,
        isValuePasted = amountField.isValuePasted,
        onValuePastedTriggerDismiss = amountField.onValuePastedTriggerDismiss,
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
        delay(timeMillis = 200)
        requester.requestFocus()
    }

    AmountSecondary(amountField, appCurrencyCode)
}

@Composable
internal fun AmountField(
    amountField: AmountFieldModel,
    appCurrencyCode: String,
    onValueChange: (String) -> Unit,
    onValuePastedTriggerDismiss: () -> Unit,
) {
    val decimalFormat = rememberDecimalFormat()
    val isFiatValue = amountField.isFiatValue
    val currencyCode = if (isFiatValue) appCurrencyCode else null
    val (primaryAmount, primaryValue) = if (isFiatValue) {
        amountField.fiatAmount to amountField.fiatValue
    } else {
        amountField.cryptoAmount to amountField.value
    }
    val requester = remember { FocusRequester() }

    AmountTextField(
        value = primaryValue,
        decimals = primaryAmount.decimals,
        visualTransformation = AmountVisualTransformation(
            decimals = primaryAmount.decimals,
            symbol = primaryAmount.currencySymbol,
            currencyCode = currencyCode,
            decimalFormat = decimalFormat,
        ),
        onValueChange = onValueChange,
        keyboardOptions = amountField.keyboardOptions,
        keyboardActions = amountField.keyboardActions,
        textStyle = TangemTheme.typography.h2.copy(
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        ),
        isAutoResize = true,
        isValuePasted = amountField.isValuePasted,
        onValuePastedTriggerDismiss = onValuePastedTriggerDismiss,
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
        delay(timeMillis = 200)
        requester.requestFocus()
    }

    AmountSecondary(amountField, appCurrencyCode)
}

@Composable
private fun AmountSecondary(amountField: AmountFieldModel, appCurrencyCode: String) {
    val secondaryAmount = if (amountField.isFiatValue) amountField.cryptoAmount else amountField.fiatAmount
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(
                top = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            ),
    ) {
        val text = if (amountField.isFiatValue) {
            secondaryAmount.value.format { crypto(secondaryAmount.currencySymbol, secondaryAmount.decimals) }
        } else {
            secondaryAmount.value.format {
                fiat(
                    fiatCurrencySymbol = secondaryAmount.currencySymbol,
                    fiatCurrencyCode = appCurrencyCode,
                )
            }
        }
        Text(
            text = text,
            style = TangemTheme.typography.caption2.copy(textDirection = TextDirection.ContentOrLtr),
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(TopCenter)
                .padding(bottom = TangemTheme.dimens.spacing32),
        )
        AmountFieldError(
            isError = amountField.isError,
            isWarning = amountField.isWarning,
            error = amountField.error,
            modifier = Modifier
                .align(BottomCenter)
                .padding(
                    top = TangemTheme.dimens.spacing20,
                    bottom = TangemTheme.dimens.spacing12,
                ),
        )
    }
}

@Composable
private fun AmountFieldError(
    isError: Boolean,
    isWarning: Boolean,
    error: TextReference,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isError || isWarning,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val errorText = remember(this, error) { error }
        val color = if (isError) TangemTheme.colors.text.warning else TangemTheme.colors.text.attention
        Text(
            text = errorText.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}