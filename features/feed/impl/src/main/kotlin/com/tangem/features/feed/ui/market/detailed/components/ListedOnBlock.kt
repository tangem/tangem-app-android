package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_24
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.features.feed.ui.components.TokenMarketInformationBlock
import com.tangem.features.feed.ui.market.detailed.state.ListedOnUM

/**
 * "Listed on" block
 *
 * @param state block state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun ListedOnBlock(state: ListedOnUM, modifier: Modifier = Modifier) {
    TokenMarketInformationBlock(
        modifier = modifier
            .clickable(enabled = state is ListedOnUM.Content) {
                (state as? ListedOnUM.Content)?.onClick?.invoke()
            }
            .testTag(MarketsTestTags.LISTED_ON_BLOCK),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = state.title.resolveReference(),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        modifier = Modifier.testTag(MarketsTestTags.LISTED_ON_EXCHANGES_COUNT),
                        text = state.description.resolveReference(),
                        style = TangemTheme.typography3.heading.small,
                        color = TangemTheme.colors3.text.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                SpacerWMax()

                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.ic_chevron_right_24,
                    tint = TangemTheme.colors3.icon.secondary,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
internal fun ListedOnBlockPlaceholder(modifier: Modifier = Modifier) {
    TokenMarketInformationBlock(
        modifier = modifier.fillMaxWidth(),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ListedOnShimmerLine(
                    style = TangemTheme.typography3.caption.medium,
                    width = 120.dp,
                )
                ListedOnShimmerLine(
                    style = TangemTheme.typography3.heading.small,
                    width = 66.dp,
                )
            }
        },
    )
}

@Composable
private fun ListedOnShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}