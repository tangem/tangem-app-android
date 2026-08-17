@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.tokenrow

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.coroutines.delay

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
    var idleIndex by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(timeMillis = STEP_DURATION_MILLIS)
            idleIndex = (idleIndex + 1) % DOT_COUNT
        }
    }

    Row(
        modifier = modifier.progressSemantics(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(DOT_COUNT) { index ->
            val isIdle = index == idleIndex
            val alpha by animateFloatAsState(
                targetValue = if (isIdle) IDLE_ALPHA else 1f,
                animationSpec = tween(durationMillis = STEP_DURATION_MILLIS.toInt()),
                label = "TokenRowPendingDotAlpha",
            )
            val scale by animateFloatAsState(
                targetValue = if (isIdle) IDLE_SCALE else 1f,
                animationSpec = tween(durationMillis = STEP_DURATION_MILLIS.toInt()),
                label = "TokenRowPendingDotScale",
            )

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(color = color.copy(alpha = alpha), shape = CircleShape),
            )
        }
    }
}

private const val DOT_COUNT = 3
private const val STEP_DURATION_MILLIS = 400L
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