package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.TooltipText
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.utils.PreviewShimmerContainer
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.components.ContainerWithDivider
import com.tangem.features.feed.ui.market.detailed.state.SecurityScoreUM

@Composable
internal fun SecurityScoreBlock(state: SecurityScoreUM, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        SecurityScoreBlockV2(state, modifier)
    } else {
        SecurityScoreBlockV1(state, modifier)
    }
}

@Composable
private fun SecurityScoreBlockV1(state: SecurityScoreUM, modifier: Modifier = Modifier) {
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
private fun SecurityScoreBlockV2(state: SecurityScoreUM, modifier: Modifier = Modifier) {
    ContainerWithDivider(
        modifier = modifier,
        showDivider = true,
    ) {
        TangemRowContainer(modifier = Modifier.padding(top = 20.dp, bottom = 24.dp)) {
            Text(
                modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
                text = "${state.score}",
                color = TangemTheme.colors2.text.neutral.primary,
                style = TangemTheme.typography2.headingBold28,
            )

            InformationTextBlock(
                modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_BOTTOM),
                text = resourceReference(R.string.markets_token_details_security_score),
                onInfoClick = state.onInfoClick,
                textColor = TangemTheme.colors2.text.neutral.primary,
                informationTextBlockIconPosition = InformationTextBlockIconPosition.END,
            )

            Text(
                modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_BOTTOM),
                text = state.description.resolveReference(),
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            ScoreStarsBlock(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .layoutId(layoutId = TangemRowLayoutId.END_TOP),
                score = state.score,
                scoreTextStyle = TangemTheme.typography.body1,
                horizontalSpacing = TangemTheme.dimens.spacing8,
            )
        }
    }
}

@Composable
internal fun SecurityScoreBlockPlaceholder(modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        SecurityScoreBlockPlaceholderV2(modifier)
    } else {
        SecurityScoreBlockPlaceholderV1(modifier)
    }
}

@Composable
private fun SecurityScoreBlockPlaceholderV2(modifier: Modifier = Modifier) {
    TangemRowContainer(modifier = modifier) {
        TextShimmer(
            modifier = Modifier
                .width(114.dp)
                .layoutId(layoutId = TangemRowLayoutId.START_TOP),
            style = TangemTheme.typography2.headingBold28,
            radius = TangemTheme.dimens2.x25,
        )

        TextShimmer(
            modifier = Modifier
                .width(74.dp)
                .padding(top = 8.dp)
                .layoutId(layoutId = TangemRowLayoutId.START_BOTTOM),
            style = TangemTheme.typography2.captionSemibold12,
            radius = TangemTheme.dimens2.x25,
        )

        TextShimmer(
            modifier = Modifier
                .width(116.dp)
                .layoutId(layoutId = TangemRowLayoutId.END_TOP),
            style = TangemTheme.typography2.headingBold28,
            radius = TangemTheme.dimens2.x25,
        )

        TextShimmer(
            modifier = Modifier
                .width(96.dp)
                .padding(top = 8.dp)
                .layoutId(layoutId = TangemRowLayoutId.END_BOTTOM),
            style = TangemTheme.typography2.captionSemibold12,
            radius = TangemTheme.dimens2.x25,
        )
    }
}

@Composable
private fun SecurityScoreBlockPlaceholderV1(modifier: Modifier = Modifier) {
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
private fun ContentPreviewV1() {
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
private fun PreviewPlaceholderV1() {
    TangemThemePreview {
        PreviewShimmerContainer(
            shimmerContent = {
                SecurityScoreBlockPlaceholder(
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            actualContent = {
                ContentPreviewV1()
            },
        )
    }
}

@Preview(widthDp = 328, showBackground = true)
@Preview(widthDp = 328, showBackground = true, locale = "ru")
@Preview(widthDp = 328, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreviewV2() {
    CompositionLocalProvider(
        LocalRedesignEnabled provides true,
    ) {
        TangemThemePreviewRedesign {
            Column(modifier = Modifier.background(TangemTheme.colors2.surface.level2)) {
                SecurityScoreBlock(
                    state = SecurityScoreUM(
                        score = 3.5f,
                        description = stringReference("Based on 3 ratings"),
                        onInfoClick = {},
                    ),
                )

                SpacerH(10.dp)

                SecurityScoreBlockPlaceholder(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}