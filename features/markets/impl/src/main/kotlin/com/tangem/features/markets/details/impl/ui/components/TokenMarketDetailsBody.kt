package com.tangem.features.markets.details.impl.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.details.impl.ui.state.MarketsTokenDetailsUM
import com.tangem.features.markets.tokenlist.impl.ui.components.UnableToLoadData

internal fun LazyListScope.tokenMarketDetailsBody(
    state: MarketsTokenDetailsUM.Body,
    portfolioBlock: @Composable ((Modifier) -> Unit)?,
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

private fun LazyListScope.description(description: MarketsTokenDetailsUM.Description) {
    item("description") {
        Description(
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
        InsightsBlockPlaceholder(modifier = Modifier.blockPaddings())
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

@Composable
private fun Modifier.blockPaddings(): Modifier {
    return this.padding(
        start = TangemTheme.dimens.spacing16,
        end = TangemTheme.dimens.spacing16,
        bottom = TangemTheme.dimens.spacing12,
    )
}