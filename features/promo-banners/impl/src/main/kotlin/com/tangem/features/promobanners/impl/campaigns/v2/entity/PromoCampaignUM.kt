package com.tangem.features.promobanners.impl.campaigns.v2.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * State of the backend-driven campaign sheet: a hero image, the campaign name and the campaign's
 * [blocks] in the order the backend sent them, above a pinned [footer].
 */
internal data class PromoCampaignUM(
    val imageUrl: String,
    val title: TextReference,
    val subtitle: TextReference?,
    val blocks: ImmutableList<PromoBlockUM>,
    val tokenSelector: PromoTokenSelectorUM?,
    val footer: PromoCampaignFooterUM,
)

/** Payout token selector, shown only for campaigns that pay out; [selectedToken] is `null` until one is chosen. */
internal data class PromoTokenSelectorUM(
    val selectedToken: TokenItemState?,
    val selectedAccount: PromoSelectedAccountUM?,
    val onClick: () -> Unit,
)

internal data class PromoSelectedAccountUM(
    val iconState: CurrencyIconState,
    val name: TextReference,
)

@Immutable
internal sealed interface PromoBlockUM {

    data class Text(
        val title: TextReference?,
        val text: TextReference,
        val link: PromoLinkUM?,
    ) : PromoBlockUM

    data class Tiles(
        val title: TextReference?,
        val tiles: ImmutableList<Tile>,
    ) : PromoBlockUM

    /** The first cell of every row is its label, the rest are values. */
    data class Table(
        val title: TextReference?,
        val headers: ImmutableList<TextReference>,
        val rows: ImmutableList<ImmutableList<TextReference>>,
    ) : PromoBlockUM

    data class Highlight(
        val title: TextReference,
        val description: TextReference?,
    ) : PromoBlockUM

    data class Tile(
        val label: TextReference,
        val value: TextReference,
    )
}

internal data class PromoLinkUM(
    val text: TextReference,
    val onClick: () -> Unit,
)

/** [terms] is `null` while the action is choosing the payout token; it appears once the action is enrolment. */
internal data class PromoCampaignFooterUM(
    val terms: PromoLinkUM?,
    val buttonText: TextReference,
    val onButtonClick: () -> Unit,
    val isLoading: Boolean = false,
)