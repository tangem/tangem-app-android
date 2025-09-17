package com.tangem.core.ui.components.fields

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    placeholderColor: Color = TangemTheme.colors.text.disabled,
    readOnly: Boolean = false,
    centered: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isValuePasted: Boolean = false,
    onValuePastedTriggerDismiss: () -> Unit = {},
    decorationBox: (@Composable (innerTextField: @Composable () -> Unit) -> Unit)? = null,
) {
    val proxyValue by remember(value) { derivedStateOf { value } }
    var textFieldValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length, value.length),
            ),
        )
    }
    val focusRequester = remember { FocusRequester.Default }
    val textFieldValue = textFieldValueState.copy(text = value)
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

    val isSelectionChanged by rememberSelectionChanged(textFieldValue, textFieldValueState)
    LaunchedEffect(key1 = isSelectionChanged) {
        if (isSelectionChanged) {
            textFieldValueState = textFieldValue
        }
    }

    // resets paste value cursor trigger
    LaunchedEffect(key1 = isValuePasted) {
        if (isValuePasted) {
            onValuePastedTriggerDismiss()
        }
    }
    var textStyle = textStyle.copy(color = color)
    if (centered) textStyle = textStyle.copy(textAlign = TextAlign.Center)

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newTextFieldValueState ->
            textFieldValueState = newTextFieldValueState

            val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
            lastTextValue = newTextFieldValueState.text

            if (stringChangedSinceLastInvocation) onValueChange(newTextFieldValueState.text)
        },
        textStyle = textStyle,
        cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
        singleLine = singleLine,
        readOnly = readOnly,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        decorationBox = decorationBox ?: { textValue ->
            SimpleTextPlaceholder(
                placeholder = placeholder,
                value = value,
                textStyle = textStyle,
                textValue = textValue,
                centered = centered,
                color = placeholderColor,
            )
        },
        modifier = modifier
            .focusRequester(focusRequester),
    )
}

@Composable
private fun SimpleTextPlaceholder(
    value: String,
    textStyle: TextStyle,
    centered: Boolean,
    placeholder: TextReference?,
    color: Color = TangemTheme.colors.text.disabled,
    textValue: @Composable () -> Unit,
) {
    Box(contentAlignment = if (centered) Alignment.Center else Alignment.TopStart) {
        if (value.isBlank() && placeholder != null) {
            AnimatedContent(
                targetState = placeholder,
                label = "Placeholder Change Animation",
            ) {
                Text(
                    text = it.resolveReference(),
                    style = textStyle,
                    color = color,
                )
            }
        }
        textValue()
    }
}

@Composable
private fun rememberSelectionChanged(textFieldValue: TextFieldValue, textFieldValueState: TextFieldValue) = remember {
    derivedStateOf {
        val isSelectionChanged = textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        val isTextNotChanged = textFieldValue.text == textFieldValueState.text
        isSelectionChanged && isTextNotChanged
    }
}