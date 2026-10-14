@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.tokenrow

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlin.math.abs
import kotlin.math.min

/**
 * Pending-transaction indicator of [TangemTokenRow] — three accent-blue dots cycling through three
 * states. In every state two dots are highlighted and one is idle; the idle one moves along the row,
 * so the highlight reads as a wave.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9069-3158)
 *
 * The idle dot fades to [IDLE_ALPHA] and shrinks to [IDLE_SCALE]. Scaling happens in the draw layer,
 * so the indicator keeps a constant footprint and never reflows the title line it sits in.
 *
 * @param modifier Modifier applied to the dots row.
 * @param color Dot color. Highlighted dots use it as is, the idle one at [IDLE_ALPHA].
 */
@Composable
internal fun TokenRowPendingDots(modifier: Modifier = Modifier, color: Color = TangemTheme.colors3.icon.accent.blue) {
    // Not a delay loop: Compose UI tests never go idle while a finite animation keeps restarting.
    val idlePosition by rememberInfiniteTransition(label = "TokenRowPendingDots").animateFloat(
        initialValue = INITIAL_IDLE_INDEX.toFloat(),
        targetValue = (INITIAL_IDLE_INDEX + DOT_COUNT).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = STEP_DURATION_MILLIS * DOT_COUNT
                repeat(DOT_COUNT) { step ->
                    (INITIAL_IDLE_INDEX + step).toFloat() at STEP_DURATION_MILLIS * step using FastOutSlowInEasing
                }
            },
        ),
        label = "TokenRowPendingDotsIdlePosition",
    )

    Row(
        modifier = modifier.progressSemantics(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(DOT_COUNT) { index ->
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .graphicsLayer {
                        val idleness = idleness(dotIndex = index, idlePosition = idlePosition)
                        val scale = lerp(start = 1f, stop = IDLE_SCALE, fraction = idleness)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(start = 1f, stop = IDLE_ALPHA, fraction = idleness)
                    }
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}

private fun idleness(dotIndex: Int, idlePosition: Float): Float {
    val offset = abs(idlePosition % DOT_COUNT - dotIndex)
    val distance = min(offset, DOT_COUNT - offset)
    return (1f - distance).coerceIn(0f, 1f)
}

private const val DOT_COUNT = 3
private const val STEP_DURATION_MILLIS = 400
private const val INITIAL_IDLE_INDEX = 1
private const val IDLE_ALPHA = 0.4f
private const val IDLE_SCALE = 0.75f

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TokenRowPendingDots_Preview() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(12.dp),
        ) {
            TokenRowPendingDots()
        }
    }
}