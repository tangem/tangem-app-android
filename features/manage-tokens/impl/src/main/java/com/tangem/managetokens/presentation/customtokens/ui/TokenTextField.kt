package com.tangem.managetokens.presentation.customtokens.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.managetokens.presentation.customtokens.state.TextFieldState

@Composable
internal fun TokenTextField(
    state: TextFieldState.Editable,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val isInitiallyComposed = remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        isInitiallyComposed.value = true
    }

    BasicTextField(
        value = state.value,
        onValueChange = state.onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Default),
        singleLine = true,
        maxLines = 1,
        textStyle = TangemTheme.typography.subtitle1.copy(
            fontWeight = FontWeight.Normal,
            color = if (state.isEnabled) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.disabled,
        ),
        cursorBrush = SolidColor(TangemTheme.colors.icon.primary1),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (!it.isFocused && isInitiallyComposed.value) {
                    state.onFocusExit()
                }
            },
        decorationBox = { innerTextField ->
            Row(modifier = Modifier.fillMaxWidth()) {
                if (state.value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = if (state.isEnabled) {
                            TangemTheme.colors.text.tertiary
                        } else {
                            TangemTheme.colors.text.disabled
                        },
                        style = TangemTheme.typography.body2,
                    )
                }
            }
            innerTextField()
        },
        enabled = state.isEnabled,
    )
}