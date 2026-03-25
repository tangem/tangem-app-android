package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.animation.*
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.row.header.TangemHeaderRow
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.internal.TangemRowTailUM
import com.tangem.core.ui.ds.row.token.TangemTokenRow
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.row.token.internal.TokenRowTitle
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.core.ui.utils.ProvideSharedTransitionScope
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListUM
import kotlinx.collections.immutable.ImmutableList

internal const val NON_CONTENT_TOKENS_LIST_KEY = "NON_CONTENT_TOKENS_LIST"

/**
 * LazyList extension for [WalletTokensListState]
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.tokensListItems(
    state: WalletTokensListState,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean,
) {
    when (state) {
        is WalletTokensListState.ContentState.PortfolioContent -> portfolioContentItems(
            items = state.items,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
        )
        is WalletTokensListState.ContentState.Content,
        is WalletTokensListState.ContentState.Loading,
        is WalletTokensListState.ContentState.Locked,
        -> contentItems(
            items = state.items,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
        )
        WalletTokensListState.Empty -> nonContentItem(modifier = modifier)
    }
}

/**
 * LazyList extension for [WalletTokensListState]
 *
 * @param walletTokensListUM state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.tokensListItems2(
    walletTokensListUM: WalletTokensListUM,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean,
) {
    when (walletTokensListUM) {
        is WalletTokensListUM.Loading,
        is WalletTokensListUM.Content,
        -> {
            walletTokensListUM.tokenList.fastForEachIndexed { index, listItem ->
                when (listItem) {
                    is TokensListItemUM2.GroupTitle,
                    is TokensListItemUM2.Token,
                    -> tokenItem(
                        listItem = listItem,
                        index = index,
                        lastIndex = walletTokensListUM.tokenList.lastIndex,
                        isBalanceHidden = isBalanceHidden,
                        modifier = modifier,
                    )
                    is TokensListItemUM2.Portfolio -> portfolioItem(
                        listItem = listItem,
                        index = index,
                        isBalanceHidden = isBalanceHidden,
                        modifier = modifier,
                    )
                }
            }
        }
        WalletTokensListUM.Empty -> nonContentItem(modifier = modifier)
    }
}

private fun LazyListScope.tokenItem(
    listItem: TokensListItemUM2,
    index: Int,
    lastIndex: Int,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    item(
        key = listItem.tokenRowUM.id,
        contentType = listItem.tokenRowUM::class.java,
    ) {
        val itemModifier = modifier
            .testTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            .semantics { lazyListItemPosition = index }
            .padding(top = if (index == 0) TangemTheme.dimens2.x3 else 0.dp)
            .roundedShapeItemDecoration(
                radius = 18.dp,
                currentIndex = index,
                addDefaultPadding = false,
                lastIndex = lastIndex,
                backgroundColor = TangemTheme.colors2.surface.level3,
            )

        when (val tokenRowUM = listItem.tokenRowUM) {
            is TangemTokenRowUM -> TangemTokenRow(
                tokenRowUM = tokenRowUM,
                isBalanceHidden = isBalanceHidden,
                reorderableState = null,
                modifier = itemModifier,
            )
            is TangemHeaderRowUM -> TangemHeaderRow(
                headerRowUM = tokenRowUM,
                modifier = itemModifier,
            )
        }
    }
}

private fun LazyListScope.portfolioItem(
    listItem: TokensListItemUM2.Portfolio,
    index: Int,
    isBalanceHidden: Boolean,
    modifier: Modifier,
) {
    val lastIndex = listItem.tokenList.lastIndex + 1

    accountItem(
        listItem = listItem,
        modifier = modifier,
        index = index,
        lastIndex = lastIndex,
        isBalanceHidden = isBalanceHidden,
    )
    itemsIndexed(
        items = listItem.tokenList,
        key = { _, item -> item.tokenRowUM.id },
        contentType = { _, item -> item::class.java },
        itemContent = { tokenIndex, item ->
            SlideInItemVisibility(
                currentIndex = tokenIndex + 1,
                lastIndex = lastIndex,
                modifier = modifier
                    .animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
                    .roundedShapeItemDecoration(
                        radius = 18.dp,
                        currentIndex = tokenIndex + 1,
                        addDefaultPadding = false,
                        lastIndex = lastIndex,
                        backgroundColor = TangemTheme.colors2.surface.level3,
                    ),
                visible = listItem.isExpanded,
            ) {
                val itemModifier = Modifier
                    .testTag(MainScreenTestTags.TOKEN_LIST_ITEM)
                    .semantics { lazyListItemPosition = tokenIndex + 1 }

                when (val tokenRowUM = item.tokenRowUM) {
                    is TangemTokenRowUM -> TangemTokenRow(
                        tokenRowUM = tokenRowUM,
                        isBalanceHidden = isBalanceHidden,
                        reorderableState = null,
                        modifier = itemModifier,
                    )
                    is TangemHeaderRowUM -> TangemHeaderRow(
                        headerRowUM = tokenRowUM,
                        isBalanceHidden = isBalanceHidden,
                        modifier = itemModifier,
                    )
                }
            }
        },
    )
}

private fun LazyListScope.accountItem(
    listItem: TokensListItemUM2.Portfolio,
    modifier: Modifier,
    index: Int,
    lastIndex: Int,
    isBalanceHidden: Boolean,
) {
    item(
        key = listItem.tokenRowUM.id,
        contentType = listItem.tokenRowUM::class.java,
    ) {
        val portfolioModifier = modifier
            .padding(top = if (index != 0) TangemTheme.dimens2.x2 else TangemTheme.dimens2.x3)
            .testTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            .semantics { lazyListItemPosition = index }
            .roundedShapeItemDecoration(
                currentIndex = 0,
                radius = 18.dp,
                addDefaultPadding = false,
                lastIndex = if (listItem.isExpanded) lastIndex else 0,
                backgroundColor = TangemTheme.colors2.surface.level3,
            )
        if (listItem.isCollapsable) {
            PortfolioRowItem(
                item = listItem,
                isBalanceHidden = isBalanceHidden,
                modifier = portfolioModifier,
            )
        } else {
            TangemHeaderRow(
                title = (listItem.tokenRowUM.titleUM as? TangemTokenRowUM.TitleUM.Content)?.text.orEmpty(),
                subtitle = (listItem.tokenRowUM.topEndContentUM as? TangemTokenRowUM.EndContentUM.Content)
                    ?.text?.orMaskWithStars(isBalanceHidden),
                headTangemIconUM = listItem.tokenRowUM.headIconUM,
                modifier = portfolioModifier,
            )
        }
    }
}

private fun LazyListScope.contentItems(
    items: ImmutableList<TokensListItemUM>,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean,
) {
    itemsIndexed(
        items = items,
        key = { _, item -> item.id },
        contentType = { _, item -> item::class.java },
        itemContent = { index, item ->
            TokenListItem(
                state = item,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = items.lastIndex,
                        backgroundColor = TangemTheme.colors.background.primary,
                        radius = 18.dp,
                    )
                    .testTag(MainScreenTestTags.TOKEN_LIST_ITEM)
                    .semantics { lazyListItemPosition = index },
            )
        },
    )
}

@Suppress("MagicNumber", "ReusedModifierInstance", "LongMethod")
@Composable
internal fun PortfolioRowItem(
    item: TokensListItemUM2.Portfolio,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    // TangemSharedTransitionLayout {
    ProvideSharedTransitionScope(modifier) {
        val iconSharedContentState = rememberSharedContentState(key = "icon")
        val titleSharedContentState = rememberSharedContentState(key = "title")
        val boundsTransform = BoundsTransform { _, _ -> tween(250) }

        AnimatedContent(
            item.isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(350, delayMillis = 90))
                    .togetherWith(fadeOut(animationSpec = tween(350)))
            },
        ) { isExpandedWrapped ->
            val animatedContentScope = this

            val composables = remember {
                SharedTokenRowComposables(
                    icon = { modifier ->
                        val size = if (isExpandedWrapped) AccountIconSize.ExtraSmall else AccountIconSize.Default
                        val currencyIconState =
                            when (val currencyIconState = item.tokenRowUM.headIconUM.currencyIconState) {
                                is CurrencyIconState.CryptoPortfolio.Icon ->
                                    currencyIconState.copy(size = size)
                                is CurrencyIconState.CryptoPortfolio.Letter ->
                                    currencyIconState.copy(size = size)
                                else -> currencyIconState
                            }

                        TangemIcon(
                            tangemIconUM = item.tokenRowUM.headIconUM.copy(currencyIconState = currencyIconState),
                            modifier = modifier.sharedBounds(
                                sharedContentState = iconSharedContentState,
                                animatedVisibilityScope = animatedContentScope,
                                boundsTransform = boundsTransform,
                            ),
                        )
                    },
                    title = { modifier ->
                        val targetAnimationFraction = if (isExpandedWrapped) 0f else 1f

                        val animationFraction = animateFloatAsState(
                            targetValue = targetAnimationFraction,
                            animationSpec = tween(durationMillis = 350),
                        )

                        val startStyle = TangemTheme.typography2.captionSemibold12
                        val stopStyle = TangemTheme.typography2.bodySemibold16

                        val textStyle by remember(animationFraction.value) {
                            derivedStateOf { lerp(startStyle, stopStyle, animationFraction.value) }
                        }

                        val resizedTitle = when (val titleUM = item.tokenRowUM.titleUM) {
                            is TangemTokenRowUM.TitleUM.Content -> titleUM.copy(
                                text = styledStringReference(
                                    titleUM.text.resolveReference(),
                                    { textStyle.toSpanStyle() },
                                ),
                            )
                            else -> titleUM
                        }

                        TokenRowTitle(
                            titleUM = resizedTitle,
                            modifier = modifier.sharedBounds(
                                sharedContentState = titleSharedContentState,
                                animatedVisibilityScope = animatedContentScope,
                                boundsTransform = boundsTransform,
                                resizeMode = scaleToBounds(ContentScale.Fit, Alignment.CenterStart),
                            ),
                        )
                    },
                )
            }

            if (isExpandedWrapped) {
                TangemHeaderRow(
                    subtitle = (item.tokenRowUM.topEndContentUM as? TangemTokenRowUM.EndContentUM.Content)
                        ?.text?.orMaskWithStars(isBalanceHidden),
                    titleContent = composables.title,
                    headContent = composables.icon,
                    tailUM = TangemRowTailUM.Icon(R.drawable.ic_minimize_24),
                    onItemClick = item.tokenRowUM.onItemClick,
                )
            } else {
                TangemTokenRow(
                    tokenRowUM = item.tokenRowUM,
                    headComponent = composables.icon,
                    titleComponent = composables.title,
                    isBalanceHidden = isBalanceHidden,
                    reorderableState = null,
                )
            }
        }
        // }
    }
}

@Stable
class SharedTokenRowComposables(
    val title: @Composable (Modifier) -> Unit,
    val icon: @Composable (Modifier) -> Unit,
)

private fun LazyListScope.nonContentItem(modifier: Modifier = Modifier) {
    item(
        key = NON_CONTENT_TOKENS_LIST_KEY,
        contentType = NON_CONTENT_TOKENS_LIST_KEY,
    ) {
        NonContentItemContent(
            modifier = modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .padding(top = TangemTheme.dimens.spacing96),
        )
    }
}

@Composable
internal fun NonContentItemContent(modifier: Modifier = Modifier) {
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