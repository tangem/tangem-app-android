package com.tangem.core.ui.ds.row.token

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowUM
import com.tangem.core.ui.ds.row.internal.TangemRowTailUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
@Immutable
sealed class TangemTokenRowUM : TangemRowUM {

    /** Unique id */
    abstract override val id: String

    /** Token icon state */
    abstract val headIconUM: TangemIconUM

    /** Token title UM (in one row with [topEndContentUM]) */
    abstract val titleUM: TitleUM

    /** Token subtitle UM (in one row with [bottomEndContentUM]) */
    abstract val subtitleUM: SubtitleUM

    /** Top end content UM (e.i. fiat amount in one row with [titleUM]) */
    abstract val topEndContentUM: EndContentUM

    /** Bottom end content UM (e.i. crypto amount in one row with [subtitleUM]) */
    abstract val bottomEndContentUM: EndContentUM

    /** Token row tail UM */
    abstract val tailUM: TangemRowTailUM

    /** Promo banner UM */
    abstract val promoBannerUM: PromoBannerUM

    /** Callback which will be called when an item is clicked */
    abstract val onItemClick: (() -> Unit)?

    /** Callback which will be called when an item is long clicked */
    abstract val onItemLongClick: ((Offset, TangemTokenRowUM) -> Any)?

    /**
     * Content state of [TangemTokenRowUM]
     */
    @Serializable
    data class Content(
        override val id: String,
        override val headIconUM: TangemIconUM.Currency,
        override val titleUM: TitleUM,
        override val subtitleUM: SubtitleUM,
        override val topEndContentUM: EndContentUM,
        override val bottomEndContentUM: EndContentUM,
        override val promoBannerUM: PromoBannerUM = PromoBannerUM.Empty,
        override val tailUM: TangemRowTailUM = TangemRowTailUM.Empty,
        override val onItemClick: (() -> Unit)?,
        override val onItemLongClick: ((Offset, TangemTokenRowUM) -> Any)?,
    ) : TangemTokenRowUM()

    /**
     * Loading state of [TangemTokenRowUM]
     */
    @Serializable
    data class Loading(
        override val id: String,
        override val headIconUM: TangemIconUM.Currency = TangemIconUM.Currency(CurrencyIconState.Loading),
        override val titleUM: TitleUM = TitleUM.Loading,
        override val subtitleUM: SubtitleUM = SubtitleUM.Loading,
    ) : TangemTokenRowUM() {
        override val topEndContentUM: EndContentUM = EndContentUM.Loading
        override val bottomEndContentUM: EndContentUM = EndContentUM.Loading
        override val promoBannerUM: PromoBannerUM = PromoBannerUM.Empty
        override val tailUM: TangemRowTailUM = TangemRowTailUM.Empty
        override val onItemClick: (() -> Unit)? = null
        override val onItemLongClick: ((Offset, TangemTokenRowUM) -> Unit)? = null
    }

    /**
     * Loading state of [TangemTokenRowUM]
     */
    @Serializable
    data class Empty(
        override val id: String,
    ) : TangemTokenRowUM() {
        override val headIconUM: TangemIconUM = TangemIconUM.Empty
        override val subtitleUM: SubtitleUM = SubtitleUM.Placeholder
        override val titleUM: TitleUM = TitleUM.Placeholder
        override val topEndContentUM: EndContentUM = EndContentUM.Placeholder
        override val bottomEndContentUM: EndContentUM = EndContentUM.Placeholder
        override val promoBannerUM: PromoBannerUM = PromoBannerUM.Empty
        override val tailUM: TangemRowTailUM = TangemRowTailUM.Empty
        override val onItemClick: (() -> Unit)? = null
        override val onItemLongClick: ((Offset, TangemTokenRowUM) -> Unit)? = null
    }

    /**
     * Actionable state of [TangemTokenRowUM]
     */
    @Serializable
    data class Actionable(
        override val id: String,
        override val headIconUM: TangemIconUM.Currency,
        override val titleUM: TitleUM,
        override val subtitleUM: SubtitleUM,
        override val tailUM: TangemRowTailUM,
        override val onItemClick: (() -> Unit)?,
        override val onItemLongClick: ((Offset, TangemTokenRowUM) -> Unit)?,
        override val topEndContentUM: EndContentUM = EndContentUM.Empty,
        override val bottomEndContentUM: EndContentUM = EndContentUM.Empty,
    ) : TangemTokenRowUM() {
        override val promoBannerUM: PromoBannerUM = PromoBannerUM.Empty
    }

    @Serializable
    @Immutable
    sealed class TitleUM {
        @Serializable
        data class Content(
            val text: TextReference,
            val hasPending: Boolean = false,
            val isAvailable: Boolean = true,
            val badge: TangemBadgeUM? = null,
        ) : TitleUM()

        @Serializable
        data object Loading : TitleUM()

        @Serializable
        data object Placeholder : TitleUM()

        @Serializable
        data object Empty : TitleUM()
    }

    @Serializable
    @Immutable
    sealed class SubtitleUM {
        @Serializable
        data class Content(
            val text: TextReference,
            val isAvailable: Boolean = true,
            val isFlickering: Boolean = false,
            val icons: ImmutableList<TangemIconUM> = persistentListOf(),
            val priceChangeUM: PriceChangeState = PriceChangeState.Unknown,
            val badge: TangemBadgeUM? = null,
        ) : SubtitleUM()

        @Serializable
        data object Loading : SubtitleUM()

        @Serializable
        data object Placeholder : SubtitleUM()

        @Serializable
        data object Empty : SubtitleUM()
    }

    @Serializable
    @Immutable
    sealed class EndContentUM {
        @Serializable
        data class Content(
            val text: TextReference,
            val isAvailable: Boolean = true,
            val isFlickering: Boolean = false,
            val startIcons: ImmutableList<TangemIconUM.Icon> = persistentListOf(),
            val endIcons: ImmutableList<TangemIconUM.Icon> = persistentListOf(),
            val priceChangeUM: PriceChangeState = PriceChangeState.Unknown,
        ) : EndContentUM()

        @Serializable
        data object Loading : EndContentUM()

        @Serializable
        data object Placeholder : EndContentUM()

        @Serializable
        data object Empty : EndContentUM()
    }

    @Serializable
    @Immutable
    sealed class PromoBannerUM {
        data class Content(
            val title: TextReference,
            @param:DrawableRes val iconRes: Int,
            val type: Type,
            val onPromoBannerClick: () -> Unit,
            val onCloseClick: () -> Unit,
            val onPromoShown: () -> Unit = {},
        ) : PromoBannerUM() {
            enum class Type {
                Yield,
                Staking,
            }
        }

        @Serializable
        data object Empty : PromoBannerUM()
    }
}