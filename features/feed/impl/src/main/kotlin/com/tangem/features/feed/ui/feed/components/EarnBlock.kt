package com.tangem.features.feed.ui.feed.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.earn.components.EarnListItem
import com.tangem.features.feed.ui.earn.components.EarnListPlaceholder
import com.tangem.features.feed.ui.earn.state.EarnListItemUM
import com.tangem.features.feed.ui.earn.state.EarnListUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun EarnBlock(onSeeAllClick: () -> Unit, earnListUM: EarnListUM?, modifier: Modifier = Modifier) {
    if (earnListUM == null) return

    Column(modifier = modifier) {
        Header(
            title = {
                Text(
                    text = stringResourceSafe(R.string.markets_earn_common_title),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                )
            },
            onSeeAllClick = onSeeAllClick,
            isLoading = earnListUM is EarnListUM.Loading,
        )

        SpacerH(12.dp)

        BlockCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
        ) {
            AnimatedContent(targetState = earnListUM) { earnListState ->
                when (earnListState) {
                    is EarnListUM.Content -> EarnContentBlock(items = earnListState.items)
                    is EarnListUM.Error -> EarnErrorBlock(onRetryClick = earnListState.onRetryClicked)
                    EarnListUM.Loading -> EarnListPlaceholder(placeholderCount = PLACEHOLDER_ITEM_COUNT)
                }
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
    Column(modifier = Modifier.fillMaxWidth()) {
        items.fastForEach { item ->
            EarnListItem(item = item)
        }
    }
}

private const val PLACEHOLDER_ITEM_COUNT = 5