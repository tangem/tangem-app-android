package com.tangem.feature.swap.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.res.TangemTheme

@Suppress("MagicNumber", "LongMethod")
@Composable
internal fun AutoSizeTextField(
    textFieldValue: TextFieldValue,
    focusRequester: FocusRequester,
    onAmountChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        var shrunkFontSize = TangemTheme.typography.h2.fontSize
        val calculateIntrinsics = @Composable {
            ParagraphIntrinsics(
                text = textFieldValue.text,
                style = TangemTheme.typography.h2.copy(
                    color = TangemTheme.colors.text.primary1,
                    fontSize = shrunkFontSize,
                ),
                density = LocalDensity.current,
                fontFamilyResolver = createFontFamilyResolver(LocalContext.current),
            )
        }

        var intrinsics = calculateIntrinsics()
        with(LocalDensity.current) {
            while (intrinsics.maxIntrinsicWidth > maxWidth.toPx()) {
                shrunkFontSize *= 0.9f
                intrinsics = calculateIntrinsics()
            }
        }
        val customTextSelectionColors = TextSelectionColors(
            handleColor = Color.Transparent,
            backgroundColor = TangemTheme.colors.text.secondary.copy(alpha = 0.4f),
        )
        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    onAmountChange.invoke(it.text)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChange(it.hasFocus) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isBlank()) {
                        Text(
                            text = "0",
                            color = TangemTheme.colors.text.disabled,
                            style = TangemTheme.typography.h2,
                        )
                    }
                    innerTextField()
                },
                textStyle = TangemTheme.typography.h2.copy(
                    color = TangemTheme.colors.text.primary1,
                    fontSize = shrunkFontSize,
                ),
                cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
            )
        }
    }
}