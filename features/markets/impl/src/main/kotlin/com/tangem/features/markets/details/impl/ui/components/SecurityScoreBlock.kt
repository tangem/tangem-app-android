package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.text.TooltipText
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.markets.details.impl.ui.state.SecurityScoreUM
import com.tangem.features.markets.impl.R

@Composable
internal fun SecurityScoreBlock(state: SecurityScoreUM, modifier: Modifier = Modifier) {
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
                    text = state.description.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        action = {
            ScoreStarsBlock(
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing6),
                score = state.score,
                scoreTextStyle = TangemTheme.typography.body1,
                horizontalSpacing = TangemTheme.dimens.spacing8,
            )
        },
    )
}

@Composable
private fun SecurityScorePlaceHolder(modifier: Modifier = Modifier) {
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
                description = stringReference("Based on 3 ratings"),
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
