package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
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

internal fun LazyListScope.portfolioContentItems(
    items: ImmutableList<TokensListItemUM.Portfolio>,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean,
    portfolioVisibleState: (portfolio: TokensListItemUM.Portfolio) -> MutableTransitionState<Boolean>,
) {
    items.forEachIndexed { index, item ->
        portfolioTokensList(
            portfolio = item,
            modifier = modifier,
            portfolioIndex = index,
            isBalanceHidden = isBalanceHidden,
            portfolioVisibleState = portfolioVisibleState,
        )
    }
}

internal fun LazyListScope.portfolioTokensList(
    portfolio: TokensListItemUM.Portfolio,
    modifier: Modifier,
    portfolioIndex: Int,
    isBalanceHidden: Boolean,
    portfolioVisibleState: (portfolio: TokensListItemUM.Portfolio) -> MutableTransitionState<Boolean>,
) {
    val tokens = portfolio.tokens
    val isExpanded = portfolio.isExpanded

    portfolioItem(
        portfolio = portfolio,
        modifier = modifier,
        portfolioIndex = portfolioIndex,
        isBalanceHidden = isBalanceHidden,
        portfolioVisibleState = portfolioVisibleState,
    )
    if (!isExpanded) return
    if (tokens.isEmpty()) {
        item(
            key = "$NON_CONTENT_TOKENS_LIST_KEY account-${portfolio.id}",
            contentType = "$NON_CONTENT_TOKENS_LIST_KEY account-${portfolio.id}",
        ) {
            val appear = portfolioVisibleState(portfolio)
            SlideInItemVisibility(
                modifier = modifier
                    .animateItem()
                    .roundedShapeItemDecoration(
                        radius = TangemTheme.dimens.radius14,
                        currentIndex = 1,
                        lastIndex = 1,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
                visibleState = appear,
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
        key = { _, item -> item.id },
        contentType = { _, item -> item::class.java },
        itemContent = { tokenIndex, token ->
            val indexWithHeader = tokenIndex.inc()
            val lastIndex = tokens.lastIndex.inc()
            val appear = portfolioVisibleState(portfolio)
            SlideInItemVisibility(
                modifier = modifier
                    .testModifier(indexWithHeader)
                    .animateItem()
                    .roundedShapeItemDecoration(
                        radius = TangemTheme.dimens.radius14,
                        currentIndex = indexWithHeader,
                        lastIndex = lastIndex,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
                visibleState = appear,
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

private fun LazyListScope.portfolioItem(
    portfolio: TokensListItemUM.Portfolio,
    modifier: Modifier,
    portfolioIndex: Int,
    isBalanceHidden: Boolean,
    portfolioVisibleState: (portfolio: TokensListItemUM.Portfolio) -> MutableTransitionState<Boolean>,
) {
    val tokens = portfolio.tokens
    val isExpanded = portfolio.isExpanded

    val lastIndex = when {
        isExpanded && tokens.isEmpty() -> 1
        isExpanded -> tokens.lastIndex.inc()
        else -> 0
    }

    item(
        key = "account-${portfolio.id}-isExpanded$isExpanded",
        contentType = "account-isExpanded$isExpanded",
    ) {
        val anchorModifier = modifier
            .testModifier(portfolioIndex)
            .animateItem()
            .roundedShapeItemDecoration(
                currentIndex = 0,
                radius = TangemTheme.dimens.radius14,
                lastIndex = lastIndex,
                backgroundColor = TangemTheme.colors.background.primary,
            )
        val appear = portfolioVisibleState(portfolio)
        if (isExpanded) {
            SlideInItemVisibility(
                modifier = anchorModifier,
                visibleState = appear,
            ) {
                PortfolioListItem(
                    state = portfolio,
                    isBalanceHidden = isBalanceHidden,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            AnimatedVisibility(
                modifier = anchorModifier,
                visibleState = appear,
                enter = fadeIn(),
                exit = ExitTransition.None,
            ) {
                PortfolioListItem(
                    state = portfolio,
                    isBalanceHidden = isBalanceHidden,
                )
            }
        }
    }
}

@Composable
private fun SlideInItemVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    content: @Composable() AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier,
        visibleState = visibleState,
        enter = slideInVertically(
            animationSpec = tween(easing = LinearOutSlowInEasing),
            initialOffsetY = { it },
        ) + fadeIn(),
        exit = ExitTransition.None,
    ) {
        content()
    }
}

private fun Modifier.testModifier(index: Int): Modifier = this
    .testTag(MainScreenTestTags.TOKEN_LIST_ITEM)
    .semantics { lazyListItemPosition = index }