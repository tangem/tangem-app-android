package com.tangem.core.ui.components.fields

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.utils.StringsSigns.PASSWORD_VISUAL_CHAR
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun PinTextField(
    value: String,
    length: Int,
    isPasswordVisual: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val textFieldValue = remember(value) {
        TextFieldValue(value, selection = TextRange(value.length))
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            if (it.text.length <= length) {
                onValueChange(it.text)
            }
        },
        modifier = modifier
            .clickable {
                focusRequester.requestFocus()
            }
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        singleLine = true,
        textStyle = TangemTheme.typography.body1,
        maxLines = 1,
        cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.width(0.dp)) {
                innerTextField()
            }
            CellDecoration(
                length = length,
                isPasswordVisual = isPasswordVisual,
                value = value,
            )
        },
    )

    LaunchedEffect(Unit) {
        delay(timeMillis = 200)
        focusRequester.requestFocus()
    }
}

@Suppress("MagicNumber")
@Composable
private fun CellDecoration(
    length: Int,
    value: String,
    modifier: Modifier = Modifier,
    isPasswordVisual: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val width = textMeasurer.measure("0")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(length) {
            val char = if (it < value.length) {
                if (isPasswordVisual) PASSWORD_VISUAL_CHAR.toString() else value[it].toString()
            } else {
                ""
            }

            Box(
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.field.primary,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                slideInVertically(animationSpec = tween(330, delayMillis = 0))
                            )
                            .togetherWith(
                                fadeOut(animationSpec = tween(90)) + slideOutVertically(tween(220)),
                            )
                    },
                ) { text ->
                    Text(
                        modifier = Modifier.sizeIn(minWidth = width.size.width.dp + 8.dp, minHeight = 48.dp),
                        text = text,
                        style = TangemTheme.typography.h3,
                        color = TangemTheme.colors.text.primary1,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        var text by remember { mutableStateOf("123") }

        Column {
            PinTextField(
                value = text,
                onValueChange = { text = it },
                isPasswordVisual = false,
                length = 6,
            )

            Row {
                Button(onClick = { text += Random.nextInt(0, 10).toString() }) {
                    Text("Add Character")
                }
                Button(onClick = { text = text.dropLast(1) }) {
                    Text("Delete")
                }
            }
        }
    }
}