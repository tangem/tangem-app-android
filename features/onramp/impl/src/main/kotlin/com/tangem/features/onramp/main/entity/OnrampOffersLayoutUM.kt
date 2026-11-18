package com.tangem.features.onramp.main.entity

import com.tangem.domain.marketing.models.MarketingBanner.RecommendedSlot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class OnrampOffersLayoutUM(
    val recentOffer: OnrampOfferUM?,
    val recommendedRows: ImmutableList<OnrampRecommendedRowUM>,
    val allOffersButton: OnrampAllOffersButtonConfig?,
)

internal sealed interface OnrampRecommendedRowUM {
    data class Offer(val offer: OnrampOfferUM) : OnrampRecommendedRowUM
    data class Banner(val slot: RecommendedSlot) : OnrampRecommendedRowUM
}

/**
 * Whether CTA banners may take Recommended slots. With offers loaded they always do, even when only a Recently used
 * offer is left, so the banners form the Recommended block. A loading block keeps its slots so the banner does not
 * jump while the amount is typed; only an empty block leaves them above the offers.
 */
internal fun OnrampOffersBlockUM.takesSlots(): Boolean = this != OnrampOffersBlockUM.Empty

/**
 * Lays out the offers block with CTA banners taking the [replacedSlots]. A banner replaces the offer in its slot; with
 * no such offer, a best banner goes before the first fastest or unslotted offer and a fastest banner goes last. The
 * All offers button is derived from the same rows, so it counts exactly the offers left on screen.
 */
internal fun OnrampOffersBlockUM.layoutFor(replacedSlots: Set<RecommendedSlot>): OnrampOffersLayoutUM {
    val content = this as? OnrampOffersBlockUM.Content
    val offers = content?.recommended.orEmpty()
    val takenSlots = if (takesSlots()) replacedSlots else emptySet()
    val rows = offers.mapTo(mutableListOf<OnrampRecommendedRowUM>(), OnrampRecommendedRowUM::Offer)
    RecommendedSlot.entries.filter { it in takenSlots }.forEach { slot -> rows.placeBanner(slot) }
    val hasHiddenOffers = rows.count { it is OnrampRecommendedRowUM.Offer } < offers.size
    return OnrampOffersLayoutUM(
        recentOffer = content?.recentOffer,
        recommendedRows = rows.toImmutableList(),
        allOffersButton = content?.onrampAllOffersButtonConfig
            ?: content?.allOffersButtonIfOfferHidden?.takeIf { hasHiddenOffers },
    )
}

private fun MutableList<OnrampRecommendedRowUM>.placeBanner(slot: RecommendedSlot) {
    val banner = OnrampRecommendedRowUM.Banner(slot)
    val slotIndex = indexOfFirst { it is OnrampRecommendedRowUM.Offer && it.offer.recommendedSlot == slot }
    if (slotIndex != -1) {
        set(slotIndex, banner)
        return
    }
    val insertIndex = when (slot) {
        RecommendedSlot.BEST -> indexOfFirst {
            it is OnrampRecommendedRowUM.Offer && it.offer.recommendedSlot != RecommendedSlot.BEST
        }.takeIf { it != -1 } ?: size
        RecommendedSlot.FASTEST -> size
    }
    add(insertIndex, banner)
}