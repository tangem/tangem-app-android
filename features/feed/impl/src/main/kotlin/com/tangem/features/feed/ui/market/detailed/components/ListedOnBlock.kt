package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
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
    ListedOnBlockV2(state, modifier)
}

@Composable
private fun ListedOnBlockV2(state: ListedOnUM, modifier: Modifier = Modifier) {
    TokenMarketInformationBlock(
        modifier = modifier
            .clickable(enabled = state is ListedOnUM.Content) {
                (state as? ListedOnUM.Content)?.onClick?.invoke()
            }
            .testTag(MarketsTestTags.LISTED_ON_BLOCK),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1)) {
                    Text(
                        text = state.title.resolveReference(),
                        style = TangemTheme.typography2.captionMedium12,
                        color = TangemTheme.colors2.text.neutral.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        modifier = Modifier.testTag(MarketsTestTags.LISTED_ON_EXCHANGES_COUNT),
                        text = state.description.resolveReference(),
                        style = TangemTheme.typography2.headingSemibold20,
                        color = TangemTheme.colors2.text.neutral.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                SpacerWMax()

                Icon(
                    modifier = Modifier.size(TangemTheme.dimens2.x5),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_small_right_24),
                    tint = TangemTheme.colors2.markers.iconGray,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
internal fun ListedOnBlockPlaceholder(modifier: Modifier = Modifier) {
    ListedOnBlockPlaceholderV2(modifier)
}

@Composable
internal fun ListedOnBlockPlaceholderV2(modifier: Modifier = Modifier) {
    TokenMarketInformationBlock(
        modifier = modifier.fillMaxWidth(),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextShimmer(
                    style = TangemTheme.typography2.headingSemibold20,
                    modifier = Modifier.width(120.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                TextShimmer(
                    style = TangemTheme.typography2.captionMedium13,
                    modifier = Modifier.width(66.dp),
                    radius = TangemTheme.dimens2.x25,
                )
            }
        },
    )
}