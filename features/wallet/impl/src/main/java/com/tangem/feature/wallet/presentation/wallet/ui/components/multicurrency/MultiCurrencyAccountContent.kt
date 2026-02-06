package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.tokenlist.PortfolioListItem
import com.tangem.core.ui.components.tokenlist.PortfolioTokensListItem
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.core.ui.utils.lazyListItemPosition
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay

internal fun LazyListScope.portfolioContentItems(
    items: ImmutableList<TokensListItemUM.Portfolio>,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean,
) {
    items.forEachIndexed { index, item ->
        portfolioTokensList(
            portfolio = item,
            modifier = modifier,
            portfolioIndex = index,
            isBalanceHidden = isBalanceHidden,
        )
    }
}

internal fun LazyListScope.portfolioTokensList(
    portfolio: TokensListItemUM.Portfolio,
    modifier: Modifier,
    portfolioIndex: Int,
    isBalanceHidden: Boolean,
) {
    val tokens = portfolio.tokens
    val isExpanded = portfolio.isExpanded
    val lastIndex = tokens.lastIndex.inc()

    portfolioItem(
        portfolio = portfolio,
        modifier = modifier,
        portfolioIndex = portfolioIndex,
        isBalanceHidden = isBalanceHidden,
    )
    if (tokens.isEmpty()) {
        item(
            key = "$NON_CONTENT_TOKENS_LIST_KEY account-${portfolio.id}",
            contentType = "$NON_CONTENT_TOKENS_LIST_KEY account-${portfolio.id}",
        ) {
            SlideInItemVisibility(
                currentIndex = 1,
                lastIndex = lastIndex,
                modifier = modifier
                    .animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
                    .roundedShapeItemDecoration(
                        radius = TangemTheme.dimens.radius14,
                        currentIndex = 1,
                        lastIndex = 1,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
                visible = isExpanded,
            ) {
                NonContentItemContent(
                    modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing28),
                )
            }
        }
        return
    }
    itemsIndexed(
        items = tokens,
        key = { _, item -> item.id.toString() + "-portfolio-${portfolio.id}" },
        contentType = { _, item -> item::class.java },
        itemContent = { tokenIndex, token ->
            val indexWithHeader = tokenIndex.inc()
            SlideInItemVisibility(
                currentIndex = tokenIndex,
                lastIndex = lastIndex,
                modifier = modifier
                    .testModifier(indexWithHeader)
                    .animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
                    .roundedShapeItemDecoration(
                        radius = TangemTheme.dimens.radius14,
                        currentIndex = indexWithHeader,
                        lastIndex = lastIndex,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
                visible = isExpanded,
            ) {
                val modifier = if (indexWithHeader == lastIndex) Modifier.padding(bottom = 8.dp) else Modifier
                PortfolioTokensListItem(
                    state = token,
                    isBalanceHidden = isBalanceHidden,
                    modifier = modifier,
                )
            }
        },
    )
}

@Suppress("MagicNumber")
private fun LazyListScope.portfolioItem(
    portfolio: TokensListItemUM.Portfolio,
    modifier: Modifier,
    portfolioIndex: Int,
    isBalanceHidden: Boolean,
) {
    val tokens = portfolio.tokens
    val isExpanded = portfolio.isExpanded
    val lastIndex = when {
        isExpanded && tokens.isEmpty() -> 1
        isExpanded -> tokens.lastIndex.inc()
        else -> 0
    }

    item(
        key = "account-${portfolio.id}",
        contentType = "account-content",
    ) {
        var lastIndexProxy by remember { mutableIntStateOf(lastIndex) }

        // When collapsing the portfolio, we delay updating lastIndexProxy to allow
        // shrinking animation to complete before changing the shape.
        LaunchedEffect(lastIndex) {
            if (lastIndex != 0) {
                lastIndexProxy = lastIndex
                return@LaunchedEffect
            }
            delay(timeMillis = minOf(50 * tokens.size, 250).toLong())
            lastIndexProxy = 0
        }

        PortfolioListItem(
            state = portfolio,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier
                .testModifier(portfolioIndex)
                .roundedShapeItemDecoration(
                    currentIndex = 0,
                    radius = TangemTheme.dimens.radius14,
                    lastIndex = lastIndexProxy,
                    backgroundColor = TangemTheme.colors.background.primary,
                ),
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun SlideInItemVisibility(
    visible: Boolean,
    currentIndex: Int,
    lastIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val maxDelay = 250
    val delayEnter = minOf(50 * currentIndex, maxDelay)
    val delayExit = minOf(50 * (lastIndex - currentIndex - 1), maxDelay)

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(
            tween(100, delayMillis = delayEnter),
        ) + expandVertically(
            tween(100, delayMillis = delayEnter, easing = FastOutLinearInEasing),
            expandFrom = Alignment.Top,
        ),
        exit = fadeOut(
            tween(100, delayMillis = delayExit),
        ) + shrinkVertically(
            tween(100, delayMillis = delayExit, easing = FastOutLinearInEasing),
            shrinkTowards = Alignment.Top,
        ),
    ) {
        content()
    }
}

private fun Modifier.testModifier(index: Int): Modifier = this
    .testTag(MainScreenTestTags.TOKEN_LIST_ITEM)
    .semantics { lazyListItemPosition = index }