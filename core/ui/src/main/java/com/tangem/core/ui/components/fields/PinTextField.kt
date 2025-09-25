package com.tangem.core.ui.components.fields

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
    pinTextColor: PinTextColor,
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
            keyboardType = KeyboardType.NumberPassword,
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
                pinTextColor = pinTextColor,
                value = value,
            )
        },
    )

    LaunchedEffect(Unit) {
        delay(timeMillis = 200)
        focusRequester.requestFocus()
    }
}

enum class PinTextColor {
    Primary,
    WrongCode,
    Success,
}

@Suppress("MagicNumber", "LongMethod")
@Composable
private fun CellDecoration(
    length: Int,
    pinTextColor: PinTextColor,
    value: String,
    modifier: Modifier = Modifier,
    isPasswordVisual: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val minSize = textMeasurer.measure("0")
    val minWidth = maxOf(minSize.size.width.dp + 8.dp, 24.dp + 3.dp) // 24.dp is the minimum width of a pin cell
    val minHeight = maxOf(minSize.size.height.dp, 48.dp) // 48.dp is the minimum height of a pin cell

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

            val color = when (pinTextColor) {
                PinTextColor.Primary -> {
                    if (isPasswordVisual) {
                        TangemTheme.colors.icon.informative
                    } else {
                        TangemTheme.colors.text.primary1
                    }
                }
                PinTextColor.WrongCode -> TangemTheme.colors.icon.warning
                PinTextColor.Success -> TangemTheme.colors.icon.accent
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
                            fadeIn(animationSpec = tween(90, delayMillis = 90)) +
                                slideInVertically(animationSpec = tween(220, delayMillis = 0))
                            )
                            .togetherWith(
                                fadeOut(animationSpec = tween(90)) + slideOutVertically(tween(220)),
                            )
                    },
                ) { text ->
                    if (isPasswordVisual && text.isNotEmpty()) {
                        Canvas(
                            Modifier.sizeIn(minWidth = minWidth, minHeight = minHeight),
                        ) {
                            drawCircle(
                                color = color,
                                radius = 4.dp.toPx(),
                                center = center,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.sizeIn(minWidth = minWidth, minHeight = minHeight),
                        ) {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = text,
                                style = TangemTheme.typography.h3,
                                color = color,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
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
                isPasswordVisual = true,
                pinTextColor = PinTextColor.Success,
                length = 6,
            )
            PinTextField(
                value = text,
                onValueChange = { text = it },
                isPasswordVisual = false,
                pinTextColor = PinTextColor.Primary,
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