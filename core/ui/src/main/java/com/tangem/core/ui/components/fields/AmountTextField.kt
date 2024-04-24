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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
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
 * Validates and trims input text using [DecimalFormat]. Formats visual output using [visualTransformation].
 * Can display aligned placeholder.
 *
 * @param value initial text
 * @param decimals number of decimal places
 * @param onValueChange callback
 * @param textStyle text and placeholder styles
 * @param modifier modifier
 * @param color text color
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
    color: Color = TangemTheme.colors.text.primary1,
    visualTransformation: VisualTransformation = AmountVisualTransformation(decimals),
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
        val textColor = if (value.isBlank()) TangemTheme.colors.text.disabled else color
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
            ),
            isValuePasted = isValuePasted,
            onValuePastedTriggerDismiss = onValuePastedTriggerDismiss,
            color = textColor,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            readOnly = !isEnabled,
            visualTransformation = visualTransformation,
            modifier = Modifier.background(TangemTheme.colors.background.action),
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
            value = "1000000,123123",
            decimals = 3,
            placeholderAlignment = TopStart,
            showPlaceholder = true,
        ),
        AmountTextFieldPreviewData(
            value = "1000000.123123",
            decimals = 6,
            placeholderAlignment = TopStart,
            showPlaceholder = false,
        ),
        AmountTextFieldPreviewData(
            value = null,
            decimals = 2,
            showPlaceholder = true,
            placeholderAlignment = TopCenter,
        ),
        AmountTextFieldPreviewData(
            value = null,
            decimals = 2,
            showPlaceholder = true,
            placeholderAlignment = TopStart,
        ),
    )
}

private data class AmountTextFieldPreviewData(
    val value: String? = null,
    val decimals: Int = 2,
    val showPlaceholder: Boolean,
    val placeholderAlignment: Alignment,
)
// endregion
