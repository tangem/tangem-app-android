package com.tangem.feature.wallet.presentation.common.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Constraints
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.rememberHapticFeedback
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.token.*
import com.tangem.feature.wallet.presentation.common.component.token.icon.TokenIcon
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import org.burnoutcrew.reorderable.ReorderableLazyListState
import kotlin.math.max

private const val TITLE_MIN_WIDTH_COEFFICIENT = 0.22
private const val PRICE_CHANGE_MIN_WIDTH_COEFFICIENT = 0.16

private enum class LayoutId {
    ICON, TITLE, FIAT_AMOUNT, CRYPTO_AMOUNT, PRICE_CHANGE, NON_FIAT_CONTENT
}

@Composable
internal fun TokenItem(
    state: TokenItemState,
    modifier: Modifier = Modifier,
    reorderableTokenListState: ReorderableLazyListState? = null,
    isBalanceHidden: Boolean
) {
    CustomContainer(
        state = state,
        modifier = modifier
            .tokenClickable(state = state)
            .background(color = TangemTheme.colors.background.primary),
    ) {
        TokenIcon(state = state.iconState, modifier = Modifier.layoutId(layoutId = LayoutId.ICON))

        TokenTitle(
            state = state.titleState,
            modifier = Modifier
                .layoutId(layoutId = LayoutId.TITLE)
                .padding(horizontal = TangemTheme.dimens.spacing8)
                .padding(bottom = TangemTheme.dimens.spacing2),
        )

        TokenFiatAmount(
            state = state.fiatAmountState,
            isBalanceHidden = isBalanceHidden,
            modifier = Modifier
                .layoutId(layoutId = LayoutId.FIAT_AMOUNT)
                .padding(bottom = TangemTheme.dimens.spacing2),
        )

        TokenCryptoAmount(
            state = state.cryptoAmountState,
            isBalanceHidden = isBalanceHidden,
            modifier = Modifier
                .layoutId(layoutId = LayoutId.CRYPTO_AMOUNT)
                .padding(horizontal = TangemTheme.dimens.spacing8),
        )

        TokenPriceChange(
            state = state.priceChangeState,
            modifier = Modifier.layoutId(layoutId = LayoutId.PRICE_CHANGE),
        )

        NonFiatContentBlock(
            state = state,
            reorderableTokenListState = reorderableTokenListState,
            modifier = Modifier.layoutId(layoutId = LayoutId.NON_FIAT_CONTENT),
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

/**
 * IMPORTANT! All margins that used between children setup like as children paddings.
 */
@Suppress("LongMethod")
@Composable
private fun CustomContainer(state: TokenItemState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val dimens = TangemTheme.dimens

    Layout(content = content, modifier = modifier) { measurables, constraints ->

        val layoutWidth = constraints.maxWidth
        val layoutPadding = with(density) { dimens.size14.roundToPx() }
        val layoutWidthWithPaddings = layoutWidth - 2 * layoutPadding

        val titleMinWidth = (layoutWidth * TITLE_MIN_WIDTH_COEFFICIENT).toInt()
        val priceChangeMinWidth = (layoutWidth * PRICE_CHANGE_MIN_WIDTH_COEFFICIENT).toInt()

        val icon = measurables.measure(layoutId = LayoutId.ICON, constraints = constraints)

        /*
         * Title width take the whole REMAINING space.
         * If FiatAmount took the whole free space, then Title will has min width.
         */
        val title: Placeable

        // FiatAmount width must take the whole free space but is not greater the Title min size
        var fiatAmount: Placeable? = null

        // CryptoAmount width must take the whole free space but is not greater the PriceChange min size
        var cryptoAmount: Placeable? = null

        /*
         * PriceChange width take the whole REMAINING space.
         * If CryptoAmount took the whole free space, then PriceChange will has min width.
         */
        val priceChange: Placeable?

        val nonFiatContent = measurables.measure(layoutId = LayoutId.NON_FIAT_CONTENT, constraints = constraints)

        var firstRowRemainingFreeSpace: Int? = null
        var secondRowRemainingFreeSpace: Int? = null

        when (state) {
            is TokenItemState.Content,
            is TokenItemState.Loading,
            is TokenItemState.Locked,
            -> {
                fiatAmount = measurables.measureFiatAmount(
                    state = state,
                    maxWidth = layoutWidthWithPaddings - icon.width - titleMinWidth,
                    defaultConstraints = constraints,
                )

                cryptoAmount = measurables.measureCryptoAmount(
                    state = state,
                    maxWidth = layoutWidthWithPaddings - icon.width - priceChangeMinWidth,
                    defaultConstraints = constraints,
                )

                firstRowRemainingFreeSpace = layoutWidthWithPaddings - icon.width - fiatAmount.width
                secondRowRemainingFreeSpace = layoutWidthWithPaddings - icon.width - cryptoAmount.width
            }
            is TokenItemState.Draggable -> {
                cryptoAmount = measurables.measureCryptoAmount(
                    state = state,
                    maxWidth = layoutWidthWithPaddings - icon.width - nonFiatContent.width,
                    defaultConstraints = constraints,
                )

                firstRowRemainingFreeSpace = layoutWidthWithPaddings - icon.width - nonFiatContent.width
            }
            is TokenItemState.NoAddress,
            is TokenItemState.Unreachable,
            -> {
                firstRowRemainingFreeSpace = layoutWidthWithPaddings - icon.width - nonFiatContent.width
            }
        }

        title = measurables.measureTitle(
            state = state,
            minWidth = titleMinWidth,
            remainingFreeSpace = firstRowRemainingFreeSpace,
            defaultConstraints = constraints,
        )

        priceChange = secondRowRemainingFreeSpace?.let {
            measurables.measurePriceChange(
                state = state,
                minWidth = priceChangeMinWidth,
                remainingFreeSpace = secondRowRemainingFreeSpace,
                defaultConstraints = constraints,
            )
        }

        val layoutHeight = calculateLayoutHeight(
            state = state,
            minLayoutHeight = with(density) { dimens.size68.roundToPx() },
            layoutPadding = layoutPadding,
            betweenRowsPadding = with(density) { dimens.size2.roundToPx() },
            title = title,
            fiatAmount = fiatAmount,
            cryptoAmount = cryptoAmount,
            priceChange = priceChange,
        )

        layout(width = constraints.maxWidth, height = layoutHeight) {
            icon.placeRelative(x = layoutPadding, y = (layoutHeight - icon.height).div(other = 2))

            title.placeRelative(
                x = layoutPadding + icon.width,
                y = when (state) {
                    is TokenItemState.NoAddress,
                    is TokenItemState.Unreachable,
                    -> (layoutHeight - title.height).div(other = 2)
                    else -> layoutPadding
                },
            )

            cryptoAmount?.placeRelative(
                x = layoutPadding + icon.width,
                y = layoutHeight - cryptoAmount.height - layoutPadding,
            )

            fiatAmount?.placeRelative(x = layoutWidth - fiatAmount.width - layoutPadding, y = layoutPadding)

            priceChange?.placeRelative(
                x = layoutWidth - priceChange.width - layoutPadding,
                y = layoutHeight - priceChange.height - layoutPadding,
            )

            nonFiatContent.placeRelative(
                x = layoutWidth - nonFiatContent.width - layoutPadding,
                y = (layoutHeight - nonFiatContent.height).div(other = 2),
            )
        }
    }
}

private fun List<Measurable>.measureFiatAmount(
    state: TokenItemState,
    maxWidth: Int,
    defaultConstraints: Constraints,
): Placeable {
    return measure(
        layoutId = LayoutId.FIAT_AMOUNT,
        constraints = when (state) {
            is TokenItemState.Content,
            is TokenItemState.Draggable,
            -> createConstrainsSafely(maxWidth = maxWidth)
            else -> defaultConstraints
        },
    )
}

private fun List<Measurable>.measureCryptoAmount(
    state: TokenItemState,
    maxWidth: Int,
    defaultConstraints: Constraints,
): Placeable {
    return measure(
        layoutId = LayoutId.CRYPTO_AMOUNT,
        constraints = when (state) {
            is TokenItemState.Content,
            is TokenItemState.Draggable,
            -> createConstrainsSafely(maxWidth = maxWidth)
            else -> defaultConstraints
        },
    )
}

private fun List<Measurable>.measureTitle(
    state: TokenItemState,
    minWidth: Int,
    remainingFreeSpace: Int,
    defaultConstraints: Constraints,
): Placeable {
    return measure(
        layoutId = LayoutId.TITLE,
        constraints = when (state) {
            is TokenItemState.Content,
            is TokenItemState.Draggable,
            is TokenItemState.NoAddress,
            is TokenItemState.Unreachable,
            -> createDynamicConstrains(minWidth = minWidth, remainingFreeSpace = remainingFreeSpace)
            else -> defaultConstraints
        },
    )
}

private fun List<Measurable>.measurePriceChange(
    state: TokenItemState,
    minWidth: Int,
    remainingFreeSpace: Int,
    defaultConstraints: Constraints,
): Placeable {
    return measure(
        layoutId = LayoutId.PRICE_CHANGE,
        constraints = when (state) {
            is TokenItemState.Content,
            -> createDynamicConstrains(minWidth = minWidth, remainingFreeSpace = remainingFreeSpace)
            else -> defaultConstraints
        },
    )
}

private fun List<Measurable>.measure(layoutId: LayoutId, constraints: Constraints): Placeable {
    return requireNotNull(
        value = firstOrNull { it.layoutId == layoutId },
        lazyMessage = { "Measurables[$layoutId] is null" },
    ).measure(constraints)
}

private fun createDynamicConstrains(minWidth: Int, remainingFreeSpace: Int): Constraints {
    return createConstrainsSafely(
        minWidth = minWidth,
        maxWidth = max(a = minWidth, b = remainingFreeSpace),
    )
}

private fun createConstrainsSafely(
    minWidth: Int = 0,
    maxWidth: Int = Constraints.Infinity,
    minHeight: Int = 0,
    maxHeight: Int = Constraints.Infinity,
): Constraints {
    return Constraints(
        minWidth = minWidth.makeNotLessZero(),
        maxWidth = maxWidth.makeNotLessZero(),
        minHeight = minHeight.makeNotLessZero(),
        maxHeight = maxHeight.makeNotLessZero(),
    )
}

private fun Int.makeNotLessZero(): Int = max(a = 0, b = this)

@Suppress("LongParameterList")
private fun calculateLayoutHeight(
    state: TokenItemState,
    minLayoutHeight: Int,
    layoutPadding: Int,
    betweenRowsPadding: Int,
    title: Placeable,
    fiatAmount: Placeable?,
    cryptoAmount: Placeable?,
    priceChange: Placeable?,
): Int {
    val firstColumnHeight: Int
    val secondColumnHeight: Int

    when (state) {
        is TokenItemState.Content,
        is TokenItemState.Loading,
        is TokenItemState.Locked,
        -> {
            firstColumnHeight = 2 * layoutPadding + title.height + betweenRowsPadding + (cryptoAmount?.height ?: 0)
            secondColumnHeight = 2 * layoutPadding + (fiatAmount?.height ?: 0) + betweenRowsPadding +
                (priceChange?.height ?: 0)
        }
        is TokenItemState.Draggable,
        is TokenItemState.NoAddress,
        is TokenItemState.Unreachable,
        -> {
            firstColumnHeight = minLayoutHeight
            secondColumnHeight = minLayoutHeight
        }
    }

    return max(firstColumnHeight, secondColumnHeight).coerceAtLeast(minLayoutHeight)
}

@Preview(widthDp = 360)
@Composable
private fun Preview_CustomTokenItem_InLight(@PreviewParameter(TokenItemStateProvider::class) state: TokenItemState) {
    TangemTheme(isDark = false) {
        TokenItem(state = state, isBalanceHidden = false)
    }
}

private class TokenItemStateProvider : CollectionPreviewParameterProvider<TokenItemState>(
    collection = listOf(
        WalletPreviewData.tokenItemVisibleState.copy(
            iconState = WalletPreviewData.coinIconState.copy(showCustomBadge = true),
            titleState = TokenItemState.TitleState.Content(
                text = "PolygonPolygonPolygonPolygonPolygonPolygon",
                hasPending = true,
            ),
            fiatAmountState = TokenItemState.FiatAmountState.Content(text = "3213123123321312312312312312 $"),
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = "5,4123123213123123123123123123 MATIC"),
            priceChangeState = TokenItemState.PriceChangeState.Content(
                valueInPercent = "2365723643724723423742342374623642374723472342342.0%",
                type = PriceChangeType.UP,
            ),
        ),
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
