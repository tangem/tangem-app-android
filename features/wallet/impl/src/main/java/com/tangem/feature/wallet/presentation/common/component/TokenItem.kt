package com.tangem.feature.wallet.presentation.common.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemTypography
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.TokenOptionsState
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder

private const val DOTS = "•••"
val TOKEN_ITEM_HEIGHT: Dp
    @Composable
    @ReadOnlyComposable
    get() = TangemTheme.dimens.size68

@Composable
internal fun TokenItem(state: TokenItemState, modifier: Modifier = Modifier) {
    when (state) {
        is TokenItemState.Content -> ContentTokenItem(state, modifier)
        is TokenItemState.Loading -> LoadingTokenItem(modifier)
        is TokenItemState.Draggable -> DraggableTokenItem(state, modifier, reorderableTokenListState = null)
        is TokenItemState.Unreachable -> UnreachableTokenItem(state, modifier)
    }
}

@Composable
private fun ContentTokenItem(content: TokenItemState.Content, modifier: Modifier = Modifier) {
    InternalTokenItem(
        modifier = modifier,
        name = content.name,
        tokenIconUrl = content.tokenIconUrl,
        tokenIconResId = content.tokenIconResId,
        networkIconResId = content.networkIconResId,
        amount = if (content.tokenOptions is TokenOptionsState.Hidden) DOTS else content.amount,
        hasPending = content.hasPending,
        options = { ref ->
            TokenOptionsBlock(
                modifier = Modifier.constrainAsOptionsItem(scope = this, ref),
                state = content.tokenOptions,
            )
        },
    )
}

@Composable
internal fun DraggableTokenItem(
    state: TokenItemState.Draggable,
    modifier: Modifier = Modifier,
    reorderableTokenListState: ReorderableLazyListState? = null,
) {
    InternalTokenItem(
        modifier = modifier,
        name = state.name,
        tokenIconUrl = state.tokenIconUrl,
        tokenIconResId = state.tokenIconResId,
        networkIconResId = state.networkIconResId,
        amount = state.fiatAmount,
        hasPending = false,
        options = { ref ->
            Box(
                modifier = Modifier
                    .size(TangemTheme.dimens.size32)
                    .constrainAsOptionsItem(scope = this, ref)
                    .let {
                        if (reorderableTokenListState != null) {
                            it.detectReorder(reorderableTokenListState)
                        } else {
                            it
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_drag_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
internal fun UnreachableTokenItem(state: TokenItemState.Unreachable, modifier: Modifier = Modifier) {
    InternalTokenItem(
        modifier = modifier,
        name = state.name,
        tokenIconUrl = state.tokenIconUrl,
        tokenIconResId = state.tokenIconResId,
        networkIconResId = state.networkIconResId,
        amount = null,
        hasPending = false,
        options = { ref ->
            Text(
                modifier = Modifier.constrainAsOptionsItem(scope = this, ref),
                text = "Unreachable", // TODO (conform this text)
                style = TangemTypography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
    )
}

@Composable
private fun LoadingTokenItem(modifier: Modifier = Modifier) {
    BaseSurface(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TangemTheme.dimens.spacing12,
                    vertical = TangemTheme.dimens.spacing4,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            CircleShimmer(modifier = Modifier.size(size = TangemTheme.dimens.size42))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
                    RectangleShimmer(
                        modifier = Modifier.size(
                            width = TangemTheme.dimens.size72,
                            height = TangemTheme.dimens.size12,
                        ),
                    )
                    RectangleShimmer(
                        modifier = Modifier.size(
                            width = TangemTheme.dimens.size50,
                            height = TangemTheme.dimens.size12,
                        ),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
                    RectangleShimmer(
                        modifier = Modifier.size(
                            width = TangemTheme.dimens.size40,
                            height = TangemTheme.dimens.size12,
                        ),
                    )
                    RectangleShimmer(
                        modifier = Modifier.size(
                            width = TangemTheme.dimens.size40,
                            height = TangemTheme.dimens.size12,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Block for end part of token item
 * shows status is reachable, is drag, hidden or show balance
 */
@Composable
private fun TokenOptionsBlock(state: TokenOptionsState, modifier: Modifier = Modifier) {
    when (state) {
        is TokenOptionsState.Visible -> {
            TokenFiatPercentageBlock(
                modifier = modifier,
                fiatAmount = state.fiatAmount,
                priceChange = state.priceChange,
            )
        }
        is TokenOptionsState.Hidden -> {
            TokenFiatPercentageBlock(
                modifier = modifier,
                fiatAmount = DOTS,
                priceChange = state.priceChange,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun InternalTokenItem(
    name: String,
    tokenIconUrl: String?,
    @DrawableRes tokenIconResId: Int,
    @DrawableRes networkIconResId: Int?,
    amount: String?,
    hasPending: Boolean,
    options: @Composable ConstraintLayoutScope.(ref: ConstrainedLayoutReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSurface(modifier) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TangemTheme.dimens.spacing14,
                    vertical = TangemTheme.dimens.spacing4,
                ),
        ) {
            val (iconItem, tokenNameItem, optionsItem) = createRefs()

            TokenIcon(
                modifier = Modifier.constrainAs(iconItem) {
                    centerVerticallyTo(parent)
                    start.linkTo(parent.start)
                },
                tokenIconUrl = tokenIconUrl,
                tokenIconResId = tokenIconResId,
                networkIconRes = networkIconResId,
            )

            TokenTitleAmountBlock(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .constrainAs(tokenNameItem) {
                        centerVerticallyTo(parent)
                        start.linkTo(iconItem.end)
                        end.linkTo(optionsItem.start)
                        width = Dimension.fillToConstraints
                    },
                title = name,
                amount = amount,
                hasPending = hasPending,
            )

            options(optionsItem)
        }
    }
}

@Stable
private fun Modifier.constrainAsOptionsItem(scope: ConstraintLayoutScope, ref: ConstrainedLayoutReference): Modifier {
    return with(scope) {
        this@constrainAsOptionsItem.constrainAs(ref) {
            centerVerticallyTo(parent)
            end.linkTo(parent.end)
        }
    }
}

@Composable
private fun BaseSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = TOKEN_ITEM_HEIGHT),
        color = TangemTheme.colors.background.primary,
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        content()
    }
}

@Composable
private fun TokenTitleAmountBlock(title: String, amount: String?, hasPending: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
            Text(
                text = title,
                style = TangemTypography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            if (hasPending) {
                Image(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    painter = painterResource(id = R.drawable.img_loader_15),
                    contentDescription = null,
                )
            }
        }
        if (!amount.isNullOrBlank()) {
            Text(
                text = amount,
                style = TangemTypography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun TokenFiatPercentageBlock(
    fiatAmount: String,
    priceChange: PriceChangeConfig,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.requiredWidth(IntrinsicSize.Max)) {
        Text(
            modifier = Modifier.align(Alignment.End),
            text = fiatAmount,
            style = TangemTypography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH2()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            val iconChangeArrow: Int
            val changeTextColor: Color
            when (priceChange.type) {
                PriceChangeConfig.Type.UP -> {
                    iconChangeArrow = R.drawable.img_arrow_up_8
                    changeTextColor = TangemTheme.colors.text.accent
                }
                PriceChangeConfig.Type.DOWN -> {
                    iconChangeArrow = R.drawable.img_arrow_down_8
                    changeTextColor = TangemTheme.colors.text.warning
                }
            }
            Image(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = iconChangeArrow),
                contentDescription = null,
            )
            SpacerW4()
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = priceChange.valueInPercent,
                style = TangemTypography.body2,
                color = changeTextColor,
            )
        }
    }
}

@Composable
private fun TokenIcon(
    tokenIconUrl: String?,
    modifier: Modifier = Modifier,
    @DrawableRes tokenIconResId: Int? = null,
    @DrawableRes networkIconRes: Int? = null,
) {
    Box(
        modifier = modifier
            .padding(end = TangemTheme.dimens.spacing16)
            .size(TangemTheme.dimens.size42),
    ) {
        val tokenImageModifier = Modifier
            .align(Alignment.BottomStart)
            .size(TangemTheme.dimens.size36)

        val data = if (tokenIconUrl.isNullOrEmpty()) tokenIconResId else tokenIconUrl
        SubcomposeAsyncImage(
            modifier = tokenImageModifier,
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
                .crossfade(true)
                .build(),
            loading = { CircleShimmer(modifier = tokenImageModifier) },
            contentDescription = null,
        )

        if (networkIconRes != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(TangemTheme.dimens.size18)
                    .background(color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier.padding(all = 0.5.dp),
                    painter = painterResource(id = networkIconRes),
                    contentDescription = null,
                )
            }
        }
    }
}

// region preview

@Preview
@Composable
private fun Preview_Tokens_LightTheme(@PreviewParameter(TokenConfigProvider::class) state: TokenItemState) {
    TangemTheme(isDark = false) {
        TokenItem(state)
    }
}

@Preview
@Composable
private fun Preview_Tokens_DarkTheme(@PreviewParameter(TokenConfigProvider::class) state: TokenItemState) {
    TangemTheme(isDark = true) {
        TokenItem(state)
    }
}

private class TokenConfigProvider : CollectionPreviewParameterProvider<TokenItemState>(
    collection = listOf(
        WalletPreviewData.tokenItemVisibleState,
        WalletPreviewData.tokenItemUnreachableState,
        WalletPreviewData.tokenItemDragState,
        WalletPreviewData.tokenItemHiddenState,
        WalletPreviewData.loadingTokenItemState,
    ),
)

// endregion preview