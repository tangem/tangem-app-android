package com.tangem.core.ui.components.token

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Constraints
import com.tangem.core.ui.R
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.token.internal.*
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.rememberHapticFeedback
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf
import org.burnoutcrew.reorderable.ReorderableLazyListState
import java.util.UUID
import kotlin.math.max

private const val TITLE_MIN_WIDTH_COEFFICIENT = 0.3
private const val PRICE_MIN_WIDTH_COEFFICIENT = 0.32

private enum class LayoutId {
    ICON, TITLE, FIAT_AMOUNT, CRYPTO_AMOUNT, CRYPTO_PRICE, NON_FIAT_CONTENT
}

/**
 * Token item for non reorderable list
 *
 * @param state           token item state
 * @param isBalanceHidden flag that shows/hides balance
 * @param modifier        modifier
 *
 * @see <a href = "https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1051-866&t=ew8mbGp2lacuJfFm-4"
 * >Figma Component</a>
 */
@Composable
fun TokenItem(
    state: TokenItemState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
    itemPaddingValues: PaddingValues = PaddingValues(horizontal = TangemTheme.dimens.spacing12),
) {
    TokenItem(
        state = state,
        isBalanceHidden = isBalanceHidden,
        modifier = modifier,
        reorderableTokenListState = null,
        itemPaddingValues = itemPaddingValues,
    )
}

/**
 * Token item for reorderable list
 *
 * @param state                     token item state
 * @param isBalanceHidden           flag that shows/hides balance
 * @param reorderableTokenListState reorderable token list state
 * @param modifier                  modifier
 * @param itemPaddingValues         padding values
 *
 * @see <a href = "https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1051-866&t=ew8mbGp2lacuJfFm-4"
 * >Figma Component</a>
 */
@Composable
fun TokenItem(
    state: TokenItemState,
    isBalanceHidden: Boolean,
    reorderableTokenListState: ReorderableLazyListState?,
    modifier: Modifier = Modifier,
    itemPaddingValues: PaddingValues = PaddingValues(horizontal = TangemTheme.dimens.spacing12),
) {
    val betweenRowsMargin = TangemTheme.dimens.spacing2

    CustomContainer(
        state = state,
        modifier = modifier
            .tokenClickable(state = state)
            .padding(itemPaddingValues),
    ) {
        CurrencyIcon(
            state = state.iconState,
            modifier = Modifier
                .layoutId(layoutId = LayoutId.ICON)
                .padding(end = TangemTheme.dimens.spacing8),
        )

        TokenTitle(
            state = state.titleState,
            modifier = Modifier
                .layoutId(layoutId = LayoutId.TITLE)
                .padding(end = TangemTheme.dimens.spacing8)
                .padding(bottom = betweenRowsMargin),
        )

        TokenFiatAmount(
            state = state.fiatAmountState,
            isBalanceHidden = isBalanceHidden,
            modifier = Modifier
                .layoutId(layoutId = LayoutId.FIAT_AMOUNT)
                .padding(bottom = betweenRowsMargin),
        )

        TokenPrice(
            state = state.subtitleState,
            modifier = Modifier
                .layoutId(layoutId = LayoutId.CRYPTO_PRICE)
                .padding(end = TangemTheme.dimens.spacing8),
        )

        TokenCryptoAmount(
            state = state.subtitle2State,
            isBalanceHidden = isBalanceHidden,
            modifier = Modifier.layoutId(layoutId = LayoutId.CRYPTO_AMOUNT),
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
        is TokenItemState.Content,
        is TokenItemState.NoAddress,
        is TokenItemState.Unreachable,
        -> {
            val onClick = state.onItemClick
            val onLongClick = state.onItemLongClick?.let {
                rememberHapticFeedback(state = state, onAction = { it(state) })
            }

            when {
                onClick == null && onLongClick == null -> this
                onClick == null && onLongClick != null -> combinedClickable(onClick = {}, onLongClick = onLongClick)
                onClick != null && onLongClick == null -> combinedClickable(onClick = { onClick(state) })
                onClick != null && onLongClick != null -> {
                    combinedClickable(onClick = { onClick(state) }, onLongClick = onLongClick)
                }
                else -> this
            }
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
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun CustomContainer(state: TokenItemState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val dimens = TangemTheme.dimens

    Layout(content = content, modifier = modifier) { measurables, constraints ->

        val layoutWidth = constraints.maxWidth
        val verticalPadding = with(density) { dimens.size15.roundToPx() }
        val titleMinWidth = (layoutWidth * TITLE_MIN_WIDTH_COEFFICIENT).toInt()
        val priceChangeMinWidth = (layoutWidth * PRICE_MIN_WIDTH_COEFFICIENT).toInt()

        val icon = measurables.measure(layoutId = LayoutId.ICON, constraints = constraints)

        /*
         * Title width take the whole REMAINING space.
         * If FiatAmount took the whole free space, then Title will have min width.
         */
        val title: Placeable

        // FiatAmount width must take the whole free space but is not greater the Title min size
        var fiatAmount: Placeable? = null

        // CryptoAmount width must take the whole free space but is not greater the PriceChange min size
        var cryptoAmount: Placeable? = null

        /*
         * PriceChange width take the whole REMAINING space.
         * If CryptoAmount took the whole free space, then PriceChange will have min width.
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
                if (state.fiatAmountState != null) {
                    fiatAmount = measurables.measureFiatAmount(
                        state = state,
                        maxWidth = layoutWidth - icon.width - titleMinWidth,
                        defaultConstraints = constraints,
                    )
                }

                if (state.subtitle2State != null) {
                    cryptoAmount = measurables.measureCryptoAmount(
                        state = state,
                        maxWidth = layoutWidth - icon.width - priceChangeMinWidth,
                        defaultConstraints = constraints,
                    )
                }

                firstRowRemainingFreeSpace = layoutWidth - icon.width - (fiatAmount?.width ?: 0)
                secondRowRemainingFreeSpace = layoutWidth - icon.width - (cryptoAmount?.width ?: 0)
            }
            is TokenItemState.Draggable -> {
                cryptoAmount = measurables.measureCryptoAmount(
                    state = state,
                    maxWidth = layoutWidth - icon.width - nonFiatContent.width,
                    defaultConstraints = constraints,
                )

                firstRowRemainingFreeSpace = layoutWidth - icon.width - nonFiatContent.width
            }
            is TokenItemState.NoAddress,
            is TokenItemState.Unreachable,
            -> {
                firstRowRemainingFreeSpace = layoutWidth - icon.width - nonFiatContent.width

                if (state.subtitleState != null) {
                    secondRowRemainingFreeSpace = layoutWidth - icon.width - nonFiatContent.width
                }
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
            layoutPadding = verticalPadding,
            title = title,
            fiatAmount = fiatAmount,
            cryptoAmount = cryptoAmount,
            priceChange = priceChange,
        )

        layout(width = constraints.maxWidth, height = layoutHeight) {
            icon.placeRelative(x = 0, y = (layoutHeight - icon.height).div(other = 2))

            title.placeRelative(
                x = icon.width,
                y = when (state) {
                    is TokenItemState.NoAddress,
                    is TokenItemState.Unreachable,
                    -> {
                        if (state.subtitleState == null) {
                            (layoutHeight - title.height).div(other = 2)
                        } else {
                            verticalPadding
                        }
                    }
                    else -> verticalPadding
                },
            )

            fiatAmount?.placeRelative(x = layoutWidth - fiatAmount.width, y = verticalPadding)

            priceChange?.placeRelative(
                x = icon.width,
                y = layoutHeight - priceChange.height - verticalPadding,
            )

            cryptoAmount?.placeRelative(
                x = when (state) {
                    is TokenItemState.Draggable -> icon.width
                    else -> layoutWidth - cryptoAmount.width
                },
                y = layoutHeight - cryptoAmount.height - verticalPadding,
            )

            nonFiatContent.placeRelative(
                x = layoutWidth - nonFiatContent.width,
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
        layoutId = LayoutId.CRYPTO_PRICE,
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
            firstColumnHeight = 2 * layoutPadding + title.height + (cryptoAmount?.height ?: 0)
            secondColumnHeight = 2 * layoutPadding + (fiatAmount?.height ?: 0) + (priceChange?.height ?: 0)
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
private fun Preview_TokenItem_InLight(@PreviewParameter(TokenItemStateProvider::class) state: TokenItemState) {
    TangemThemePreview(isDark = false) {
        TokenItem(state = state, isBalanceHidden = false)
    }
}

private class TokenItemStateProvider : CollectionPreviewParameterProvider<TokenItemState>(
    collection = listOf(
        tokenItemVisibleState.copy(
            iconState = coinIconState.copy(showCustomBadge = true),
            titleState = TokenItemState.TitleState.Content(
                text = stringReference(value = "PolygonPolygonPolygonPolygonPolygonPolygon"),
                hasPending = true,
            ),
            fiatAmountState = FiatAmountState.Content(
                text = "3213123123321312312312312312 $",
                icons = persistentListOf(
                    FiatAmountState.Content.IconUM(R.drawable.ic_error_sync_24, useAccentColor = false),
                    stakingIcon,
                ),
                isFlickering = true,
            ),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(
                text = "5,4123123213123123123123123123 MATIC",
                isFlickering = true,
            ),
            subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                price = "312 USD",
                priceChangePercent = "42.0%",
                type = PriceChangeType.UP,
            ),
        ),
        TokenItemState.Unreachable(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            onItemClick = {},
            onItemLongClick = {},
        ),
        TokenItemState.Unreachable(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            subtitleState = TokenItemState.SubtitleState.TextContent(stringReference(value = "Token")),
            onItemClick = {},
            onItemLongClick = {},
        ),
        TokenItemState.NoAddress(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            onItemLongClick = {},
        ),
        TokenItemState.NoAddress(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = stringReference(value = "Token")),
            onItemLongClick = {},
        ),
        TokenItemState.Draggable(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "3 172,14 $"),
        ),
        TokenItemState.Content(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            fiatAmountState = FiatAmountState.Content(
                text = "321 $",
                icons = persistentListOf(stakingIcon),
            ),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "5,412 MATIC"),
            subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                price = "312 USD",
                priceChangePercent = "2.0%",
                type = PriceChangeType.UP,
            ),
            onItemClick = {},
            onItemLongClick = {},
        ),
        TokenItemState.Content(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            fiatAmountState = FiatAmountState.Content(
                text = "321 $",
                icons = persistentListOf(stakingIcon),
            ),
            subtitle2State = TokenItemState.Subtitle2State.LabelContent(
                auditLabelUM = AuditLabelUM(
                    text = TextReference.Str(value = "Trusted"),
                    type = AuditLabelUM.Type.Permit,
                ),
            ),
            subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                price = "312 USD",
                priceChangePercent = "2.0%",
                type = PriceChangeType.UP,
            ),
            onItemClick = {},
            onItemLongClick = {},
        ),
        TokenItemState.Loading(
            id = "Loading#1",
            iconState = customTokenIconState.copy(isGrayscale = true),
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
        ),
        tokenItemVisibleState.copy(
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon testnet")),
            iconState = tokenIconState.copy(isGrayscale = true),
        ),
        tokenItemVisibleState.copy(
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            iconState = customTokenIconState.copy(
                tint = TangemColorPalette.White,
                background = TangemColorPalette.Black,
            ),
        ),
        tokenItemVisibleState.copy(
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            iconState = customTokenIconState.copy(isGrayscale = true),
        ),
    ),
) {

    companion object {

        val stakingIcon = FiatAmountState.Content.IconUM(R.drawable.ic_staking_24, useAccentColor = true)

        val coinIconState
            get() = CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_polygon_22,
                isGrayscale = false,
                showCustomBadge = false,
            )

        val tokenIconState
            get() = CurrencyIconState.TokenIcon(
                url = null,
                topBadgeIconResId = R.drawable.img_polygon_22,
                fallbackTint = TangemColorPalette.Black,
                fallbackBackground = TangemColorPalette.Meadow,
                isGrayscale = false,
                showCustomBadge = false,
            )

        private val customTokenIconState
            get() = CurrencyIconState.CustomTokenIcon(
                tint = TangemColorPalette.Black,
                background = TangemColorPalette.Meadow,
                topBadgeIconResId = R.drawable.img_polygon_22,
                isGrayscale = false,
            )

        val tokenItemVisibleState by lazy {
            TokenItemState.Content(
                id = UUID.randomUUID().toString(),
                iconState = coinIconState,
                titleState = TokenItemState.TitleState.Content(
                    text = stringReference(value = "Polygon"),
                    hasPending = true,
                ),
                fiatAmountState = FiatAmountState.Content(
                    text = "321 $",
                    icons = persistentListOf(stakingIcon),
                ),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "5,412 MATIC"),
                subtitleState = TokenItemState.SubtitleState.Unknown,
                onItemClick = {},
                onItemLongClick = {},
            )
        }
    }
}