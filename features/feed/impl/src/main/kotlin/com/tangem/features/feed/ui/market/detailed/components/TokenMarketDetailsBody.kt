package com.tangem.features.feed.ui.market.detailed.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.items.DescriptionItem
import com.tangem.core.ui.components.items.DescriptionPlaceholder
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.feed.components.NewsSlider
import com.tangem.features.feed.ui.feed.state.NewsSliderCallbacks
import com.tangem.features.feed.ui.feed.state.NewsSliderConfig
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM.RelatedNews

internal fun LazyListScope.tokenMarketDetailsBody(
    state: MarketsTokenDetailsUM.Body,
    relatedNews: RelatedNews,
    marketingBanner: @Composable (Modifier) -> Unit,
    tokenSummaryBlock: @Composable ((Modifier) -> Unit)?,
) {
    tokenMarketDetailsBodyBlock(
        state = state,
        relatedNews = relatedNews,
        marketingBanner = marketingBanner,
        tokenSummaryBlock = tokenSummaryBlock,
    )
}

private fun LazyListScope.marketingBannerItem(marketingBanner: @Composable (Modifier) -> Unit) {
    item(key = "marketing_banner") {
        marketingBanner(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
        )
    }
}

private fun LazyListScope.tokenSummaryBlockItem(tokenSummaryBlock: @Composable (Modifier) -> Unit) {
    item(key = "token_summary_block") {
        tokenSummaryBlock(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
        )
    }
}

// Empty item with a key so that deeplink scroll-to-section can target it before the real content is composed
private fun LazyListScope.sectionStub(key: String) {
    item(key) { }
}

@Suppress("CanBeNonNullable")
private fun LazyListScope.tokenMarketDetailsBodyBlock(
    state: MarketsTokenDetailsUM.Body,
    relatedNews: RelatedNews,
    marketingBanner: @Composable (Modifier) -> Unit,
    tokenSummaryBlock: @Composable ((Modifier) -> Unit)?,
) {
    when (state) {
        MarketsTokenDetailsUM.Body.Loading -> {
            item("description-loading") {
                DescriptionPlaceholder(modifier = Modifier.padding(28.dp))
            }

            // The block renders its own loading placeholder, so it is shown in both states
            if (tokenSummaryBlock != null) {
                tokenSummaryBlockItem(tokenSummaryBlock)
            }

            loadingInfoBlocks()
        }
        is MarketsTokenDetailsUM.Body.Content -> {
            if (state.description != null) {
                description(state.description)
            }

            marketingBannerItem(marketingBanner)

            if (tokenSummaryBlock != null) {
                tokenSummaryBlockItem(tokenSummaryBlock)
            }

            infoBlocksList(
                state = state.infoBlocks,
                relatedNews = relatedNews,
            )
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
                        horizontal = 16.dp,
                        vertical = 40.dp,
                    ),
                onRetryClick = state.onLoadRetryClick,
            )
        }
    }
}

private fun LazyListScope.description(description: MarketsTokenDetailsUM.Description) {
    item("description") {
        DescriptionItem(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp, top = 4.dp),
            description = description.shortDescription,
            hasFullDescription = description.fullDescription != null,
            onReadMoreClick = description.onReadMoreClick,
        )
    }
}

internal fun LazyListScope.infoBlocksList(state: MarketsTokenDetailsUM.InformationBlocks, relatedNews: RelatedNews) {
    if (relatedNews.articles.isNotEmpty()) {
        relatedNews(relatedNews)
    } else {
        sectionStub(RelatedNews.SECTION_KEY)
    }

    if (state.insights != null) {
        item("insights") {
            InsightsBlock(
                modifier = Modifier.blockPaddings(),
                state = state.insights,
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

    item(key = "listedOn") {
        ListedOnBlock(
            state = state.listedOn,
            modifier = Modifier.blockPaddings(),
        )
    }

    if (state.securityScore != null) {
        item("securityScore") {
            SecurityScoreBlock(
                modifier = Modifier.blockPaddings(),
                state = state.securityScore,
            )
        }
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
        InsightsBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item("metrics-loading") {
        MetricsBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item(key = "listedOn-loading") {
        ListedOnBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item("securityScore-loading") {
        SecurityScoreBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item("links-loading") {
        LinksBlockPlaceholder(modifier = Modifier.blockPaddings())
    }
}

private fun LazyListScope.relatedNews(relatedNews: RelatedNews) {
    item(RelatedNews.SECTION_KEY) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .onFirstVisible(
                    minFractionVisible = 0.5f,
                    callback = relatedNews.onFirstVisible,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                modifier = Modifier.padding(start = 24.dp),
                text = stringResourceSafe(R.string.news_related_news),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )

            NewsSlider(
                NewsSliderConfig(
                    callbacks = NewsSliderCallbacks(
                        onOpenAllNews = {}, // not applicable here
                        onSliderScroll = relatedNews.onScroll,
                        onSliderEndReached = {}, // not applicable here
                        onArticleClick = relatedNews.onArticledClicked,
                    ),
                    content = relatedNews.articles,
                    shouldShowSeeAllNewsItem = false,
                ),
            )
        }
    }
}

@Composable
private fun Modifier.blockPaddings(): Modifier {
    return this
        .padding(horizontal = 16.dp)
        .padding(bottom = 8.dp)
}