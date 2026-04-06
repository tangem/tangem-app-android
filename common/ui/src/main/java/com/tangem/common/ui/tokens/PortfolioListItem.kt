package com.tangem.common.ui.tokens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.tokenlist.NON_CONTENT_TOKENS_LIST_KEY
import com.tangem.core.ui.components.tokenlist.PortfolioListItem
import com.tangem.core.ui.components.tokenlist.PortfolioTokensListItem
import com.tangem.core.ui.components.tokenlist.state.PortfolioItemContentUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.lazyListItemPosition

fun LazyListScope.portfolioTokensList(
    portfolio: TokensListItemUM.Portfolio,
    portfolioIndex: Int,
    testTag: String,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = portfolio.tokens
    val isExpanded = portfolio.isExpanded
    val lastIndex = tokens.lastIndex.inc()

    portfolioItem(
        portfolio = portfolio,
        modifier = modifier.testModifier(portfolioIndex, testTag),
        isBalanceHidden = isBalanceHidden,
    )
    val portfolioContent = portfolio.content
    if (portfolioContent is PortfolioItemContentUM.Empty) {
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
                EmptyAccountContent(portfolioContent)
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
                    .testModifier(indexWithHeader, testTag)
                    .animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
                    .roundedShapeItemDecoration(
                        radius = TangemTheme.dimens.radius14,
                        currentIndex = indexWithHeader,
                        lastIndex = lastIndex,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
                visible = isExpanded,
            ) {
                PortfolioTokensListItem(
                    state = token,
                    isBalanceHidden = isBalanceHidden,
                    modifier = Modifier
                        .conditional(indexWithHeader == lastIndex) { padding(bottom = 8.dp) },
                )
            }
        },
    )
}

@Suppress("MagicNumber")
fun LazyListScope.portfolioItem(portfolio: TokensListItemUM.Portfolio, modifier: Modifier, isBalanceHidden: Boolean) {
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
        // Snap immediately on expand; on collapse, hold until all child items finish
        // their shrink animation, then snap to fully-rounded shape.
        val effectiveLastIndex by animateIntAsState(
            targetValue = lastIndex,
            animationSpec = if (lastIndex != 0) {
                snap()
            } else {
                snap(delayMillis = minOf(50 * tokens.lastIndex, 250) + 150)
            },
            label = "lastIndex",
        )

        PortfolioListItem(
            state = portfolio,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier
                .roundedShapeItemDecoration(
                    currentIndex = 0,
                    radius = TangemTheme.dimens.radius14,
                    lastIndex = effectiveLastIndex,
                    backgroundColor = TangemTheme.colors.background.primary,
                ),
        )
    }
}

@Suppress("MagicNumber")
@Composable
fun SlideInItemVisibility(
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
        enter = expandVertically(
            tween(200, delayMillis = delayEnter, easing = LinearOutSlowInEasing),
            expandFrom = Alignment.Top,
        ) + fadeIn(tween(200, delayMillis = delayEnter, easing = LinearOutSlowInEasing)),
        exit = shrinkVertically(
            tween(150, delayMillis = delayExit, easing = FastOutLinearInEasing),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(tween(150, delayMillis = delayExit, easing = FastOutLinearInEasing)),
    ) {
        content()
    }
}

@Composable
private fun EmptyAccountContent(emptyConent: PortfolioItemContentUM.Empty, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpacerH16()
        NonContentItemContent()
        val emptyAction = emptyConent.action
        if (emptyAction != null) {
            SpacerH16()
            SecondarySmallButton(
                config = SmallButtonConfig(
                    text = emptyAction.text,
                    onClick = { emptyAction.onClick() },
                ),
            )
        }
        SpacerH24()
    }
}

@Composable
fun NonContentItemContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_empty_64),
            contentDescription = null,
            modifier = Modifier.size(size = TangemTheme.dimens.size64),
            tint = TangemTheme.colors.icon.inactive,
        )

        Text(
            text = stringResourceSafe(id = R.string.main_empty_tokens_list_message),
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing48),
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.caption2,
        )
    }
}

private fun Modifier.testModifier(index: Int, testTag: String): Modifier = this
    .testTag(testTag)
    .semantics { lazyListItemPosition = index }