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
    decorationBox: (@Composable (innerTextField: @Composable () -> Unit) -> Unit)? = null,
) {
    var textFieldValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = when {
                    value.isEmpty() -> TextRange.Zero
                    else -> TextRange(value.length, value.length)
                },
            ),
        )
    }
    val focusRequester = remember { FocusRequester.Default }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = TangemTheme.colors.text.secondary,
        backgroundColor = TangemTheme.colors.text.secondary.copy(alpha = 0.4f),
    )

    val textFieldValue = textFieldValueState.copy(text = value)

    SideEffect {
        if (textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }

    var lastTextValue by remember(value) { mutableStateOf(value) }

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newTextFieldValueState ->
                textFieldValueState = newTextFieldValueState

                val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
                lastTextValue = newTextFieldValueState.text

                if (stringChangedSinceLastInvocation) {
                    onValueChange(newTextFieldValueState.text)
                }
            },
            textStyle = textStyle.copy(color = color),
            cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
            singleLine = singleLine,
            readOnly = readOnly,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = decorationBox ?: { textValue ->
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
            },
            modifier = modifier
                .focusRequester(focusRequester),
        )
    }
}