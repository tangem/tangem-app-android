package com.tangem.features.feed.ui.news.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.markets.MarketsListItem
import com.tangem.common.ui.markets.MarketsListItemPlaceholder
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.model.news.details.NewsDetailsModel.Companion.RELATED_TOKEN_MAX_COUNT
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM

@Composable
internal fun RelatedTokensBlock(
    relatedTokensUM: RelatedTokensUM,
    onItemClick: ((MarketsListItemUM) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (LocalRedesignEnabled.current) {
        RelatedTokensBlockV2(
            relatedTokensUM = relatedTokensUM,
            onItemClick = onItemClick,
            modifier = modifier,
        )
    } else {
        RelatedTokensBlockV1(
            relatedTokensUM = relatedTokensUM,
            onItemClick = onItemClick,
            modifier = modifier,
        )
    }
}

@Composable
internal fun RelatedTokensBlockV1(
    relatedTokensUM: RelatedTokensUM,
    onItemClick: ((MarketsListItemUM) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isVisible = remember(relatedTokensUM) {
        when (relatedTokensUM) {
            is RelatedTokensUM.Content -> relatedTokensUM.items.isNotEmpty()
            RelatedTokensUM.Loading -> true
            RelatedTokensUM.LoadingError -> false
        }
    }

    if (!isVisible) return

    Column(modifier = modifier) {
        SpacerH(40.dp)
        Text(
            text = stringResourceSafe(R.string.news_related_tokens),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH(12.dp)

        BlockCard(
            colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (relatedTokensUM) {
                    is RelatedTokensUM.Content -> {
                        relatedTokensUM.items.fastForEach { marketsListItemUM ->
                            MarketsListItem(
                                model = marketsListItemUM,
                                onClick = { onItemClick?.invoke(marketsListItemUM) },
                            )
                        }
                    }
                    RelatedTokensUM.Loading -> {
                        repeat(RELATED_TOKEN_MAX_COUNT) {
                            MarketsListItemPlaceholder()
                        }
                    }
                    RelatedTokensUM.LoadingError -> Unit
                }
            }
        }
    }
}

@Composable
internal fun RelatedTokensBlockV2(
    relatedTokensUM: RelatedTokensUM,
    onItemClick: ((MarketsListItemUM) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isVisible = remember(relatedTokensUM) {
        when (relatedTokensUM) {
            is RelatedTokensUM.Content -> relatedTokensUM.items.isNotEmpty()
            RelatedTokensUM.Loading -> true
            RelatedTokensUM.LoadingError -> false
        }
    }

    if (!isVisible) return

    Column(modifier = modifier) {
        SpacerH(TangemTheme.dimens2.x6)
        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens2.x2)
                .padding(top = TangemTheme.dimens2.x4, bottom = TangemTheme.dimens2.x2),
            text = stringResourceSafe(R.string.news_related_tokens),
            style = TangemTheme.typography2.headingSemibold20,
            color = TangemTheme.colors2.text.neutral.primary,
        )
        SpacerH(TangemTheme.dimens2.x3)

        Column(modifier = Modifier.fillMaxWidth()) {
            when (relatedTokensUM) {
                is RelatedTokensUM.Content -> {
                    relatedTokensUM.items.fastForEach { marketsListItemUM ->
                        WithDecorated {
                            MarketsListItem(
                                model = marketsListItemUM,
                                onClick = { onItemClick?.invoke(marketsListItemUM) },
                            )
                        }
                        SpacerH(TangemTheme.dimens2.x2)
                    }
                }
                RelatedTokensUM.Loading -> {
                    repeat(RELATED_TOKEN_MAX_COUNT) {
                        WithDecorated {
                            MarketsListItemPlaceholder()
                        }
                        SpacerH(TangemTheme.dimens2.x2)
                    }
                }
                RelatedTokensUM.LoadingError -> Unit
            }
        }
    }
}

@Composable
private fun WithDecorated(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            ),
        content = content,
    )
}