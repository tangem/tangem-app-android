package com.tangem.core.ui.ds.row.token

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed class TangemTokenRowUM {

    /** Unique id */
    abstract val id: String

    /** Token icon state */
    abstract val iconState: CurrencyIconState

    /** Token title UM (in one row with [topEndContentUM]) */
    abstract val titleUM: TitleUM

    /** Token subtitle UM (in one row with [bottomEndContentUM]) */
    abstract val subtitleUM: SubtitleUM

    /** Top end content UM (e.i. fiat amount in one row with [titleUM]) */
    abstract val topEndContentUM: EndContentUM

    /** Bottom end content UM (e.i. crypto amount in one row with [subtitleUM]) */
    abstract val bottomEndContentUM: EndContentUM

    /** Token row tail UM */
    abstract val tailUM: TailUM

    /** Promo banner UM */
    abstract val promoBannerUM: PromoBannerUM

    /** Callback which will be called when an item is clicked */
    abstract val onItemClick: ((TangemTokenRowUM) -> Unit)?

    /** Callback which will be called when an item is long clicked */
    abstract val onItemLongClick: ((TangemTokenRowUM) -> Unit)?

    /**
     * Content state of [TangemTokenRowUM]
     */
    data class Content(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleUM: TitleUM,
        override val subtitleUM: SubtitleUM,
        override val topEndContentUM: EndContentUM,
        override val bottomEndContentUM: EndContentUM,
        override val promoBannerUM: PromoBannerUM = PromoBannerUM.Empty,
        override val tailUM: TailUM = TailUM.Empty,
        override val onItemClick: ((TangemTokenRowUM) -> Unit)?,
        override val onItemLongClick: ((TangemTokenRowUM) -> Unit)?,
    ) : TangemTokenRowUM()

    /**
     * Loading state of [TangemTokenRowUM]
     */
    data class Loading(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleUM: TitleUM = TitleUM.Loading,
        override val subtitleUM: SubtitleUM = SubtitleUM.Loading,
    ) : TangemTokenRowUM() {
        override val topEndContentUM: EndContentUM = EndContentUM.Loading
        override val bottomEndContentUM: EndContentUM = EndContentUM.Loading
        override val promoBannerUM: PromoBannerUM = PromoBannerUM.Empty
        override val tailUM: TailUM = TailUM.Empty
        override val onItemClick: ((TangemTokenRowUM) -> Unit)? = null
        override val onItemLongClick: ((TangemTokenRowUM) -> Unit)? = null
    }

    /**
     * Actionable state of [TangemTokenRowUM]
     */
    data class Actionable(
        override val id: String,
        override val iconState: CurrencyIconState,
        override val titleUM: TitleUM,
        override val subtitleUM: SubtitleUM,
        override val tailUM: TailUM,
        override val onItemClick: ((TangemTokenRowUM) -> Unit)?,
        override val onItemLongClick: ((TangemTokenRowUM) -> Unit)?,
        override val topEndContentUM: EndContentUM = EndContentUM.Empty,
        override val bottomEndContentUM: EndContentUM = EndContentUM.Empty,
    ) : TangemTokenRowUM() {
        override val promoBannerUM: PromoBannerUM = PromoBannerUM.Empty
    }

    @Immutable
    sealed class TitleUM {

        data class Content(
            val text: TextReference,
            val hasPending: Boolean = false,
            val isAvailable: Boolean = true,
            val badge: TangemBadgeUM? = null,
            val onBadgeClick: (() -> Unit)? = null,
        ) : TitleUM()

        data object Loading : TitleUM()

        data object Empty : TitleUM()
    }

    @Immutable
    sealed class SubtitleUM {

        data class Content(
            val text: TextReference,
            val isAvailable: Boolean = true,
            val isFlickering: Boolean = false,
            val icons: ImmutableList<IconUM> = persistentListOf(),
            val priceChangeUM: PriceChangeState = PriceChangeState.Unknown,
            val badge: TangemBadgeUM? = null,
        ) : SubtitleUM()

        data object Loading : SubtitleUM()

        data object Empty : SubtitleUM()
    }

    @Immutable
    sealed class EndContentUM {

        data class Content(
            val text: TextReference,
            val isAvailable: Boolean = true,
            val isFlickering: Boolean = false,
            val icons: ImmutableList<IconUM> = persistentListOf(),
            val priceChangeUM: PriceChangeState = PriceChangeState.Unknown,
        ) : EndContentUM()

        data object Loading : EndContentUM()

        data object Empty : EndContentUM()
    }

    @Immutable
    sealed class PromoBannerUM {
        data class Content(
            val title: TextReference,
            val onPromoBannerClick: () -> Unit,
            val onCloseClick: () -> Unit,
            val onPromoShown: () -> Unit = {},
        ) : PromoBannerUM()

        data object Empty : PromoBannerUM()
    }

    @Immutable
    sealed class TailUM {
        data class Text(
            val text: TextReference,
        ) : TailUM()

        data object Draggable : TailUM()

        data object Empty : TailUM()
    }

    data class IconUM(
        val iconRes: Int,
        val tintReference: ColorReference2 = ColorReference2 { TangemTheme.colors2.graphic.neutral.primary },
    )
}