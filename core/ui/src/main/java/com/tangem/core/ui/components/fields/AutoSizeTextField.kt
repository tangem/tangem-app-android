package com.tangem.core.ui.components.fields

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme

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