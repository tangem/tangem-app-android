package com.tangem.core.ui.components.fields

import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SendScreenTestTags
import com.tangem.core.ui.utils.*
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Simple text field for amount input.
 * Validates and trims input text using [DecimalFormat]. Formats visual output using [visualTransformation].
 * Can display aligned placeholder.
 *
 * @param value initial text
 * @param decimals number of decimal places
 * @param onValueChange callback
 * @param textStyle text and placeholder styles
 * @param modifier modifier
 * @param colors text and background colors
 * @param visualTransformation text visual transformation
 * @param keyboardOptions keyboard options
 * @param keyboardActions keyboard actions
 * @param isEnabled is field editing enabled
 * @param isAutoResize is text font auto resize
 * @param reduceFactor font resize factor
 * @see [SimpleTextField] for standard text field
 */
@Composable
fun AmountTextField(
    value: String,
    decimals: Int,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    colors: AmountTextFieldColors = TangemAmountTextFieldColors,
    visualTransformation: VisualTransformation = AmountVisualTransformation(
        decimals = decimals,
        symbolColor = if (value.isBlank()) colors.disabledTextColor else colors.textColor,
    ),
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isEnabled: Boolean = true,
    isAutoResize: Boolean = false,
    isValuePasted: Boolean = false,
    onValuePastedTriggerDismiss: () -> Unit = {},
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
    reduceFactor: Double = 0.9,
) {
    val decimalFormat = rememberDecimalFormat()
    BoxWithConstraints(modifier = modifier) {
        val fontSize = if (isAutoResize) {
            resizeFont(
                visualTransformation = visualTransformation,
                value = value,
                textStyle = textStyle,
                reduceFactor = reduceFactor,
            )
        } else {
            textStyle.fontSize
        }
        val textColor = if (value.isBlank()) colors.disabledTextColor else colors.textColor
        SimpleTextField(
            value = value,
            onValueChange = { newText ->
                onValueChange(
                    prepareEnter(
                        oldValue = value,
                        newValue = newText,
                        decimalFormat = decimalFormat,
                        decimals = decimals,
                    ),
                )
            },
            textStyle = textStyle.copy(
                fontSize = fontSize,
                textDirection = TextDirection.ContentOrLtr,
            ),
            isValuePasted = isValuePasted,
            onValuePastedTriggerDismiss = onValuePastedTriggerDismiss,
            color = textColor,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            readOnly = !isEnabled,
            visualTransformation = visualTransformation,
            modifier = Modifier
                .background(colors.backgroundColor)
                .testTag(SendScreenTestTags.INPUT_TEXT_FIELD),
        )
    }
}

val TangemAmountTextFieldColors: AmountTextFieldColors
    @Composable
    @ReadOnlyComposable
    get() = AmountTextFieldColors(
        textColor = TangemTheme.colors.text.primary1,
        disabledTextColor = TangemTheme.colors.text.disabled,
        backgroundColor = TangemTheme.colors.background.action,
    )

@Immutable
data class AmountTextFieldColors(
    val textColor: Color,
    val disabledTextColor: Color,
    val backgroundColor: Color,
)

private fun prepareEnter(oldValue: String, newValue: String, decimalFormat: DecimalFormat, decimals: Int): String {
    val decimalSymbol = decimalFormat.decimalFormatSymbols.decimalSeparator
    return if (decimalFormat.isValidSymbols(newValue)) {
        val parsedDecimal = newValue.parseBigDecimalOrNull()
        val parsedValue = parsedDecimal?.toPlainString() ?: if (newValue.isBlank()) "" else oldValue

        val replacedWithSymbol = parsedValue.replaceDecimalSymbol(decimalSymbol)
        val joinedSymbol = replacedWithSymbol.preserveDecimalSymbol(newValue, decimalSymbol)
        val withPreservedZeros = joinedSymbol.preserveTrailingZeros(newValue, parsedDecimal, decimalSymbol)
        decimalFormat.getValidatedNumberWithFixedDecimals(withPreservedZeros, decimals)
    } else {
        oldValue
    }
}

private fun String.replaceDecimalSymbol(decimalSymbol: Char) = if (this.findLast { it != decimalSymbol } != null) {
    when {
        this.findLast { it == COMMA_SEPARATOR } != null -> {
            this.replace(COMMA_SEPARATOR, decimalSymbol)
        }
        this.findLast { it == POINT_SEPARATOR } != null -> {
            this.replace(POINT_SEPARATOR, decimalSymbol)
        }
        else -> this
    }
} else {
    this
}

private fun String.preserveDecimalSymbol(newValue: String, decimalSymbol: Char) = if (
    newValue.endsWith(COMMA_SEPARATOR) || newValue.endsWith(POINT_SEPARATOR)
) {
    this.plus(decimalSymbol)
} else {
    this
}

private fun String.preserveTrailingZeros(newValue: String, parsedDecimal: BigDecimal?, decimalSymbol: Char): String {
    val trailingZeros = newValue.split(decimalSymbol).getOrNull(1)?.takeLastWhile { it == '0' }.orEmpty()
    return when {
        this.endsWith('0') && parsedDecimal?.scale() != 0 -> this
        parsedDecimal?.scale() == 0 && trailingZeros.isNotEmpty() -> "$this$decimalSymbol$trailingZeros"
        else -> this.plus(trailingZeros)
    }
}

private fun DecimalFormat.isValidSymbols(text: String): Boolean {
    return checkDecimalSeparatorDuplicate(text)
}

// region preview
@Preview(locale = "en", showBackground = true, name = "English")
@Preview(locale = "ru", showBackground = true, name = "Russian")
@Composable
private fun AmountTextFieldPreview(
    @PreviewParameter(AmountTextFieldPreviewProvider::class) amount: AmountTextFieldPreviewData,
) {
    var text by remember { mutableStateOf(amount.value.orEmpty()) }
    TangemThemePreview {
        AmountTextField(
            value = text,
            decimals = amount.decimals,
            onValueChange = { text = it },
            colors = TangemAmountTextFieldColors,
            textStyle = TangemTheme.typography.h2.copy(
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private class AmountTextFieldPreviewProvider : PreviewParameterProvider<AmountTextFieldPreviewData> {
    override val values = sequenceOf(
        AmountTextFieldPreviewData(
            value = "1000000,123123",
            decimals = 3,
            placeholderAlignment = TopStart,
            shouldShowPlaceholder = true,
        ),
        AmountTextFieldPreviewData(
            value = "1000000.123123",
            decimals = 6,
            placeholderAlignment = TopStart,
            shouldShowPlaceholder = false,
        ),
        AmountTextFieldPreviewData(
            value = null,
            decimals = 2,
            shouldShowPlaceholder = true,
            placeholderAlignment = TopCenter,
        ),
        AmountTextFieldPreviewData(
            value = null,
            decimals = 2,
            shouldShowPlaceholder = true,
            placeholderAlignment = TopStart,
        ),
    )
}

private data class AmountTextFieldPreviewData(
    val value: String? = null,
    val decimals: Int = 2,
    val shouldShowPlaceholder: Boolean,
    val placeholderAlignment: Alignment,
)
// endregion