package com.tangem.core.ui.components.tokenlist

import androidx.compose.animation.*
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.account.AccountCharIcon
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.AccountResIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.TokenItemComposables
import com.tangem.core.ui.components.token.internal.TokenTitle
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.internal.GroupTitleItem
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Multi-currency content item
 *
 * @param state           component UI model
 * @param isBalanceHidden flag that shows/hides balance
 * @param modifier        modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TokenListItem(state: TokensListItemUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TokensListItemUM.GroupTitle -> PortfolioTokensListItem(state, isBalanceHidden, modifier)
        is TokensListItemUM.Token -> PortfolioTokensListItem(state, isBalanceHidden, modifier)
        is TokensListItemUM.Portfolio -> PortfolioListItem(state, isBalanceHidden, modifier)
        is TokensListItemUM.SearchBar -> {
            SearchBar(state = state.searchBarUM, modifier = modifier.padding(all = 12.dp))
        }
        is TokensListItemUM.Text -> {
            Text(
                text = state.text.resolveReference(),
                modifier = modifier.padding(all = 12.dp),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
            )
        }
    }
}

@Suppress("MagicNumber", "ReusedModifierInstance", "LongMethod")
@Composable
fun PortfolioListItem(state: TokensListItemUM.Portfolio, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    // Box is not needed here, but due to a bug in compose
    // we use it like a wrapper to properly handle lookahead scope inside this composable
    Box(modifier) {
        SharedTransitionLayout {
            AnimatedContent(
                state.isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350, delayMillis = 90))
                        .togetherWith(fadeOut(animationSpec = tween(350)))
                },
            ) { isExpanded ->
                val animatedContentScope = this
                val iconSharedContentState = rememberSharedContentState(key = "icon")
                val titleSharedContentState = rememberSharedContentState(key = "title")
                val boundsTransform = BoundsTransform { _, _ -> tween(350) }

                val composables = remember {
                    TokenItemComposables(
                        icon = { modifier: Modifier ->
                            val iconState = when (val icon = state.tokenItemUM.iconState) {
                                is CurrencyIconState.CryptoPortfolio.Icon ->
                                    icon.copy(
                                        size = if (isExpanded) AccountIconSize.ExtraSmall else AccountIconSize.Default,
                                    )
                                is CurrencyIconState.CryptoPortfolio.Letter ->
                                    icon.copy(
                                        size = if (isExpanded) AccountIconSize.ExtraSmall else AccountIconSize.Default,
                                    )
                                else -> icon
                            }

                            CurrencyIcon(
                                state = iconState,
                                withFixedSize = false,
                                modifier = modifier
                                    .sharedBounds(
                                        sharedContentState = iconSharedContentState,
                                        animatedVisibilityScope = animatedContentScope,
                                        boundsTransform = boundsTransform,
                                    ),
                            )
                        },
                        title = { modifier: Modifier ->
                            val textStyle = if (isExpanded) {
                                TangemTheme.typography.caption1
                            } else {
                                TangemTheme.typography.subtitle2
                            }

                            val textSize by animateFloatAsState(
                                targetValue = textStyle.fontSize.value,
                                animationSpec = tween(350),
                            )

                            TokenTitle(
                                state = state.tokenItemUM.titleState,
                                textStyle = textStyle.copy(fontSize = textSize.sp),
                                modifier = modifier
                                    .sharedBounds(
                                        sharedContentState = titleSharedContentState,
                                        animatedVisibilityScope = animatedContentScope,
                                        boundsTransform = boundsTransform,
                                        resizeMode = scaleToBounds(ContentScale.Fit, Alignment.CenterStart),
                                    ),
                            )
                        },
                    )
                }

                if (isExpanded) {
                    ExpandedPortfolioHeader(
                        state = state.tokenItemUM,
                        isCollapsable = state.isCollapsable,
                        composables = composables,
                        balanceTextModifier = Modifier
                            .animateEnterExit(
                                enter = fadeIn(tween(350)) +
                                    slideInHorizontally(
                                        tween(350),
                                        initialOffsetX = { fullWidth -> fullWidth * 2 },
                                    ),
                                exit = fadeOut(
                                    tween(350),
                                ) + slideOutHorizontally(
                                    tween(350),
                                    targetOffsetX = { fullWidth -> fullWidth * 2 },
                                ),
                            ),
                    )
                } else {
                    TokenItem(
                        state = state.tokenItemUM,
                        isBalanceHidden = isBalanceHidden,
                        composables = composables,
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioTokensListItem(state: PortfolioTokensListItemUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TokensListItemUM.GroupTitle -> GroupTitleItem(state, modifier)
        is TokensListItemUM.Token -> TokenItem(
            state = state.state,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
        )
    }
}

@Suppress("LongMethod")
@Composable
fun ExpandedPortfolioHeader(
    state: TokenItemState,
    isCollapsable: Boolean,
    composables: TokenItemComposables?,
    modifier: Modifier = Modifier,
    balanceTextModifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { state.onItemClick?.invoke(state) })
            .padding(
                vertical = if (composables != null) 8.dp else 4.dp,
                horizontal = 12.dp,
            ),
    ) {
        if (composables != null) {
            composables.icon.invoke(Modifier)
        } else {
            when (val icon = state.iconState) {
                is CurrencyIconState.CryptoPortfolio.Icon -> AccountResIcon(
                    resId = icon.resId,
                    color = icon.color,
                    size = AccountIconSize.ExtraSmall,
                )
                is CurrencyIconState.CryptoPortfolio.Letter -> AccountCharIcon(
                    char = icon.char.resolveReference().first(),
                    color = icon.color,
                    size = AccountIconSize.ExtraSmall,
                )
                is CurrencyIconState.CoinIcon,
                is CurrencyIconState.CustomTokenIcon,
                is CurrencyIconState.Empty,
                is CurrencyIconState.FiatIcon,
                CurrencyIconState.Loading,
                CurrencyIconState.Locked,
                is CurrencyIconState.TokenIcon,
                -> Unit
            }
        }

        SpacerW4()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            if (composables != null) {
                composables.title.invoke(Modifier.alignByBaseline())
            } else {
                when (val titleState = state.titleState) {
                    TokenItemState.TitleState.Loading -> Unit
                    TokenItemState.TitleState.Locked -> Unit
                    is TokenItemState.TitleState.Content -> Text(
                        text = titleState.text.resolveReference(),
                        color = TangemTheme.colors.text.primary1,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = TangemTheme.typography.caption1,
                    )
                }
            }

            val text = when (val fiatAmountState = state.fiatAmountState) {
                is TokenItemState.FiatAmountState.Content -> fiatAmountState.text
                is TokenItemState.FiatAmountState.TextContent -> fiatAmountState.text
                else -> null
            }

            Text(
                text = text.orEmpty(),
                modifier = balanceTextModifier
                    .alignByBaseline()
                    .padding(horizontal = 8.dp),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption1,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        if (isCollapsable) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size16),
                painter = painterResource(id = R.drawable.ic_minimize_24),
                tint = TangemTheme.colors.icon.inactive,
                contentDescription = null,
            )
        }
    }
}