package com.tangem.core.ui.components.fields

import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.*
import java.text.DecimalFormat

/**
 * Simple text field for amount input.
 * Validates and trims input text using [DecimalFormat]. Formats visual output using [AmountVisualTransformation].
 * Can display aligned placeholder and currency symbol [symbol].
 *
 * @param value initial text
 * @param decimals number of decimal places
 * @param onValueChange callback
 * @param textStyle text and placeholder styles
 * @param modifier modifier
 * @param symbol currency symbol
 * @param color text color
 * @param placeholderAlignment alignment of placeholder
 * @param showPlaceholder show placeholder
 * @param keyboardOptions keyboard options
 *
 * @see [SimpleTextField] for standard text field
 */
@Composable
fun AmountTextField(
    value: String,
    decimals: Int,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    symbol: String? = null,
    color: Color = TangemTheme.colors.text.primary1,
    placeholderAlignment: Alignment = TopStart,
    showPlaceholder: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isAutoResize: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
    reduceFactor: Double = 0.9,
) {
    val decimalFormat = rememberDecimalFormat()
    val visualTransformation = remember { AmountVisualTransformation(decimals, symbol, decimalFormat) }
    val placeholderTextAlign = if (placeholderAlignment == TopCenter) {
        TextAlign.Center
    } else {
        TextAlign.Start
    }
    BoxWithConstraints(modifier = modifier) {
        var fontSize = textStyle.fontSize
        if (isAutoResize) {
            val calculateIntrinsics = @Composable {
                val transformedText = visualTransformation.filter(AnnotatedString(value)).text.text
                ParagraphIntrinsics(
                    text = transformedText,
                    style = textStyle.copy(fontSize = fontSize),
                    density = LocalDensity.current,
                    fontFamilyResolver = createFontFamilyResolver(LocalContext.current),
                )
            }
            var intrinsics = calculateIntrinsics()
            with(LocalDensity.current) {
                while (intrinsics.maxIntrinsicWidth > maxWidth.toPx()) {
                    fontSize *= reduceFactor
                    intrinsics = calculateIntrinsics()
                }
            }
        }
        SimpleTextField(
            value = value,
            onValueChange = { newText ->
                if (decimalFormat.isValidSymbols(newText)) {
                    val trimmed = decimalFormat.getValidatedNumberWithFixedDecimals(newText, decimals)
                    onValueChange(trimmed)
                }
            },
            textStyle = textStyle.copy(
                fontSize = fontSize,
                textDirection = TextDirection.ContentOrLtr,
                textAlign = TextAlign.Center,
            ),
            color = color,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            visualTransformation = AmountVisualTransformation(decimals, symbol, decimalFormat),
            modifier = Modifier.background(TangemTheme.colors.background.action),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isBlank() && showPlaceholder) {
                        val placeholder = if (symbol != null) {
                            decimalFormat.defaultFormat().plus(" $symbol")
                        } else {
                            decimalFormat.defaultFormat()
                        }
                        Text(
                            text = placeholder,
                            style = textStyle.copy(textDirection = TextDirection.ContentOrLtr),
                            color = TangemTheme.colors.text.disabled,
                            textAlign = placeholderTextAlign,
                            modifier = Modifier.align(placeholderAlignment),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

private fun DecimalFormat.isValidSymbols(text: String): Boolean {
    return checkDecimalSeparatorDuplicate(text) && checkGroupingSeparator(text)
}

// region preview
@Preview(locale = "en", showBackground = true, name = "English")
@Preview(locale = "ru", showBackground = true, name = "Russian")
@Composable
private fun AmountTextFieldPreview(
    @PreviewParameter(AmountTextFieldPreviewProvider::class) amount: AmountTextFieldPreviewData,
) {
    var text by remember { mutableStateOf(amount.value.orEmpty()) }
    TangemTheme {
        AmountTextField(
            value = text,
            decimals = amount.decimals,
            symbol = amount.symbol,
            placeholderAlignment = amount.placeholderAlignment,
            showPlaceholder = amount.showPlaceholder,
            onValueChange = { text = it },
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
            symbol = "USD",
            value = "1000000,123123",
            decimals = 3,
            placeholderAlignment = TopStart,
            showPlaceholder = true,
        ),
        AmountTextFieldPreviewData(
            symbol = null,
            value = "1000000.123123",
            decimals = 6,
            placeholderAlignment = TopStart,
            showPlaceholder = false,
        ),
        AmountTextFieldPreviewData(
            symbol = "$",
            value = null,
            decimals = 2,
            showPlaceholder = true,
            placeholderAlignment = TopCenter,
        ),
        AmountTextFieldPreviewData(
            symbol = null,
            value = null,
            decimals = 2,
            showPlaceholder = true,
            placeholderAlignment = TopStart,
        ),
    )
}

private data class AmountTextFieldPreviewData(
    val symbol: String? = "$",
    val value: String? = null,
    val decimals: Int = 2,
    val showPlaceholder: Boolean,
    val placeholderAlignment: Alignment,
)
// endregion
