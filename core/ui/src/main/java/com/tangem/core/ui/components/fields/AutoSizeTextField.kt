package com.tangem.core.ui.components.fields

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.TextUnit
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Simple text field for auto size input.
 * Can display aligned placeholder.
 *
 * @param value initial text
 * @param onValueChange callback
 * @param isAutoResize is text font auto resize
 * @param reduceFactor font resize factor
 * @param textStyle text and placeholder styles
 * @param textFieldModifier modifier for [SimpleTextField]
 * @param boxModifier modifier for [BoxWithConstraints]
 * @see [SimpleTextField] for other text field params
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AutoSizeTextField(
    value: String,
    onValueChange: (String) -> Unit,

    // region AutoSize
    isAutoResize: Boolean = true,
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
    reduceFactor: Double = 0.9,

    // region TextField
    textFieldModifier: Modifier = Modifier,
    boxModifier: Modifier = Modifier,
    placeholder: TextReference? = null,
    singleLine: Boolean = isAutoResize,
    centered: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    color: Color = TangemTheme.colors.text.primary1,
    textStyle: TextStyle = TangemTheme.typography.body2.copy(color = color),
    placeholderColor: Color = TangemTheme.colors.text.disabled,
    readOnly: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isValuePasted: Boolean = false,
    onValuePastedTriggerDismiss: () -> Unit = {},
    decorationBox: (@Composable (innerTextField: @Composable () -> Unit) -> Unit)? = null,
) {
    BoxWithConstraints(modifier = boxModifier) {
        val fontSize = if (isAutoResize) {
            resizeFont(visualTransformation, value, textStyle, reduceFactor)
        } else {
            textStyle.fontSize
        }
        val textColor = if (value.isBlank()) TangemTheme.colors.text.disabled else color
        SimpleTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(
                fontSize = fontSize,
                textDirection = TextDirection.ContentOrLtr,
            ),
            isValuePasted = isValuePasted,
            onValuePastedTriggerDismiss = onValuePastedTriggerDismiss,
            color = textColor,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            placeholder = placeholder,
            placeholderColor = placeholderColor,
            singleLine = singleLine,
            interactionSource = interactionSource,
            readOnly = readOnly,
            centered = centered,
            visualTransformation = visualTransformation,
            decorationBox = decorationBox,
            modifier = textFieldModifier,
        )
    }
}

@Composable
internal fun BoxWithConstraintsScope.resizeFont(
    visualTransformation: VisualTransformation,
    value: String,
    textStyle: TextStyle,
    reduceFactor: Double,
): TextUnit {
    var result = textStyle.fontSize
    val calculateIntrinsics = @Composable {
        val transformedText = visualTransformation.filter(AnnotatedString(value)).text.text
        ParagraphIntrinsics(
            text = transformedText,
            style = textStyle.copy(fontSize = result),
            density = LocalDensity.current,
            fontFamilyResolver = createFontFamilyResolver(LocalContext.current),
        )
    }
    var intrinsics = calculateIntrinsics()
    with(LocalDensity.current) {
        while (intrinsics.maxIntrinsicWidth > maxWidth.toPx()) {
            result *= reduceFactor
            intrinsics = calculateIntrinsics()
        }
    }
    return result
}

// region preview
@Preview(widthDp = 360, showBackground = true)
@Composable
private fun AmountTextFieldPreview(
    @PreviewParameter(AutoSizeTextFieldPreviewProvider::class) data: AutoSizeTextFieldPreviewData,
) {
    var text by remember { mutableStateOf(data.value) }
    TangemThemePreview {
        AutoSizeTextField(
            textFieldModifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = { text = it },
            centered = data.centered,
            isAutoResize = data.isAutoResize,
            placeholder = data.placeholder,
        )
    }
}

private class AutoSizeTextFieldPreviewProvider : PreviewParameterProvider<AutoSizeTextFieldPreviewData> {
    override val values = sequenceOf(
        AutoSizeTextFieldPreviewData(
            value = "AutoSizeTextField",
            placeholder = stringReference("placeholder"),
            isAutoResize = true,
            centered = false,
        ),
        AutoSizeTextFieldPreviewData(
            value = "AutoSizeTextFieldAutoSizeTextFieldAutoSizeTextFieldAutoSizeTextField",
            placeholder = stringReference("placeholder"),
            isAutoResize = true,
            centered = false,
        ),
        AutoSizeTextFieldPreviewData(
            value = "AutoSizeTextFieldAutoSizeTextFieldAutoSizeTextFieldAutoSizeTextField",
            placeholder = stringReference("Placeholder"),
            isAutoResize = true,
            centered = false,
        ),
        AutoSizeTextFieldPreviewData(
            value = "",
            placeholder = stringReference("Placeholder"),
            isAutoResize = true,
            centered = false,
        ),
        AutoSizeTextFieldPreviewData(
            value = "AutoSizeTextField",
            placeholder = stringReference("Placeholder"),
            isAutoResize = false,
            centered = true,
        ),
        AutoSizeTextFieldPreviewData(
            value = "AutoSizeTextFieldAutoSizeTextFieldAutoSizeTextFieldAutoSizeTextField",
            placeholder = stringReference("Placeholder"),
            isAutoResize = false,
            centered = true,
        ),
        AutoSizeTextFieldPreviewData(
            value = "",
            placeholder = stringReference("Placeholder"),
            isAutoResize = false,
            centered = true,
        ),
    )
}

private data class AutoSizeTextFieldPreviewData(
    val value: String,
    val placeholder: TextReference,
    val isAutoResize: Boolean,
    val centered: Boolean,
)
// endregion