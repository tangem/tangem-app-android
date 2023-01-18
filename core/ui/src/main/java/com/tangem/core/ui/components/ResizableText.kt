package com.tangem.core.ui.components

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Suppress("MagicNumber")
@Composable
fun ResizableText(
    text: String,
    fontSizeRange: FontSizeRange,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val fontSizeValue = remember { mutableStateOf(fontSizeRange.max.value) }
    val readyToDraw = remember { mutableStateOf(false) }

    val textState = remember { mutableStateOf(text) }
    if (textState.value != text) {
        readyToDraw.value = false
        fontSizeValue.value = fontSizeRange.max.value
        textState.value = text
    }

    Text(
        modifier = modifier.drawWithContent { if (readyToDraw.value) drawContent() },
        text = text,
        color = color,
        softWrap = false,
        style = style,
        fontSize = fontSizeValue.value.sp,
        onTextLayout = {
            if (it.hasVisualOverflow) {
                val nextFontSizeValue = fontSizeValue.value - fontSizeRange.step.value
                if (nextFontSizeValue <= fontSizeRange.min.value) {
                    fontSizeValue.value = fontSizeRange.min.value
                    readyToDraw.value = true
                } else {
                    fontSizeValue.value = nextFontSizeValue * 0.8f
                }
            } else {
                readyToDraw.value = true
            }
        },
    )
}

data class FontSizeRange(
    val min: TextUnit,
    val max: TextUnit,
    val step: TextUnit = DEFAULT_TEXT_STEP,
) {
    init {
        require(min < max) { "min should be less than max, $this" }
        require(step.value > 0) { "step should be greater than 0, $this" }
    }

    companion object {
        private val DEFAULT_TEXT_STEP = 1.sp
    }
}
