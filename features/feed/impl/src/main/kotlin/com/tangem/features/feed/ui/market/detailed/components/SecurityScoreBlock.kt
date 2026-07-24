package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
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
    TokenMarketInformationBlock(
        modifier = modifier.clickable(onClick = state.onInfoClick),
        title = {
            TangemRowContainer(contentPadding = PaddingValues()) {
                Text(
                    modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
                    text = "${state.score}",
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.heading.small,
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
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
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
    TangemRowContainer(modifier = modifier) {
        SecurityScoreShimmerLine(
            style = TangemTheme.typography3.heading.small,
            width = 114.dp,
            modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
        )

        SecurityScoreShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 74.dp,
            modifier = Modifier
                .padding(top = 8.dp)
                .layoutId(layoutId = TangemRowLayoutId.START_BOTTOM),
        )

        SecurityScoreShimmerLine(
            style = TangemTheme.typography3.heading.small,
            width = 116.dp,
            modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.END_TOP),
        )

        SecurityScoreShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 96.dp,
            modifier = Modifier
                .padding(top = 8.dp)
                .layoutId(layoutId = TangemRowLayoutId.END_BOTTOM),
        )
    }
}

@Composable
private fun SecurityScoreShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Preview(widthDp = 328, showBackground = true)
@Preview(widthDp = 328, showBackground = true, locale = "ru")
@Preview(widthDp = 328, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContentPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
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