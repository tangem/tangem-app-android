package com.tangem.core.ui.components.text

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.LocalBladeAnimation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlin.math.sqrt

data class BladeAnimation(
    val offsetState: State<Float>,
)

@Composable
fun rememberBladeAnimation(): BladeAnimation {
    val infiniteTransition = rememberInfiniteTransition(label = "BladeAnimation")
    val offsetState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
        ),
        label = "BladeOffset",
    )

    return remember(offsetState) {
        BladeAnimation(offsetState)
    }
}

@Suppress("MagicNumber")
@Composable
fun TextStyle.applyBladeBrush(isEnabled: Boolean, textColor: Color): TextStyle {
    val offsetState = LocalBladeAnimation.current.offsetState
    val offset = if (isEnabled) offsetState.value else 0f

    val brush = remember(offset, textColor, isEnabled) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                if (!isEnabled) {
                    // Solid color via the same shader path (LinearGradientShader needs >= 2 colors).
                    return LinearGradientShader(
                        colors = listOf(textColor, textColor),
                        from = Offset.Zero,
                        to = Offset(size.width, size.height),
                        tileMode = TileMode.Clamp,
                    )
                }

                val center = Offset(size.width / 2f, size.height / 2f)
                val diagonal = sqrt(size.width * size.width + size.height * size.height)
                // Subtle diagonal angle, similar to iOS shimmer
                val direction = Offset(x = 1f, y = 0.3f)

                // Half-width of the blob (80% of diagonal total — wide, soft sweep)
                val bandHalf = diagonal * 0.40f

                // Sweep the highlight center from left-of-element to right-of-element.
                // offset 0..1 maps to a full pass including off-screen padding on both sides.
                val shift = direction * ((offset - 0.5f) * diagonal * 1.5f)
                val highlightCenter = center + shift

                // Full color text with a wide, gradual low-alpha dip sweeping left → right
                return LinearGradientShader(
                    colors = listOf(
                        textColor,
                        textColor.copy(alpha = 0.75f),
                        textColor.copy(alpha = 0.45f),
                        textColor.copy(alpha = 0.3f),
                        textColor.copy(alpha = 0.45f),
                        textColor.copy(alpha = 0.75f),
                        textColor,
                    ),
                    from = highlightCenter - direction * bandHalf,
                    to = highlightCenter + direction * bandHalf,
                    colorStops = listOf(0f, 0.15f, 0.35f, 0.5f, 0.65f, 0.85f, 1f),
                    tileMode = TileMode.Clamp,
                )
            }
        }
    }

    return this.copy(brush = brush)
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        Text(
            "Blade animation text",
            style = TangemTheme.typography.body1.applyBladeBrush(true, Color.Black),
        )
    }
}