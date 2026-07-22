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
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.model.news.details.NewsDetailsModel.Companion.RELATED_TOKEN_MAX_COUNT
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM

@Composable
internal fun RelatedTokensBlock(
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
        SpacerH(24.dp)
        Text(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            text = stringResourceSafe(R.string.news_related_tokens),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        SpacerH(12.dp)

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
                        SpacerH(8.dp)
                    }
                }
                RelatedTokensUM.Loading -> {
                    repeat(RELATED_TOKEN_MAX_COUNT) {
                        WithDecorated {
                            MarketsListItemPlaceholder()
                        }
                        SpacerH(8.dp)
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
                color = TangemTheme.colors3.bg.secondary,
                shape = RoundedCornerShape(20.dp),
            ),
        content = content,
    )
}