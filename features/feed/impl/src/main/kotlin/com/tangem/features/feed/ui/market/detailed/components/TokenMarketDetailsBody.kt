package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.news.ArticleCard
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.items.DescriptionItem
import com.tangem.core.ui.components.items.DescriptionPlaceholder
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM.RelatedNews

private const val FOURTH_ITEM_INDEX = 3

@Suppress("CanBeNonNullable") // TODO will be removed after [REDACTED_JIRA]
internal fun LazyListScope.tokenMarketDetailsBody(
    state: MarketsTokenDetailsUM.Body,
    isAccountEnabled: Boolean,
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
    relatedNews: RelatedNews,
) {
    when (state) {
        MarketsTokenDetailsUM.Body.Loading -> {
            item("description-loading") {
                DescriptionPlaceholder(modifier = Modifier.blockPaddings())
            }

            if (portfolioBlock != null) {
                item(key = "portfolio") {
                    portfolioBlock(Modifier.blockPaddings())
                }
            }

            if (isAccountEnabled) {
                aboutCoinHeader()
            }

            loadingInfoBlocks()
        }
        is MarketsTokenDetailsUM.Body.Content -> {
            if (state.description != null) {
                description(state.description)
            }

            if (portfolioBlock != null) {
                item(key = "portfolio") {
                    portfolioBlock(Modifier.blockPaddings())
                }
            }

            if (relatedNews.articles.isNotEmpty()) {
                relatedNews(relatedNews)
            }

            if (isAccountEnabled) {
                aboutCoinHeader()
            }

            infoBlocksList(state.infoBlocks)
        }
        is MarketsTokenDetailsUM.Body.Error -> {
            error(state)
        }
        MarketsTokenDetailsUM.Body.Nothing -> {
            // Do nothing
        }
    }
}

private fun LazyListScope.error(state: MarketsTokenDetailsUM.Body.Error) {
    item("body-error") {
        Box(Modifier.fillMaxWidth()) {
            UnableToLoadData(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(
                        horizontal = TangemTheme.dimens.spacing16,
                        vertical = TangemTheme.dimens.spacing40,
                    ),
                onRetryClick = state.onLoadRetryClick,
            )
        }
    }
}

private fun LazyListScope.aboutCoinHeader() {
    item("aboutCoinHeader") {
        Text(
            modifier = Modifier.padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing20,
            ),
            text = stringResourceSafe(R.string.markets_about_coin_header),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.h3,
        )
    }
}

private fun LazyListScope.description(description: MarketsTokenDetailsUM.Description) {
    item("description") {
        DescriptionItem(
            modifier = Modifier.blockPaddings(),
            description = description.shortDescription,
            hasFullDescription = description.fullDescription != null,
            onReadMoreClick = description.onReadMoreClick,
        )
    }
}

internal fun LazyListScope.infoBlocksList(state: MarketsTokenDetailsUM.InformationBlocks) {
    if (state.insights != null) {
        item("insights") {
            InsightsBlock(
                modifier = Modifier.blockPaddings(),
                state = state.insights,
            )
        }
    }

    if (state.securityScore != null) {
        item("securityScore") {
            SecurityScoreBlock(
                modifier = Modifier.blockPaddings(),
                state = state.securityScore,
            )
        }
    }

    if (state.metrics != null) {
        item("metrics") {
            MetricsBlock(
                modifier = Modifier.blockPaddings(),
                state = state.metrics,
            )
        }
    }

    if (state.pricePerformance != null) {
        item("pricePerformance") {
            PricePerformanceBlock(
                modifier = Modifier.blockPaddings(),
                state = state.pricePerformance,
            )
        }
    }

    item(key = "listedOn") {
        ListedOnBlock(
            state = state.listedOn,
            modifier = Modifier.blockPaddings(),
        )
    }

    if (state.links != null) {
        item("links") {
            LinksBlock(
                modifier = Modifier.blockPaddings(),
                state = state.links,
            )
        }
    }
}

private fun LazyListScope.loadingInfoBlocks() {
    item("insights-loading") {
        InsightsBlockPlaceholder(
            modifier = Modifier.blockPaddings(),
        )
    }

    item("securityScore-loading") {
        SecurityScoreBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item("metrics-loading") {
        MetricsBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item("pricePerformance-loading") {
        PricePerformanceBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item(key = "listedOn-loading") {
        ListedOnBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item("links-loading") {
        LinksBlockPlaceholder(modifier = Modifier.blockPaddings())
    }
}

private fun LazyListScope.relatedNews(relatedNews: RelatedNews) {
    item("related-news") {
        val listState = rememberLazyListState()
        val articlesReadStatus = remember(relatedNews.articles) {
            relatedNews.articles.map { it.isViewed }
        }
        LaunchedEffect(articlesReadStatus) {
            listState.requestScrollToItem(0)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 20.dp)
                .onFirstVisible(
                    minFractionVisible = 0.5f,
                    callback = relatedNews.onFirstVisible,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResourceSafe(R.string.news_related_news),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
            )

            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                state = listState,
            ) {
                itemsIndexed(
                    items = relatedNews.articles,
                    key = { index, article -> article.id },
                ) { index, article ->
                    val articleModifier = if (index == FOURTH_ITEM_INDEX) {
                        Modifier.onFirstVisible(
                            minFractionVisible = 0.5f,
                            callback = relatedNews.onScroll,
                        )
                    } else {
                        Modifier
                    }

                    ArticleCard(
                        articleConfigUM = article,
                        onArticleClick = { relatedNews.onArticledClicked(article.id) },
                        modifier = articleModifier
                            .height(164.dp)
                            .width(216.dp),
                        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.blockPaddings(): Modifier {
    return this.padding(
        start = TangemTheme.dimens.spacing16,
        end = TangemTheme.dimens.spacing16,
        bottom = TangemTheme.dimens.spacing12,
    )
}