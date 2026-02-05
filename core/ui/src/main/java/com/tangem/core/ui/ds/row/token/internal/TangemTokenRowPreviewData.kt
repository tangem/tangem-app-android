package com.tangem.core.ui.ds.row.token.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.extensions.styledStringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

internal object TangemTokenRowPreviewData {

    private val priceChangeState: PriceChangeState.Content
        get() = PriceChangeState.Content(
            type = PriceChangeType.UP,
            valueInPercent = "2.45%",
        )

    val promoBannerUM: TangemTokenRowUM.PromoBannerUM.Content
        get() = TangemTokenRowUM.PromoBannerUM.Content(
            title = stringReference("Earn yield by supplying your crypto assets"),
            onPromoBannerClick = {},
            onPromoShown = {},
            onCloseClick = {},
        )

    val titleUM: TangemTokenRowUM.TitleUM.Content
        get() = TangemTokenRowUM.TitleUM.Content(
            text = stringReference(value = "Polygon"),
            hasPending = true,
        )

    val subtitleUM: TangemTokenRowUM.SubtitleUM.Content
        get() = TangemTokenRowUM.SubtitleUM.Content(
            text = stringReference(value = "$ 0.6631"),
            priceChangeUM = priceChangeState,
        )

    val topEndContentUM: TangemTokenRowUM.EndContentUM.Content
        get() = TangemTokenRowUM.EndContentUM.Content(
            text = combinedReference(
                stringReference("$ 500"),
                styledStringReference(".17", {
                    SpanStyle(
                        color = TangemTheme.colors2.text.neutral.secondary,
                        fontWeight = TangemTheme.typography2.bodyRegular16.fontWeight,
                    )
                }),
            ),
        )

    val bottomEndContentUM: TangemTokenRowUM.EndContentUM.Content
        get() = TangemTokenRowUM.EndContentUM.Content(
            text = stringReference("Title"),
        )

    private val accountResIcon: TangemIconUM.Currency
        get() = TangemIconUM.Currency(
            CurrencyIconState.CryptoPortfolio.Icon(
                resId = R.drawable.ic_rounded_star_24,
                color = Color.Blue,
                isGrayscale = false,
            ),
        )

    private val accountLetterIcon: TangemIconUM.Currency
        get() = TangemIconUM.Currency(
            CurrencyIconState.CryptoPortfolio.Letter(
                char = stringReference("A"),
                color = Color.Blue,
                isGrayscale = false,
            ),
        )

    private val coinIconState: TangemIconUM.Currency
        get() = TangemIconUM.Currency(
            CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_polygon_22,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        )

    private val tokenIconState: TangemIconUM.Currency
        get() = TangemIconUM.Currency(
            CurrencyIconState.TokenIcon(
                url = null,
                topBadgeIconResId = R.drawable.img_polygon_22,
                fallbackTint = TangemColorPalette.Black,
                fallbackBackground = TangemColorPalette.Meadow,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        )

    private val customTokenIconState: TangemIconUM.Currency
        get() = TangemIconUM.Currency(
            CurrencyIconState.CustomTokenIcon(
                tint = TangemColorPalette.Black,
                background = TangemColorPalette.Meadow,
                topBadgeIconResId = R.drawable.img_polygon_22,
                isGrayscale = false,
            ),
        )

    val defaultState: TangemTokenRowUM.Content
        get() = TangemTokenRowUM.Content(
            id = UUID.randomUUID().toString(),
            headIconUM = coinIconState,
            titleUM = titleUM,
            subtitleUM = subtitleUM,
            topEndContentUM = topEndContentUM,
            bottomEndContentUM = bottomEndContentUM,
            promoBannerUM = TangemTokenRowUM.PromoBannerUM.Empty,
            tailUM = TangemTokenRowUM.TailUM.Empty,
            onItemClick = {},
            onItemLongClick = {},
        )

    val defaultEllipsisState: TangemTokenRowUM.Content
        get() = defaultState.copy(
            titleUM = titleUM.copy(text = stringReference("Polygon Polygon Polygon Polygon")),
            subtitleUM = subtitleUM,
            topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = combinedReference(
                    stringReference("$ 500"),
                    styledStringReference(".11232131237", {
                        SpanStyle(
                            color = TangemTheme.colors2.text.neutral.secondary,
                            fontWeight = TangemTheme.typography2.bodyRegular16.fontWeight,
                        )
                    }),
                ),
                icons = persistentListOf(
                    TangemIconUM.Icon(R.drawable.ic_staking_mini_10),
                    TangemIconUM.Icon(R.drawable.ic_attention_12),
                    TangemIconUM.Icon(R.drawable.ic_error_sync_24),
                ),
            ),
            bottomEndContentUM = bottomEndContentUM,
            promoBannerUM = TangemTokenRowUM.PromoBannerUM.Empty,
            tailUM = TangemTokenRowUM.TailUM.Empty,
            onItemClick = {},
            onItemLongClick = {},
        )

    val tokenState: TangemTokenRowUM.Content
        get() = TangemTokenRowUM.Content(
            id = UUID.randomUUID().toString(),
            headIconUM = tokenIconState,
            titleUM = titleUM,
            subtitleUM = subtitleUM,
            topEndContentUM = topEndContentUM,
            bottomEndContentUM = bottomEndContentUM,
            promoBannerUM = TangemTokenRowUM.PromoBannerUM.Empty,
            tailUM = TangemTokenRowUM.TailUM.Empty,
            onItemClick = {},
            onItemLongClick = {},
        )

    val customTokenState: TangemTokenRowUM.Content
        get() = TangemTokenRowUM.Content(
            id = UUID.randomUUID().toString(),
            headIconUM = customTokenIconState,
            titleUM = titleUM,
            subtitleUM = subtitleUM,
            topEndContentUM = topEndContentUM,
            bottomEndContentUM = bottomEndContentUM,
            promoBannerUM = TangemTokenRowUM.PromoBannerUM.Empty,
            tailUM = TangemTokenRowUM.TailUM.Empty,
            onItemClick = {},
            onItemLongClick = {},
        )

    val draggableState: TangemTokenRowUM.Actionable
        get() = TangemTokenRowUM.Actionable(
            id = UUID.randomUUID().toString(),
            headIconUM = coinIconState,
            titleUM = titleUM,
            subtitleUM = subtitleUM,
            tailUM = TangemTokenRowUM.TailUM.Draggable,
            onItemClick = {},
            onItemLongClick = {},
        )

    val draggableStateV2: TangemTokenRowUM.Actionable
        get() = TangemTokenRowUM.Actionable(
            id = UUID.randomUUID().toString(),
            headIconUM = coinIconState,
            titleUM = titleUM,
            subtitleUM = subtitleUM,
            topEndContentUM = topEndContentUM,
            bottomEndContentUM = bottomEndContentUM,
            tailUM = TangemTokenRowUM.TailUM.Draggable,
            onItemClick = {},
            onItemLongClick = {},
        )

    val loadingState: TangemTokenRowUM.Loading
        get() = TangemTokenRowUM.Loading(
            id = UUID.randomUUID().toString(),
            headIconUM = coinIconState,
            titleUM = TangemTokenRowUM.TitleUM.Loading,
            subtitleUM = TangemTokenRowUM.SubtitleUM.Loading,
        )

    val unreachableState: TangemTokenRowUM
        get() = defaultState.copy(
            topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = stringReference(StringsSigns.DASH_SIGN),
            ),
            bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = styledResourceReference(
                    id = R.string.common_unreachable,
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.status.attention) },
                ),
            ),
        )

    val accountState: TangemTokenRowUM.Content
        get() = TangemTokenRowUM.Content(
            id = UUID.randomUUID().toString(),
            headIconUM = accountResIcon,
            titleUM = TangemTokenRowUM.TitleUM.Content(
                text = stringReference(value = "Portfolio"),
            ),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                text = stringReference("24 tokens"),
            ),
            topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = stringReference("22,129.65 $"),
            ),
            bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = stringReference("+ $ 1,245.32"),
                priceChangeUM = priceChangeState,
            ),
            onItemClick = {},
            onItemLongClick = {},
        )

    val accountLetterState: TangemTokenRowUM.Content
        get() = accountState.copy(
            headIconUM = accountLetterIcon,
        )

    val accountEllipsisState: TangemTokenRowUM.Content
        get() = accountState.copy(
            titleUM = TangemTokenRowUM.TitleUM.Content(
                text = stringReference(value = "Portfolio Portfolio Portfolio Portfolio"),
            ),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                text = stringReference("24 tokens 24 tokens 24 tokens 24 tokens"),
            ),
            topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = stringReference("22,129.6129387147653025 $"),
            ),
            bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = stringReference("+ $ 1,245.31093284302752"),
                priceChangeUM = priceChangeState,
            ),
        )

    val promoBannerState: TangemTokenRowUM.Content
        get() = defaultState.copy(
            promoBannerUM = promoBannerUM,
        )
}