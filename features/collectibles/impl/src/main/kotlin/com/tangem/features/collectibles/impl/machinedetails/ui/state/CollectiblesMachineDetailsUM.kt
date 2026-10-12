package com.tangem.features.collectibles.impl.machinedetails.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CollectiblesMachineDetailsUM {

    val onBackClick: () -> Unit

    data class Loading(override val onBackClick: () -> Unit) : CollectiblesMachineDetailsUM

    data class Content(
        override val onBackClick: () -> Unit,
        val title: TextReference,
        val imageUrl: String?,
        val tiers: ImmutableList<TierUM>,
        val details: ImmutableList<DetailUM>,
        val prizePool: PrizePoolUM,
        val footer: FooterUM,
    ) : CollectiblesMachineDetailsUM

    data class Error(
        override val onBackClick: () -> Unit,
        val message: TextReference,
        val onReloadClick: () -> Unit,
    ) : CollectiblesMachineDetailsUM

    /**
     * One rarity tier of the pack.
     *
     * @property odds value range and drop chance as one line, e.g. `$15-$40 · 80%`
     */
    data class TierUM(
        val id: String,
        val name: TextReference,
        val accent: Accent,
        val odds: TextReference,
    ) {

        /** Tier colour as a design-system role; resolved to a token in composition. */
        enum class Accent {
            Neutral, Green, Blue, Violet;

            @ReadOnlyComposable
            @Composable
            fun toColor(): Color = when (this) {
                Neutral -> TangemTheme.colors3.bg.accent.neutral
                Green -> TangemTheme.colors3.bg.accent.green
                Blue -> TangemTheme.colors3.bg.accent.blue
                Violet -> TangemTheme.colors3.bg.accent.violet
            }
        }
    }

    data class DetailUM(val id: String, val label: TextReference, val value: TextReference)

    @Immutable
    sealed interface PrizePoolUM {

        data object Loading : PrizePoolUM

        /**
         * @property rows collectibles laid out two per row; a row is one item of the screen's lazy list
         * @property isLoadingNextPage whether the footer loader of the next page is shown
         */
        data class Content(
            val rows: ImmutableList<PrizeRowUM>,
            val isLoadingNextPage: Boolean,
        ) : PrizePoolUM

        data class Error(val message: TextReference, val onReloadClick: () -> Unit) : PrizePoolUM

        data class Empty(val message: TextReference) : PrizePoolUM
    }

    /** @property id id of the first collectible in the row — the row's stable identity as a list item */
    data class PrizeRowUM(val id: String, val prizes: ImmutableList<PrizeUM>)

    data class PrizeUM(
        val id: String,
        val imageUrl: String?,
        val grade: TextReference?,
        val title: TextReference,
        val value: TextReference,
    )

    data class FooterUM(
        val isTurboEnabled: Boolean,
        val onTurboCheckedChange: (Boolean) -> Unit,
        val onTurboInfoClick: () -> Unit,
        val expectedValue: TextReference,
        val buyButtonText: TextReference,
        val onBuyClick: () -> Unit,
    )
}