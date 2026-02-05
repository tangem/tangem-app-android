package com.tangem.core.ui.ds.message

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeForegroundEffectTangem
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.utils.toPx
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Different visual effects for [Tangem message component].
 */
enum class TangemMessageEffect(val isAnimatable: Boolean) {
    /** Magic effect with vibrant colors */
    Magic(true),
    /** Card effect with bright colors */
    Card(true),
    /** Warning effect with alert colors */
    Warning(false),
    /** No special effect */
    None(false),
    ;

    /** Gets the color gradient based on the effect type and [isInDarkTheme] */
    @Suppress("MagicNumber")
    fun getColorGradient(isInDarkTheme: Boolean): ImmutableList<Color> {
        return when (this) {
            Magic -> if (isInDarkTheme) {
                persistentListOf(
                    Color(0x9976E463),
                    Color(0x994A40D3),
                    Color(0x99CC4D8C),
                    Color(0x99E0BC56),
                )
            } else {
                persistentListOf(
                    Color(0xFF76E463),
                    Color(0xFF4A40D3),
                    Color(0xFFCC4D8C),
                    Color(0xFFE0BC56),
                )
            }
            Card -> if (isInDarkTheme) {
                persistentListOf(
                    Color(0x99D21FF2),
                    Color(0x991319F3),
                    Color(0x99FF005D),
                    Color(0x99EAB216),
                )
            } else {
                persistentListOf(
                    Color(0xFFD21FF2),
                    Color(0xFF1319F3),
                    Color(0xFFFF005D),
                    Color(0xFFEAB216),
                )
            }
            Warning -> if (isInDarkTheme) {
                persistentListOf(
                    Color(0x59FF0004),
                    Color(0x59FF0004),
                    Color(0x59E0BC56),
                    Color(0x59FF0000),
                    Color(0x59FFACC1),
                    Color(0x59FFACC1),
                )
            } else {
                persistentListOf(
                    Color(0x4DFF0004),
                    Color(0x4DFF0004),
                    Color(0x4DE0BC56),
                    Color(0x4DFF0000),
                    Color(0x4DFFACC1),
                    Color(0x4DFFACC1),
                )
            }
            None -> if (isInDarkTheme) {
                persistentListOf(
                    Color(0x1AFFFFFF),
                    Color(0x1AFFFFFF),
                )
            } else {
                persistentListOf()
            }
        }
    }

    /** Gets the gradient tints based on the effect type and [isInDarkTheme] */
    @Suppress("MagicNumber")
    fun getGradientTint(isInDarkTheme: Boolean) = when (this) {
        Magic,
        Card,
        -> {
            listOf(
                HazeTint(
                    color = Color(0x4DFFFFFF),
                    blendMode = BlendMode.Color,
                ),
                HazeTint(
                    color = Color(0x80C2C2C2),
                    blendMode = BlendMode.Overlay,
                ),
                HazeTint(
                    color = Color(0x1FC2C2C2),
                ),
            )
        }
        Warning -> {
            if (isInDarkTheme) {
                listOf(
                    HazeTint(
                        color = Color(0x4D7F7F7F),

                        blendMode = BlendMode.Luminosity,
                    ),
                    HazeTint(
                        color = Color(0x80C2C2C2),
                        blendMode = BlendMode.Overlay,
                    ),
                    HazeTint(
                        Color(0x1FFF0000),
                    ),
                )
            } else {
                listOf(
                    HazeTint(
                        color = Color(0x0DFEC2C2),
                    ),
                )
            }
        }
        None -> {
            listOf(
                HazeTint(
                    color = if (isInDarkTheme) Color(0x1AFFFFFF) else Color.Transparent,
                ),
            )
        }
    }

    /** Gets the border gradient based on the effect type and [isInDarkTheme] */
    @Suppress("MagicNumber")
    fun getBorderGradient(isInDarkTheme: Boolean): ImmutableList<Color> {
        return when (this) {
            Card,
            Magic,
            -> if (isInDarkTheme) {
                persistentListOf(
                    Color(0x4DFFFFFF),
                    Color(0x17FFFFFF),
                    Color(0x4DFFFFFF),
                    Color(0x17FFFFFF),
                )
            } else {
                persistentListOf(
                    Color(0x0d000000),
                    Color(0x26000000),
                    Color(0x0d000000),
                    Color(0x26000000),
                )
            }
            Warning -> persistentListOf(
                Color(0x4DE44848),
                Color(0x17E44848),
                Color(0x4DE44848),
                Color(0x17E44848),
            )
            None -> if (isInDarkTheme) {
                persistentListOf()
            } else {
                persistentListOf(
                    Color(0x0d000000),
                    Color(0x26000000),
                    Color(0x0d000000),
                    Color(0x26000000),
                )
            }
        }
    }

    /** Gets the border color based on the effect type and [isInDarkTheme] */
    @Suppress("MagicNumber")
    fun getBorderColor(isInDarkTheme: Boolean): Color {
        return when (this) {
            Card,
            Magic,
            None,
            -> if (isInDarkTheme) Color(0x1AFFFFFF) else Color.Transparent
            Warning -> Color(0x1AE44848)
        }
    }

    // todo redesign replace with proper color when design is ready
    @Suppress("MagicNumber")
    fun fallbackColor(isInDarkTheme: Boolean): Color {
        return when (this) {
            Card -> Color(0x339B8B86)
            Magic -> Color(0x33B43B96)
            Warning -> if (isInDarkTheme) Color(0x33FF0000) else Color(0x33FF0000)
            None -> if (isInDarkTheme) Color(0x1AFFFFFF) else Color.Transparent
        }
    }
}

/** Applies a message effect background to the [Modifier] based on the provided [messageEffect] and [radius] */
@Composable
internal fun Modifier.messageEffectBackground(
    messageEffect: TangemMessageEffect,
    radius: Dp,
    contentColor: Color,
): Modifier {
    val isInDarkTheme = LocalIsInDarkTheme.current
    val borderGradientColors = remember { messageEffect.getBorderGradient(isInDarkTheme) }
    val gradientColors = remember { messageEffect.getColorGradient(isInDarkTheme) }

    val angle by rememberAnimationAngle(messageEffect.isAnimatable)
    val brush = Brush.sweepGradient(messageEffect.getColorGradient(isInDarkTheme))
    val padding = 1.dp.toPx()

    return this
        .clip(RoundedCornerShape(radius))
        .border(
            width = 1.dp,
            color = messageEffect.getBorderColor(isInDarkTheme),
            shape = RoundedCornerShape(radius),
        )
        .conditionalCompose(borderGradientColors.isNotEmpty()) {
            border(
                width = 1.dp,
                brush = Brush.sweepGradient(
                    colors = messageEffect.getBorderGradient(isInDarkTheme),
                    center = Offset.Infinite,
                ),
                shape = RoundedCornerShape(radius),
            )
        }
        .hazeForegroundEffectTangem(
            style = HazeStyle(tints = messageEffect.getGradientTint(isInDarkTheme)),
            isBlurEnabled = true,
        ) {
            fallbackTint = HazeTint(
                messageEffect.fallbackColor(isInDarkTheme),
            )
            blurRadius = 25.dp
        }
        .conditionalCompose(gradientColors.isNotEmpty()) {
            drawWithContent {
                rotate(angle) {
                    drawCircle(
                        brush = brush,
                        radius = size.width,
                        blendMode = BlendMode.SrcIn,
                    )
                }
                drawRect(
                    color = contentColor,
                    topLeft = Offset(padding, padding),
                    size = Size(size.width - 2 * padding, size.height - 2 * padding),
                )
                drawContent()
            }
        }
}

@Composable
private fun rememberAnimationAngle(isAnimatable: Boolean) = if (isAnimatable) {
    val infiniteTransition = rememberInfiniteTransition()
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
} else {
    remember { mutableFloatStateOf(0f) }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemMessageEffect_Preview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TangemMessageEffect.entries.forEach { messageEffect ->
                Box(
                    modifier = Modifier
                        .messageEffectBackground(
                            messageEffect = messageEffect,
                            radius = 16.dp,
                            contentColor = TangemTheme.colors2.surface.level1,
                        )
                        .fillMaxWidth()
                        .height(height = 100.dp),
                )
            }
        }
    }
}
// endregion