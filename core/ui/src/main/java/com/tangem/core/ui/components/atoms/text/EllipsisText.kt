package com.tangem.core.ui.components.atoms.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Constraints
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import java.text.NumberFormat
import java.util.Locale

sealed class TextEllipsis {

    data object Middle : TextEllipsis()

    object End : TextEllipsis()

    data class OffsetEnd(
        val offsetEnd: Int = 0,
        val hasSeparator: Boolean = true,
    ) : TextEllipsis()
}

/**
 * https://stackoverflow.com/questions/69083061/how-to-make-middle-ellipsis-in-text-with-jetpack-compose
 *
 * Customized Text with ellipsis. Ellipsis can be placed in: Middle, End or OffsetEnd (OffsetEnd with separator).
 *
 * * OffsetEnd can be useful to display big amounts with currency symbol. OffsetEnd 0 is equal to End.
 */
@Suppress("LongMethod")
@Composable
fun EllipsisText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    ellipsis: TextEllipsis = TextEllipsis.End,
) {
    val ellipsisText = remember(text) {
        if (ellipsis is TextEllipsis.OffsetEnd && ellipsis.hasSeparator) {
            ELLIPSIS_TEXT_WITH_SEPARATOR
        } else {
            ELLIPSIS_TEXT
        }
    }

    // some letters, like "r", will have less width when placed right before "."
    // adding a space to prevent such case
    val layoutText = remember(text) { "$text $ellipsisText" }
    val textLayoutResultState = remember(layoutText) {
        mutableStateOf<TextLayoutResult?>(null)
    }
    SubcomposeLayout(modifier) { constraints ->
        // result is ignored - we only need to fill our textLayoutResult
        subcompose("measure") {
            Text(
                text = layoutText,
                color = color,
                style = style,
                fontStyle = fontStyle,
                textDecoration = textDecoration,
                textAlign = textAlign,
                softWrap = softWrap,
                maxLines = 1,
                onTextLayout = { textLayoutResultState.value = it },
                modifier = modifier,
            )
        }.first().measure(Constraints())
        // to allow smart cast
        val textLayoutResult = textLayoutResultState.value
            ?: // shouldn't happen - onTextLayout is called before subcompose finishes
            return@SubcomposeLayout layout(0, 0) {}
        val placeable = subcompose("visible") {
            val finalText = remember(text, textLayoutResult, constraints.maxWidth) {
                if (
                    text.isEmpty() ||
                    textLayoutResult.getBoundingBox(text.indices.last).right <= constraints.maxWidth
                ) {
                    // text not including ellipsis fits on the first line.
                    return@remember text
                }

                var ellipsisWidth = 0f
                layoutText.indices.toList()
                    .takeLast(ellipsisText.length)
                    .forEach widthLet@{
                        ellipsisWidth += textLayoutResult.getBoundingBox(it).width
                    }

                val availableWidth = constraints.maxWidth - ellipsisWidth
                val startCounter = BoundCounter(text, textLayoutResult) { it }
                val endCounter = BoundCounter(text, textLayoutResult) { text.indices.last - it }

                when (ellipsis) {
                    TextEllipsis.Middle -> {
                        middleEllipsisText(
                            availableWidth,
                            startCounter,
                            endCounter,
                        )
                    }
                    TextEllipsis.End -> {
                        offsetEndEllipsisText(
                            availableWidth = availableWidth,
                            startCounter = startCounter,
                            endCounter = endCounter,
                        )
                    }
                    is TextEllipsis.OffsetEnd -> {
                        val locale = Locale.getDefault()
                        val formatter = NumberFormat.getCurrencyInstance(locale)
                        val formattedValue = formatter.format(1)

                        val isPrefixSymbol = formattedValue.startsWith(formatter.currency?.symbol.orEmpty())

                        if (isPrefixSymbol) {
                            offsetEndEllipsisText(
                                availableWidth = availableWidth,
                                startCounter = startCounter,
                                endCounter = endCounter,
                            )
                        } else {
                            offsetEndEllipsisText(
                                availableWidth = availableWidth,
                                startCounter = startCounter,
                                endCounter = endCounter,
                                offsetEnd = ellipsis.offsetEnd,
                                withSeparator = ellipsis.hasSeparator,
                            )
                        }
                    }
                }
            }
            Text(
                text = finalText,
                color = color,
                fontStyle = fontStyle,
                textAlign = textAlign,
                softWrap = softWrap,
                onTextLayout = onTextLayout,
                style = style,
            )
        }[0].measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

private const val ELLIPSIS_SEPARATOR = " "
private const val ELLIPSIS_TEXT = "..."
private const val ELLIPSIS_TEXT_WITH_SEPARATOR = ELLIPSIS_TEXT.plus(ELLIPSIS_SEPARATOR)

private fun middleEllipsisText(availableWidth: Float, startCounter: BoundCounter, endCounter: BoundCounter): String {
    while (availableWidth - startCounter.width - endCounter.width > 0) {
        val possibleEndWidth = endCounter.widthWithNextChar()
        if (
            startCounter.width >= possibleEndWidth &&
            availableWidth - startCounter.width - possibleEndWidth >= 0
        ) {
            endCounter.addNextChar()
        } else if (availableWidth - startCounter.widthWithNextChar() - endCounter.width >= 0) {
            startCounter.addNextChar()
        } else {
            break
        }
    }
    return startCounter.string.trimEnd() + ELLIPSIS_TEXT + endCounter.string.reversed().trimStart()
}

private fun offsetEndEllipsisText(
    availableWidth: Float,
    startCounter: BoundCounter,
    endCounter: BoundCounter,
    offsetEnd: Int = 0,
    withSeparator: Boolean = false,
): String {
    while (availableWidth - startCounter.width - endCounter.width > 0) {
        val possibleEndWidth = endCounter.widthWithNextChar()
        if (
            offsetEnd > endCounter.string.length &&
            availableWidth - startCounter.width - possibleEndWidth >= 0
        ) {
            endCounter.addNextChar()
        } else if (availableWidth - startCounter.widthWithNextChar() - endCounter.width >= 0) {
            startCounter.addNextChar()
        } else {
            break
        }
    }
    val ellipsis = if (withSeparator) ELLIPSIS_TEXT_WITH_SEPARATOR else ELLIPSIS_TEXT

    return startCounter.string.trimEnd() + ellipsis + endCounter.string.reversed().trimStart()
}

//region Preview
@Preview(widthDp = 200)
@Composable
private fun EllipsisTexPreview(@PreviewParameter(EllipsisTexPreviewParameterProvider::class) ellipsis: TextEllipsis) {
    TangemThemePreview {
        EllipsisText(
            text = "11111111111111111111111111111111111111111111111111 END",
            ellipsis = ellipsis,
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .fillMaxWidth(),
        )
    }
}

private class EllipsisTexPreviewParameterProvider : PreviewParameterProvider<TextEllipsis> {
    override val values: Sequence<TextEllipsis>
        get() = sequenceOf(
            TextEllipsis.Middle,
            TextEllipsis.End,
            TextEllipsis.OffsetEnd("TEXT".length),
            TextEllipsis.OffsetEnd("TEXT".length, false),
        )
}
//endregion