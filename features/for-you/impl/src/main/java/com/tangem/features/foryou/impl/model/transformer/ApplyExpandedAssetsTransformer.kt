package com.tangem.features.foryou.impl.model.transformer

import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.ForYouWalletGroupUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Applies the expanded-assets selection to one section's token list by flipping [ForYouTokenListItemUM.isExpanded]
 * on the affected items only
 */
internal class ApplyExpandedAssetsTransformer(
    private val expandedAssetIds: Set<String>,
    private val section: Section,
) : Transformer<ForYouUM> {

    enum class Section { PortfolioReview, EarnOpportunities }

    override fun transform(prevState: ForYouUM): ForYouUM {
        return when (section) {
            Section.PortfolioReview -> {
                val updated = prevState.portfolioReviewUM.applyExpandedAssets(expandedAssetIds)
                if (updated === prevState.portfolioReviewUM) prevState else prevState.copy(portfolioReviewUM = updated)
            }
            Section.EarnOpportunities -> {
                val updated = prevState.earnOpportunities.applyExpandedAssets(expandedAssetIds)
                if (updated === prevState.earnOpportunities) prevState else prevState.copy(earnOpportunities = updated)
            }
        }
    }
}

/** Returns `this` when no item's expansion changes, so callers can skip the state update entirely. */
internal fun PortfolioReviewUM.applyExpandedAssets(expandedAssetIds: Set<String>): PortfolioReviewUM {
    val updated = tokenList.applyExpandedAssets(expandedAssetIds)
    if (updated === tokenList) return this
    return when (this) {
        is PortfolioReviewUM.Loading -> copy(tokenList = updated)
        is PortfolioReviewUM.Content -> copy(tokenList = updated)
    }
}

/** Returns `this` when no item's expansion changes, so callers can skip the state update entirely. */
internal fun EarnOpportunitiesUM.applyExpandedAssets(expandedAssetIds: Set<String>): EarnOpportunitiesUM {
    val updated = tokenList.applyExpandedAssetsToGroups(expandedAssetIds)
    if (updated === tokenList) return this
    return when (this) {
        is EarnOpportunitiesUM.Loading -> copy(tokenList = updated)
        is EarnOpportunitiesUM.Content -> copy(tokenList = updated)
    }
}

private fun ImmutableList<ForYouWalletGroupUM>.applyExpandedAssetsToGroups(
    expandedAssetIds: Set<String>,
): ImmutableList<ForYouWalletGroupUM> {
    var isChanged = false
    val updated = map { group ->
        val updatedItems = group.items.applyExpandedAssets(expandedAssetIds)
        if (updatedItems === group.items) {
            group
        } else {
            isChanged = true
            group.copy(items = updatedItems)
        }
    }
    return if (isChanged) updated.toPersistentList() else this
}

private fun ImmutableList<ForYouTokenListItemUM>.applyExpandedAssets(
    expandedAssetIds: Set<String>,
): ImmutableList<ForYouTokenListItemUM> {
    var isChanged = false
    val updated = map { item ->
        val isExpanded = item.isExpandable && item.tokenRowUM.id in expandedAssetIds
        if (item.isExpanded == isExpanded) {
            item
        } else {
            isChanged = true
            item.copy(isExpanded = isExpanded)
        }
    }
    return if (isChanged) updated.toPersistentList() else this
}