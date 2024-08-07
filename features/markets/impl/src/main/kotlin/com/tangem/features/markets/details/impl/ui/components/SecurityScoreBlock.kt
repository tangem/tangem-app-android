package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.text.TooltipText
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.markets.details.impl.ui.entity.SecurityScoreUM
import com.tangem.features.markets.impl.R
import kotlin.math.round

private const val STARS_COUNT = 5

@Composable
internal fun SecurityScoreBlock(state: SecurityScoreUM, modifier: Modifier = Modifier) {
    val rounded = state.score.roundTo1decimal()
    val percentage = rounded / STARS_COUNT
    InformationBlock(
        modifier = modifier,
        title = {
            Column(
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing6),
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            ) {
                TooltipText(
                    text = resourceReference(R.string.markets_token_details_security_score),
                    onInfoClick = state.onInfoClick,
                    textStyle = TangemTheme.typography.subtitle2,
                )

                Text(
                    text = state.description,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        action = {
            Row(
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing6),
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = rounded.toString(),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                )
                Stars(fraction = percentage)
            }
        },
    )
}

@Suppress("MagicNumber")
@Composable
private fun Stars(@FloatRange(0.0, 1.0) fraction: Float = 0f) {
    val grayColor = TangemTheme.colors.icon.inactive

    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(times = 5) { i ->
            Box(
                modifier = Modifier.size(TangemTheme.dimens.size16),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier
                        .requiredSize(13.dp)
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
private fun Float.roundTo1decimal(): Float {
    return round(this * 10) / 10
}

@Composable
internal fun SecurityScorePlaceHolder(modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        title = {
            Column(
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing6),
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            ) {
                TextShimmer(
                    modifier = Modifier.fillMaxWidth(),
                    style = TangemTheme.typography.subtitle2,
                    textSizeHeight = true,
                )
                TextShimmer(
                    modifier = Modifier.fillMaxWidth(),
                    style = TangemTheme.typography.body2,
                    textSizeHeight = true,
                )
            }
        },
        action = {
            Box(
                modifier = Modifier.padding(
                    start = TangemTheme.dimens.spacing24,
                    bottom = TangemTheme.dimens.spacing6,
                ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                TextShimmer(
                    modifier = Modifier.fillMaxWidth(),
                    style = TangemTheme.typography.body2,
                    textSizeHeight = true,
                )
                RectangleShimmer(
                    modifier = Modifier
                        .height(TangemTheme.dimens.size16)
                        .fillMaxWidth(),
                    radius = TangemTheme.dimens.radius3,
                )
            }
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreview {
        SecurityScoreBlock(
            state = SecurityScoreUM(
                score = 3.5f,
                description = "Based on 3 ratings",
                onInfoClick = {},
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPlaceholder() {
    TangemThemePreview {
        PreviewShimmerContainer(
            shimmerContent = {
                SecurityScorePlaceHolder(
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            actualContent = {
                ContentPreview()
            },
        )
    }
}