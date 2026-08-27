package com.tangem.features.feed.ui.market.detailed.components

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import kotlin.math.round

private const val STARS_COUNT = 5

@Composable
internal fun ScoreStarsBlock(score: Float, modifier: Modifier = Modifier) {
    val rounded = score.roundTo1decimal()
    val percentage = rounded / STARS_COUNT
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Stars(fraction = percentage)
    }
}

@Suppress("MagicNumber")
@Composable
private fun Stars(@FloatRange(0.0, 1.0) fraction: Float = 0f) {
    val grayColor = TangemTheme.colors3.icon.secondary

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(times = STARS_COUNT) { i ->
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier
                        .requiredSize(16.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithCache {
                            onDrawWithContent {
                                val starFraction = ((fraction - i * 0.2) / 0.2).coerceIn(0.0, 1.0)
                                val starFractionFloat = starFraction
                                    .toFloat()
                                    .roundTo1decimal()

                                drawContent()
                                drawRect(
                                    color = grayColor,
                                    topLeft = Offset(x = size.width * starFractionFloat, y = 0f),
                                    size = Size(size.width * (1 - starFractionFloat), size.height),
                                    blendMode = BlendMode.SrcIn,
                                )
                            }
                        },
                    imageVector = ImageVector.vectorResource(R.drawable.ic_star_filled_20),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.brand,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
private fun Float.roundTo1decimal(): Float {
    return round(this * 10) / 10
}