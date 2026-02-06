package com.tangem.core.ui.components.tokenlist

import androidx.compose.animation.*
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.ProvideSharedTransitionScope

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
    ProvideSharedTransitionScope(modifier) {
        val iconSharedContentState = rememberSharedContentState(key = "icon_${state.id}")
        val titleSharedContentState = rememberSharedContentState(key = "title_${state.id}")
        val boundsTransform = BoundsTransform { _, _ -> tween(250) }

        AnimatedContent(
            targetState = state.isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(250, delayMillis = 90))
                    .togetherWith(fadeOut(animationSpec = tween(250)))
            },
        ) { isExpanded ->
            val animatedContentScope = this

            val composables = TokenItemComposables(
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
                        animationSpec = tween(250),
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

            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                if (isExpanded) {
                    ExpandedPortfolioHeader(
                        state = state.tokenItemUM,
                        isCollapsable = state.isCollapsable,
                        isBalanceHidden = isBalanceHidden,
                        composables = composables,
                        balanceTextModifier = Modifier
                            .animateEnterExit(
                                enter = fadeIn(tween(250)) +
                                    slideInHorizontally(
                                        tween(250),
                                        initialOffsetX = { fullWidth -> fullWidth },
                                    ),
                                exit = fadeOut(tween(250)) +
                                    slideOutHorizontally(
                                        tween(250),
                                        targetOffsetX = { fullWidth -> fullWidth },
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
    isBalanceHidden: Boolean,
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
                top = 12.dp,
                bottom = 4.dp,
                start = 12.dp,
                end = 12.dp,
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
                composables.title.invoke(
                    Modifier
                        .padding(vertical = 2.dp)
                        .alignByBaseline(),
                )
            } else {
                when (val titleState = state.titleState) {
                    TokenItemState.TitleState.Loading -> Unit
                    TokenItemState.TitleState.Locked -> Unit
                    is TokenItemState.TitleState.Content -> Text(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .alignByBaseline(),
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
                text = text.orEmpty().orMaskWithStars(isBalanceHidden),
                modifier = balanceTextModifier
                    .alignByBaseline()
                    .padding(horizontal = 4.dp),
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