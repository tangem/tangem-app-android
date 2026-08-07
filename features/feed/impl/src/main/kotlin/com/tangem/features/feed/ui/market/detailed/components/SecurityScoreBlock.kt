package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.components.TokenMarketInformationBlock
import com.tangem.features.feed.ui.market.detailed.state.SecurityScoreUM

@Composable
internal fun SecurityScoreBlock(state: SecurityScoreUM, modifier: Modifier = Modifier) {
    SecurityScoreBlockV2(state, modifier)
}

@Composable
private fun SecurityScoreBlockV2(state: SecurityScoreUM, modifier: Modifier = Modifier) {
    TokenMarketInformationBlock(
        modifier = modifier.clickable(onClick = state.onInfoClick),
        title = {
            TangemRowContainer(contentPadding = PaddingValues()) {
                Text(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
                    text = "${state.score}",
                    color = TangemTheme.colors2.text.neutral.primary,
                    style = TangemTheme.typography2.headingSemibold20,
                )

                InformationTextBlock(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_BOTTOM),
                    text = resourceReference(R.string.markets_token_details_security_score),
                    onInfoClick = state.onInfoClick,
                    informationTextBlockIconPosition = InformationTextBlockIconPosition.START,
                )

                Text(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_BOTTOM),
                    text = state.description.resolveReference(),
                    style = TangemTheme.typography2.captionMedium12,
                    color = TangemTheme.colors2.text.neutral.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                ScoreStarsBlock(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_TOP),
                    score = state.score,
                )
            }
        },
    )
}

@Composable
internal fun SecurityScoreBlockPlaceholder(modifier: Modifier = Modifier) {
    SecurityScoreBlockPlaceholderV2(modifier)
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
            style = TangemTheme.typography2.captionMedium12,
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
            style = TangemTheme.typography2.captionMedium12,
            radius = TangemTheme.dimens2.x25,
        )
    }
}

@Preview(widthDp = 328, showBackground = true)
@Preview(widthDp = 328, showBackground = true, locale = "ru")
@Preview(widthDp = 328, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreviewV2() {
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