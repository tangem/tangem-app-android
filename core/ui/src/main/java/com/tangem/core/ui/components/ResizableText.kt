package com.tangem.core.ui.components

import androidx.annotation.FloatRange
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * A Composable function that displays text which can be resized based on its content's overflow.
 *
 * This function draws text on the screen and checks if it overflows. If the text overflows,
 * its font size is reduced recursively until it either fits the available space or reaches a
 * specified minimum font size.
 */
@Composable
fun ResizableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    minFontSize: TextUnit = TextUnit.Unspecified,
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
    reduceFactor: Double = 0.9,
) {
    var fontSize by remember { mutableStateOf(style.fontSize) }
    var readyToDraw by remember { mutableStateOf(value = false) }

    Text(
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        text = text,
        color = color,
        fontSize = fontSize,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style,
        onTextLayout = { result ->
            fun reduceFontSize() {
                val reducedFontSize = fontSize * reduceFactor

                if (minFontSize != TextUnit.Unspecified && reducedFontSize <= minFontSize) {
                    fontSize = minFontSize
                    readyToDraw = true
                } else {
                    fontSize = reducedFontSize
                }
            }

            if (result.hasVisualOverflow) {
                reduceFontSize()
            } else {
                readyToDraw = true
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
