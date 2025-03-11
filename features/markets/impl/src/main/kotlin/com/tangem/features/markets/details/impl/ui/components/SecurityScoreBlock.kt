package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.TextShimmer
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
    Row(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .fillMaxWidth()
            .heightIn(max = TangemTheme.dimens.size72)
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1F)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
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
        ScoreStarsBlock(
            score = state.score,
            scoreTextStyle = TangemTheme.typography.body1,
            horizontalSpacing = TangemTheme.dimens.spacing8,
        )
    }
}

@Composable
internal fun SecurityScoreBlockPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .heightIn(max = TangemTheme.dimens.size72)
            .padding(all = TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.4f)
                .padding(vertical = TangemTheme.dimens.spacing2)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
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

        TextShimmer(
            modifier = Modifier.fillMaxWidth(fraction = 0.5f),
            style = TangemTheme.typography.body2,
            textSizeHeight = true,
        )
    }
}

@Preview(widthDp = 328, showBackground = true)
@Preview(widthDp = 328, showBackground = true, locale = "ru")
@Preview(widthDp = 328, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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

@Preview(widthDp = 328, showBackground = true)
@Preview(widthDp = 328, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPlaceholder() {
    TangemThemePreview {
        PreviewShimmerContainer(
            shimmerContent = {
                SecurityScoreBlockPlaceholder(
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            actualContent = {
                ContentPreview()
            },
        )
    }
}