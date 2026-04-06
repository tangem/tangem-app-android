package com.tangem.features.feed.ui.market.detailed.components

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import kotlin.math.round

private const val STARS_COUNT = 5

@Composable
internal fun ScoreStarsBlock(
    score: Float,
    horizontalSpacing: Dp,
    scoreTextStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    if (LocalRedesignEnabled.current) {
        ScoreStarsBlockV2(
            score = score,
            modifier = modifier,
        )
    } else {
        ScoreStarsBlockV1(
            score = score,
            horizontalSpacing = horizontalSpacing,
            scoreTextStyle = scoreTextStyle,
            modifier = modifier,
        )
    }
}

@Composable
private fun ScoreStarsBlockV1(
    score: Float,
    horizontalSpacing: Dp,
    scoreTextStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val rounded = score.roundTo1decimal()
    val percentage = rounded / STARS_COUNT
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rounded.toString(),
            style = scoreTextStyle,
            color = TangemTheme.colors.text.primary1,
        )
        Stars(fraction = percentage)
    }
}

@Composable
private fun ScoreStarsBlockV2(score: Float, modifier: Modifier = Modifier) {
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
    if (LocalRedesignEnabled.current) {
        StarsV2(fraction)
    } else {
        StarsV1(fraction)
    }
}

@Suppress("MagicNumber")
@Composable
private fun StarsV1(@FloatRange(0.0, 1.0) fraction: Float = 0f) {
    val grayColor = TangemTheme.colors.icon.inactive

    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(times = STARS_COUNT) { i ->
            Box(
                modifier = Modifier.size(TangemTheme.dimens.size16),
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
                    imageVector = ImageVector.vectorResource(R.drawable.ic_star_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.accent,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun StarsV2(@FloatRange(0.0, 1.0) fraction: Float = 0f) {
    val grayColor = TangemTheme.colors2.markers.iconGray

    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(times = STARS_COUNT) { i ->
            Box(
                modifier = Modifier.size(TangemTheme.dimens2.x5),
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
                    tint = TangemTheme.colors2.markers.iconBlue,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
private fun Float.roundTo1decimal(): Float {
    return round(this * 10) / 10
}