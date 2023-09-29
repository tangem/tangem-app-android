package com.tangem.feature.wallet.presentation.common.component

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.constraintlayout.compose.*
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.rememberHapticFeedback
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.token.*
import com.tangem.feature.wallet.presentation.common.component.token.icon.TokenIcon
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import org.burnoutcrew.reorderable.ReorderableLazyListState

@Suppress("LongMethod")
@Composable
internal fun TokenItem(
    state: TokenItemState,
    modifier: Modifier = Modifier,
    reorderableTokenListState: ReorderableLazyListState? = null,
) {
    var rootWidth by remember { mutableStateOf(Int.MIN_VALUE) }

    @Suppress("DestructuringDeclarationWithTooManyEntries")
    BaseContainer(
        modifier = modifier
            .tokenClickable(state)
            .onSizeChanged { rootWidth = it.width },
    ) {
        val (iconRef, titleRef, cryptoAmountRef, fiatAmountRef, priceChangeRef, nonFiatContentRef) = createRefs()

        val isBalanceHidden = (state as? TokenItemState.Content)?.isBalanceHidden ?: false

        TokenIcon(
            state = state.iconState,
            modifier = Modifier.constrainAs(iconRef) {
                centerVerticallyTo(parent)
                start.linkTo(parent.start)
            },
        )

        val density = LocalDensity.current
        val titleRequiredMinWidth by remember(rootWidth) {
            derivedStateOf { with(density) { rootWidth.toDp().times(other = 0.22f) } }
        }

        TokenTitle(
            state = state.titleState,
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing8)
                .constrainAs(titleRef) {
                    start.linkTo(iconRef.end)
                    top.linkTo(parent.top)

                    width = Dimension.fillToConstraints.atLeast(dp = titleRequiredMinWidth)

                    when (state) {
                        is TokenItemState.Content -> end.linkTo(fiatAmountRef.start)
                        is TokenItemState.Draggable -> end.linkTo(nonFiatContentRef.start)
                        is TokenItemState.Unreachable,
                        is TokenItemState.NoAddress,
                        -> {
                            end.linkTo(nonFiatContentRef.start)
                            bottom.linkTo(parent.bottom)
                        }
                        else -> Unit
                    }
                },
        )

        TokenFiatAmount(
            state = state.fiatAmountState,
            isBalanceHidden = isBalanceHidden,
            modifier = Modifier.constrainAs(fiatAmountRef) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)

                width = Dimension.fillToConstraints.atMostWrapContent

                if (state is TokenItemState.Content) {
                    start.linkTo(titleRef.end)
                }
            },
        )

        val marginBetweenRows = TangemTheme.dimens.spacing2
        TokenCryptoAmount(
            state = state.cryptoAmountState,
            isBalanceHidden = isBalanceHidden,
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing8)
                .constrainAs(cryptoAmountRef) {
                    start.linkTo(iconRef.end)
                    top.linkTo(titleRef.bottom, marginBetweenRows)
                    bottom.linkTo(parent.bottom)

                    when (state) {
                        is TokenItemState.Content -> {
                            end.linkTo(priceChangeRef.start)
                            width = Dimension.fillToConstraints.atMostWrapContent
                        }
                        is TokenItemState.Draggable -> {
                            end.linkTo(nonFiatContentRef.start)
                            width = Dimension.fillToConstraints
                        }
                        else -> Unit
                    }
                },
        )

        val priceChangeRequiredMinWidth by remember(rootWidth) {
            derivedStateOf { with(density) { rootWidth.toDp().times(other = 0.16f) } }
        }
        TokenPriceChange(
            state = state.priceChangeState,
            modifier = Modifier.constrainAs(priceChangeRef) {
                top.linkTo(fiatAmountRef.bottom, marginBetweenRows)
                end.linkTo(anchor = parent.end)
                bottom.linkTo(parent.bottom)

                when (state.priceChangeState) {
                    is TokenItemState.PriceChangeState.Content,
                    is TokenItemState.PriceChangeState.Unknown,
                    -> {
                        start.linkTo(cryptoAmountRef.end)
                        width = Dimension.fillToConstraints
                            .atLeast(priceChangeRequiredMinWidth)
                    }
                    else -> Unit
                }
            },
        )

        NonFiatContentBlock(
            state = state,
            reorderableTokenListState = reorderableTokenListState,
            modifier = Modifier.constrainAs(nonFiatContentRef) {
                centerVerticallyTo(parent)
                end.linkTo(parent.end)
            },
        )
    }
}

@Composable
private inline fun BaseContainer(
    modifier: Modifier = Modifier,
    crossinline content: @Composable ConstraintLayoutScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size68)
            .background(color = TangemTheme.colors.background.primary),
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = TangemTheme.dimens.spacing14),
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.tokenClickable(state: TokenItemState): Modifier = composed {
    when (state) {
        is TokenItemState.Content -> {
            val onLongClick = rememberHapticFeedback(state = state, onAction = state.onItemLongClick)
            combinedClickable(onClick = state.onItemClick, onLongClick = onLongClick)
        }
        is TokenItemState.Unreachable -> {
            val onLongClick = rememberHapticFeedback(state = state, onAction = state.onItemLongClick)
            combinedClickable(onClick = state.onItemClick, onLongClick = onLongClick)
        }
        is TokenItemState.NoAddress -> {
            val onLongClick = rememberHapticFeedback(state = state, onAction = state.onItemLongClick)
            combinedClickable(onClick = {}, onLongClick = onLongClick)
        }
        is TokenItemState.Draggable,
        is TokenItemState.Loading,
        is TokenItemState.Locked,
        -> this
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
        WalletPreviewData.tokenItemVisibleState.copy(
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(
                text = "5,41221467146712416241274127841274174213421 MATIC",
            ),
        ),
        WalletPreviewData.tokenItemVisibleState.copy(
            priceChangeState = TokenItemState.PriceChangeState.Content(
                valueInPercent = "31231231231231231231223123123123212312312312.0%",
                type = PriceChangeType.UP,
            ),
        ),
        WalletPreviewData.tokenItemVisibleState.copy(
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(
                text = "5,41221467146712416241274127841274174213421 MATIC",
            ),
            priceChangeState = TokenItemState.PriceChangeState.Content(
                valueInPercent = "31231231231231231231223123123123212312312312.0%",
                type = PriceChangeType.UP,
            ),
        ),
        WalletPreviewData.tokenItemVisibleState,
        WalletPreviewData.tokenItemUnreachableState,
        WalletPreviewData.tokenItemNoAddressState,
        WalletPreviewData.tokenItemDragState,
        WalletPreviewData.tokenItemHiddenState,
        WalletPreviewData.loadingTokenItemState,
        WalletPreviewData.testnetTokenItemVisibleState,
        WalletPreviewData.customTokenItemVisibleState,
        WalletPreviewData.customTestnetTokenItemVisibleState,
    ),
)

// endregion preview