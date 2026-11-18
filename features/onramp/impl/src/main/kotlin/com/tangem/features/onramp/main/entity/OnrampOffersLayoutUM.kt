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
 * Lays out the offers block with CTA banners taking the [replacedSlots]. The All offers button is derived from the
 * same rows, so it counts exactly the offers left on screen.
 */
internal fun OnrampOffersBlockUM.layoutFor(replacedSlots: Set<RecommendedSlot>): OnrampOffersLayoutUM {
    val content = this as? OnrampOffersBlockUM.Content
    val offers = content?.recommended.orEmpty()
    val takenSlots = if (takesSlots()) replacedSlots else emptySet()
    val rows = buildList {
        RecommendedSlot.entries.forEach { slot ->
            if (slot in takenSlots) {
                add(OnrampRecommendedRowUM.Banner(slot))
            } else {
                offers.filter { it.recommendedSlot == slot }.forEach { add(OnrampRecommendedRowUM.Offer(it)) }
            }
        }
        offers.filter { it.recommendedSlot == null }.forEach { add(OnrampRecommendedRowUM.Offer(it)) }
    }.toImmutableList()
    val hasHiddenOffers = rows.count { it is OnrampRecommendedRowUM.Offer } < offers.size
    return OnrampOffersLayoutUM(
        recentOffer = content?.recentOffer,
        recommendedRows = rows,
        allOffersButton = content?.onrampAllOffersButtonConfig
            ?: content?.allOffersButtonIfOfferHidden?.takeIf { hasHiddenOffers },
    )
}