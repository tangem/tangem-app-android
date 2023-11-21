package com.tangem.core.ui.components.fields

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Simple text field with placeholder
 */
@Composable
fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: TextReference? = null,
    singleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    color: Color = TangemTheme.colors.text.primary1,
    readOnly: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TangemTheme.typography.body2.copy(color = color),
        cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
        singleLine = singleLine,
        readOnly = readOnly,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        decorationBox = { textValue ->
            Box {
                if (value.isBlank() && placeholder != null) {
                    Text(
                        text = placeholder.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.disabled,
                        modifier = Modifier,
                    )
                }
                textValue()
            }
        },
        modifier = modifier
            .focusRequester(focusRequester),
    )
}