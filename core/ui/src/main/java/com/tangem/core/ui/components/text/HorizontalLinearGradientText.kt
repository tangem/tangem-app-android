package com.tangem.core.ui.components.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Text with horizontal linear gradient.
 *
 * @param text Text to display
 * @param modifier Composable modifier
 * @param gradientColors Gradient colors applied horizontally
 * @param textStyle Text style
 */
@Composable
fun HorizontalLinearGradientText(
    text: String,
    gradientColors: List<Color>,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(SpanStyle(Brush.linearGradient(gradientColors))) {
                append(text)
            }
        },
        style = textStyle,
    )
}
