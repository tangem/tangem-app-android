package com.tangem.features.feed.ui.feed.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.earn.components.EarnItemPlaceholderV1
import com.tangem.features.feed.ui.earn.components.EarnListItem
import com.tangem.features.feed.ui.earn.components.MostlyUsedCard
import com.tangem.features.feed.ui.earn.components.MostlyUsedPlaceholder
import com.tangem.features.feed.ui.earn.state.EarnListItemUM
import com.tangem.features.feed.ui.earn.state.EarnListUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun EarnBlock(onSeeAllClick: () -> Unit, earnListUM: EarnListUM, modifier: Modifier = Modifier) {
    if (earnListUM is EarnListUM.Empty) return
    if (LocalRedesignEnabled.current) {
        EarnBlockV2(onSeeAllClick = onSeeAllClick, earnListUM = earnListUM, modifier = modifier)
    } else {
        EarnBlockV1(onSeeAllClick = onSeeAllClick, earnListUM = earnListUM, modifier = modifier)
    }
}

@Composable
private fun EarnBlockV1(onSeeAllClick: () -> Unit, earnListUM: EarnListUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Header(
            title = {
                Text(
                    text = stringResourceSafe(R.string.markets_earn_common_title),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            onSeeAllClick = onSeeAllClick,
            isLoading = earnListUM is EarnListUM.Loading,
            shouldShowSeeAll = earnListUM is EarnListUM.Content,
        )

        SpacerH(12.dp)

        BlockCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
        ) {
            AnimatedContent(
                targetState = earnListUM,
                contentKey = { it::class.java },
            ) { earnListUM ->
                when (earnListUM) {
                    is EarnListUM.Content -> EarnContentBlock(items = earnListUM.items)
                    is EarnListUM.Error -> EarnErrorBlock(onRetryClick = earnListUM.onRetryClicked)
                    EarnListUM.Loading -> {
                        repeat(PLACEHOLDER_ITEM_COUNT) {
                            EarnItemPlaceholderV1()
                        }
                    }
                    EarnListUM.Empty -> Unit
                }
            }
        }
        SpacerH(32.dp)
    }
}

@Composable
private fun EarnBlockV2(onSeeAllClick: () -> Unit, earnListUM: EarnListUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Header(
            title = {
                Text(
                    text = stringResourceSafe(R.string.markets_earn_common_title),
                    style = TangemTheme.typography2.headingSemibold20,
                    color = TangemTheme.colors2.text.neutral.primary,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            onSeeAllClick = onSeeAllClick,
            isLoading = earnListUM is EarnListUM.Loading,
            shouldShowSeeAll = earnListUM is EarnListUM.Content,
        )

        SpacerH(12.dp)
        AnimatedContent(
            targetState = earnListUM,
            contentKey = { it::class.java },
        ) { earnListUM ->
            when (earnListUM) {
                is EarnListUM.Content -> EarnContentBlock(items = earnListUM.items)
                is EarnListUM.Error -> {
                    BlockCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors2.surface.level3),
                    ) {
                        EarnErrorBlock(onRetryClick = earnListUM.onRetryClicked)
                    }
                }
                EarnListUM.Loading -> {
                    MostlyUsedPlaceholder(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 8.dp,
                        ),
                    )
                }
                EarnListUM.Empty -> Unit
            }
        }
        SpacerH(32.dp)
    }
}

@Composable
private fun EarnErrorBlock(onRetryClick: () -> Unit) {
    UnableToLoadData(
        onRetryClick = onRetryClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 35.dp, horizontal = 10.dp),
    )
}

@Composable
private fun EarnContentBlock(items: ImmutableList<EarnListItemUM>) {
    if (LocalRedesignEnabled.current) {
        LazyRow(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = items,
                key = { item -> "${item.tokenName}-${item.network}" },
            ) { item ->
                MostlyUsedCard(
                    item = item,
                    onClick = item.onItemClick,
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.fastForEach { item ->
                EarnListItem(item = item)
            }
        }
    }
}

private const val PLACEHOLDER_ITEM_COUNT = 5