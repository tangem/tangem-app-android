package com.tangem.features.foryou.impl.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.common.ui.tokens.SlideInItemVisibility
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.toBoxSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.TangemCurrencyIcon
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRow
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.row.token.internal.TokenRowTitle
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.utils.ProvideSharedTransitionScope
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.core.ui.utils.sharedBoundsSafely
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ForYouPortfolioTokenList(tokenList: ImmutableList<ForYouTokenListItemUM>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val outerLastIndex = tokenList.lastIndex
        tokenList.fastForEachIndexed { index, listItem ->
            key(listItem.tokenRowUM.id) {
                PortfolioTokenItem(listItem = listItem, index = index, outerLastIndex = outerLastIndex)
            }
        }
    }
}

@Composable
private fun PortfolioTokenItem(listItem: ForYouTokenListItemUM, index: Int, outerLastIndex: Int) {
    PortfolioAssetItem(listItem = listItem, index = index, outerLastIndex = outerLastIndex)

    val lastIndex = listItem.tokenList.lastIndex.inc()
    listItem.tokenList.fastForEachIndexed { tokenIndex, item ->
        SlideInItemVisibility(
            currentIndex = tokenIndex + 1,
            lastIndex = lastIndex,
            modifier = Modifier
                .roundedShapeItemDecoration(
                    radius = 24.dp,
                    currentIndex = tokenIndex + 1,
                    addDefaultPadding = false,
                    lastIndex = lastIndex,
                    backgroundColor = TangemTheme.colors3.bg.secondary,
                ),
            visible = listItem.isExpanded,
        ) {
            val itemModifier = Modifier
                .semantics { lazyListItemPosition = tokenIndex + 1 }

            var position by remember { mutableStateOf(Offset.Zero) }
            TangemTokenRow(
                tokenRowUM = item,
                isBalanceHidden = false, // TODO For You
                reorderableState = null,
                modifier = itemModifier
                    .onGloballyPositioned {
                        position = it.positionInWindow()
                    }
                    .conditionalCompose(item.onItemClick != null) {
                        clickable(onClick = requireNotNull(item.onItemClick))
                    },
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun PortfolioAssetItem(listItem: ForYouTokenListItemUM, index: Int, outerLastIndex: Int) {
    val itemBackgroundColor = TangemTheme.colors3.bg.secondary

    ProvideSharedTransitionScope(
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { lazyListItemPosition = index }
            .roundedShapeItemDecoration(
                currentIndex = 0,
                radius = 24.dp,
                addDefaultPadding = false,
                lastIndex = portfolioAssetExpandAnimation(listItem = listItem, outerLastIndex = outerLastIndex).value,
                backgroundColor = itemBackgroundColor,
            ),
    ) {
        val iconSharedContentState = rememberSharedContentState(key = "icon_${listItem.tokenRowUM.id}")
        val titleSharedContentState = rememberSharedContentState(key = "title_${listItem.tokenRowUM.id}")
        val boundsTransform = BoundsTransform { _, _ -> tween(250) }

        AnimatedContent(
            targetState = listItem.isExpanded,
            transitionSpec = { portfolioAssetExpandFadeAnimation() },
        ) { isExpandedWrapped ->
            val composables = remember {
                SharedTokenRowComposables(
                    icon = { modifier ->
                        PortfolioSharedAssetIcon(
                            listItem = listItem,
                            isExpandedWrapped = isExpandedWrapped,
                            itemBackgroundColor = itemBackgroundColor,
                            modifier = modifier.sharedBoundsSafely(
                                sharedContentState = iconSharedContentState,
                                animatedVisibilityScope = this,
                                boundsTransform = boundsTransform,
                            ),
                        )
                    },
                    title = { modifier ->
                        PortfolioSharedAssetTitle(
                            listItem = listItem,
                            isExpandedWrapped = isExpandedWrapped,
                            modifier = modifier.sharedBoundsSafely(
                                sharedContentState = titleSharedContentState,
                                animatedVisibilityScope = this,
                                boundsTransform = boundsTransform,
                                resizeMode = scaleToBounds(ContentScale.Fit, Alignment.CenterStart),
                            ),
                        )
                    },
                )
            }

            if (isExpandedWrapped) {
                ForYouPortfolioListHeader(
                    tokenRowUM = listItem.tokenRowUM,
                    headComponent = composables.icon,
                    titleComponent = composables.title,
                )
            } else {
                TangemTokenRow(
                    tokenRowUM = listItem.tokenRowUM,
                    headComponent = composables.icon,
                    titleComponent = composables.title,
                    isBalanceHidden = false, // todo For You
                    reorderableState = null,
                )
            }
        }
    }
}

@Composable
private fun PortfolioSharedAssetIcon(
    listItem: ForYouTokenListItemUM,
    isExpandedWrapped: Boolean,
    itemBackgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val headIcon = listItem.tokenRowUM.headIconUM

    if (headIcon is TangemIconUM.Currency) {
        val size = if (isExpandedWrapped) {
            AccountIconSize.RedesignExtraSmall
        } else {
            AccountIconSize.RedesignedDefault
        }
        val currencyIconState = when (val currencyIconState = headIcon.currencyIconState) {
            is CurrencyIconState.CryptoPortfolio.Icon -> currencyIconState.copy(size = size)
            is CurrencyIconState.CryptoPortfolio.Letter -> currencyIconState.copy(size = size)
            else -> currencyIconState
        }
        TangemCurrencyIcon(
            state = currencyIconState,
            shouldDisplayNetwork = false,
            modifier = modifier
                .size(size.toBoxSize())
                // TODO For You replace with DC components
                .drawWithContent {
                    drawContent()
                    if (!isExpandedWrapped) {
                        val offset = 34.dp.toPx()
                        drawBadge(
                            color = Color.Red,
                            containerColor = itemBackgroundColor,
                            offset = Offset(
                                x = offset,
                                y = offset,
                            ),
                            size = 3.dp,
                            padding = 1.dp,
                        )
                    }
                },
        )
    }
}

@Composable
private fun PortfolioSharedAssetTitle(
    listItem: ForYouTokenListItemUM,
    isExpandedWrapped: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetAnimationFraction = if (isExpandedWrapped) 0f else 1f

    val animationFraction = animateFloatAsState(
        targetValue = targetAnimationFraction,
        animationSpec = tween(durationMillis = 350),
    )

    val startStyle = TangemTheme.typography3.subheading.medium
    val stopStyle = TangemTheme.typography3.body.medium

    val textStyle by remember(animationFraction.value) {
        derivedStateOf { lerp(startStyle, stopStyle, animationFraction.value) }
    }

    val resizedTitle = when (val titleUM = listItem.tokenRowUM.titleUM) {
        is TangemTokenRowUM.TitleUM.Content -> titleUM.copy(
            text = styledStringReference(
                titleUM.text.resolveReference(),
                { textStyle.toSpanStyle() },
            ),
        )
        else -> titleUM
    }

    TokenRowTitle(
        titleUM = if (isExpandedWrapped) {
            (resizedTitle as? TangemTokenRowUM.TitleUM.Content)?.copy(badge = null) ?: resizedTitle
        } else {
            resizedTitle
        },
        modifier = modifier,
    )
}

@Composable
private fun ForYouPortfolioListHeader(
    tokenRowUM: TangemTokenRowUM,
    headComponent: @Composable (Modifier) -> Unit,
    titleComponent: @Composable (Modifier) -> Unit,
) {
    TangemRow(
        modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        divider = true,
        onClick = tokenRowUM.onItemClick,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            headComponent(Modifier)
        },
        titleSlot = {
            titleComponent(Modifier)

            val topEndUM = tokenRowUM.topEndContentUM
            val bottomEndUM = tokenRowUM.bottomEndContentUM
            when {
                topEndUM is TangemTokenRowUM.EndContentUM.Content &&
                    bottomEndUM is TangemTokenRowUM.EndContentUM.Content -> {
                    Text(
                        text = annotatedReference {
                            appendColored(StringsSigns.DOT, TangemTheme.colors3.icon.tertiary)
                            appendSpace()
                            append(topEndUM.text.resolveReference())
                            appendSpace()
                            appendColored(StringsSigns.DOT, TangemTheme.colors3.icon.tertiary)
                            appendSpace()
                            appendColored(bottomEndUM.text.resolveReference(), TangemTheme.colors3.text.secondary)
                        }.resolveAnnotatedReference(),
                        style = TangemTheme.typography3.subheading.medium,
                        color = TangemTheme.colors3.text.primary,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
                topEndUM is TangemTokenRowUM.EndContentUM.Loading ||
                    bottomEndUM is TangemTokenRowUM.EndContentUM.Loading -> {
                    TextShimmer(style = TangemTheme.typography3.subheading.medium)
                }
                else -> Unit
            }
            SpacerWMax()
            Icon(
                imageVector = Icons.ic_error_20,
                tint = TangemTheme.colors3.icon.primary,
                contentDescription = null,
            )
        },
    )
}

private fun DrawScope.drawBadge(
    containerColor: Color,
    color: Color = TangemColorPalette.Azure,
    offset: Offset,
    size: Dp = 5.dp,
    padding: Dp = 2.dp,
) {
    drawCircle(
        color = containerColor,
        center = offset,
        radius = size.toPx(),
    )
    drawCircle(
        color = color,
        center = offset,
        radius = (size - padding).toPx(),
    )
}

@Suppress("MagicNumber")
@Composable
private fun portfolioAssetExpandAnimation(listItem: ForYouTokenListItemUM, outerLastIndex: Int): State<Int> {
    // Snap immediately on expand; on collapse, hold the current value until all
    // child items finish their shrink animation, then snap to fully-rounded shape.
    return animateIntAsState(
        targetValue = if (listItem.isExpanded) outerLastIndex else 0,
        animationSpec = if (listItem.isExpanded) {
            snap()
        } else {
            snap(delayMillis = minOf(50 * maxOf(listItem.tokenList.lastIndex, 0), 250) + 150)
        },
        label = "lastIndex",
    )
}

@Suppress("MagicNumber")
private fun portfolioAssetExpandFadeAnimation(): ContentTransform {
    return fadeIn(animationSpec = tween(350, delayMillis = 90))
        .togetherWith(fadeOut(animationSpec = tween(350)))
}

@Stable
internal class SharedTokenRowComposables(
    val title: @Composable (Modifier) -> Unit,
    val icon: @Composable (Modifier) -> Unit,
)