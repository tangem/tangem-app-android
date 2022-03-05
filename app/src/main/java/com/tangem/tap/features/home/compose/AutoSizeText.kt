package com.tangem.tap.features.home.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.ExperimentalUnitApi

@OptIn(ExperimentalUnitApi::class)
@Composable
fun AutoSizeText(
    text: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val scaledTextStyle = remember { mutableStateOf(textStyle) }
    val readyToDraw = remember { mutableStateOf(false) }

    val onTextLayout: (TextLayoutResult) -> Unit = { result ->
        if (result.didOverflowWidth) {
            val fontSize = result.size.width / result.multiParagraph.width
            scaledTextStyle.value =
//                scaledTextStyle.value.copy(fontSize = TextUnit(fontSize, TextUnitType.Sp))
                scaledTextStyle.value.copy(fontSize = scaledTextStyle.value.fontSize * 0.9)
        } else {
            readyToDraw.value = true
        }
    }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw.value) {
                drawContent()
            }
        },
        softWrap = false,
        onTextLayout = onTextLayout,
        style = scaledTextStyle.value,
        )
}