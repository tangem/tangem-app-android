package com.tangem.core.ui.components.fields

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Simple text field with placeholder
 */
@Suppress("ReusedModifierInstance")
@Composable
fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: TextReference? = null,
    singleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    color: Color = TangemTheme.colors.text.primary1,
    textStyle: TextStyle = TangemTheme.typography.body2.copy(color = color),
    readOnly: Boolean = false,
    isValuePasted: Boolean = false,
    onValuePastedTriggerDismiss: () -> Unit = {},
    decorationBox: (@Composable (innerTextField: @Composable () -> Unit) -> Unit)? = null,
) {
    val proxyValue by remember(value) { derivedStateOf { value } }
    var textFieldValueState by remember {
        mutableStateOf(TextFieldValue(text = value))
    }
    val focusRequester = remember { FocusRequester.Default }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = TangemTheme.colors.text.accent,
        backgroundColor = TangemTheme.colors.text.accent.copy(alpha = 0.3f),
    )

    var lastTextValue by remember(proxyValue, isValuePasted) {
        textFieldValueState = textFieldValueState.copy(
            text = proxyValue,
            selection = if (isValuePasted) {
                TextRange(proxyValue.length, proxyValue.length)
            } else {
                textFieldValueState.selection
            },
        )
        mutableStateOf(proxyValue)
    }

    // resets paste value cursor trigger
    LaunchedEffect(key1 = isValuePasted) {
        if (isValuePasted) {
            onValuePastedTriggerDismiss()
        }
    }

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = textFieldValueState,
            onValueChange = { newTextFieldValueState ->
                textFieldValueState = newTextFieldValueState

                val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
                lastTextValue = newTextFieldValueState.text

                if (stringChangedSinceLastInvocation) onValueChange(newTextFieldValueState.text)
            },
            textStyle = textStyle.copy(color = color),
            cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
            singleLine = singleLine,
            readOnly = readOnly,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = decorationBox ?: { textValue ->
                SimpleTextPlaceholder(
                    placeholder = placeholder,
                    value = value,
                    textStyle = textStyle,
                    textValue = textValue,
                )
            },
            modifier = modifier
                .focusRequester(focusRequester),
        )
    }
}

// private fun TextRange.getValueRange(oldValue: String, newValue: String) = when {
//     newValue.isEmpty() -> TextRange.Zero
//     newValue.length - oldValue.length > 1 -> TextRange(newValue.length, newValue.length)
//     newValue.length - oldValue.length > 0 -> TextRange(this.start.inc(), this.end.inc())
//     else -> this
// }

@Composable
private fun SimpleTextPlaceholder(
    placeholder: TextReference?,
    value: String,
    textStyle: TextStyle,
    textValue: @Composable () -> Unit,
) {
    Box {
        if (value.isBlank() && placeholder != null) {
            AnimatedContent(
                targetState = placeholder,
                label = "Placeholder Change Animation",
            ) {
                Text(
                    text = it.resolveReference(),
                    style = textStyle,
                    color = TangemTheme.colors.text.disabled,
                )
            }
        }
        textValue()
    }
}
