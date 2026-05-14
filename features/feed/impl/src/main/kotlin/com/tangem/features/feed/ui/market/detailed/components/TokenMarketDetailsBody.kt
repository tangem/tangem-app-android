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
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.feed.components.NewsSlider
import com.tangem.features.feed.ui.feed.state.NewsSliderCallbacks
import com.tangem.features.feed.ui.feed.state.NewsSliderConfig
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM
import com.tangem.features.feed.ui.market.detailed.state.MarketsTokenDetailsUM.RelatedNews

@Suppress("CanBeNonNullable")
internal fun LazyListScope.tokenMarketDetailsBody(
    isRedesignEnabled: Boolean,
    state: MarketsTokenDetailsUM.Body,
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
    relatedNews: RelatedNews,
) {
    if (isRedesignEnabled) {
        tokenMarketDetailsBodyV2(
            state = state,
            relatedNews = relatedNews,
        )
    } else {
        tokenMarketDetailsBodyV1(
            state = state,
            portfolioBlock = portfolioBlock,
            relatedNews = relatedNews,
        )
    }
}

@Suppress("CanBeNonNullable")
private fun LazyListScope.tokenMarketDetailsBodyV1(
    state: MarketsTokenDetailsUM.Body,
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

            aboutCoinHeader()

            loadingInfoBlocks(false)
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
            } else {
                sectionStub(RelatedNews.SECTION_KEY)
            }

            aboutCoinHeader()

            infoBlocksListV1(state = state.infoBlocks)
        }
        is MarketsTokenDetailsUM.Body.Error -> {
            error(state)
        }
        MarketsTokenDetailsUM.Body.Nothing -> {
            // Do nothing
        }
    }
}

// Empty item with a key so that deeplink scroll-to-section can target it before the real content is composed
private fun LazyListScope.sectionStub(key: String) {
    item(key) { }
}

@Suppress("CanBeNonNullable")
private fun LazyListScope.tokenMarketDetailsBodyV2(state: MarketsTokenDetailsUM.Body, relatedNews: RelatedNews) {
    when (state) {
        MarketsTokenDetailsUM.Body.Loading -> {
            item("description-loading") {
                DescriptionPlaceholder(modifier = Modifier.blockPaddings())
            }

            loadingInfoBlocks(true)
        }
        is MarketsTokenDetailsUM.Body.Content -> {
            if (state.description != null) {
                description(state.description)
            }

            infoBlocksListV2(
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
            modifier = Modifier
                .conditionalCompose(
                    condition = LocalRedesignEnabled.current,
                    modifier = {
                        this
                            .padding(horizontal = TangemTheme.dimens2.x4)
                            .padding(bottom = TangemTheme.dimens2.x8)
                    },
                    otherModifier = {
                        blockPaddings()
                    },
                ),
            description = description.shortDescription,
            hasFullDescription = description.fullDescription != null,
            onReadMoreClick = description.onReadMoreClick,
        )
    }
}

internal fun LazyListScope.infoBlocksListV1(state: MarketsTokenDetailsUM.InformationBlocks) {
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

internal fun LazyListScope.infoBlocksListV2(state: MarketsTokenDetailsUM.InformationBlocks, relatedNews: RelatedNews) {
    if (state.metrics != null) {
        item("metrics") {
            MetricsBlock(
                modifier = Modifier.blockPaddings(),
                state = state.metrics,
            )
        }
    }

    if (state.insights != null) {
        item("insights") {
            InsightsBlock(
                modifier = Modifier.blockPaddings(),
                state = state.insights,
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

    if (relatedNews.articles.isNotEmpty()) {
        relatedNewsV2(relatedNews)
    } else {
        sectionStub(RelatedNews.SECTION_KEY)
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

private fun LazyListScope.loadingInfoBlocks(isRedesignEnabled: Boolean) {
    if (isRedesignEnabled) {
        loadingInfoBlocksV2()
    } else {
        loadingInfoBlocksV1()
    }
}

private fun LazyListScope.loadingInfoBlocksV1() {
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

private fun LazyListScope.loadingInfoBlocksV2() {
    item("metrics-loading") {
        MetricsBlockPlaceholder(modifier = Modifier.blockPaddings())
    }

    item("insights-loading") {
        InsightsBlockPlaceholder(modifier = Modifier.blockPaddings())
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

private fun LazyListScope.relatedNewsV2(relatedNews: RelatedNews) {
    item(RelatedNews.SECTION_KEY) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TangemTheme.dimens2.x8)
                .onFirstVisible(
                    minFractionVisible = 0.5f,
                    callback = relatedNews.onFirstVisible,
                ),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3),
        ) {
            Text(
                modifier = Modifier.padding(start = TangemTheme.dimens2.x6),
                text = stringResourceSafe(R.string.news_related_news),
                style = TangemTheme.typography2.headingSemibold20,
                color = TangemTheme.colors2.text.neutral.primary,
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
    return if (LocalRedesignEnabled.current) {
        this
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = TangemTheme.dimens2.x2)
    } else {
        this.padding(
            start = TangemTheme.dimens.spacing16,
            end = TangemTheme.dimens.spacing16,
            bottom = TangemTheme.dimens.spacing12,
        )
    }
}