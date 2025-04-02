package com.tangem.features.nft.details.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

data class NFTAssetUM(
    val name: String,
    val salePrice: SalePrice,
    val description: String?,
    val rarity: Rarity?,
    val media: Media?,
    val traits: ImmutableList<BlockItem>,
    val baseInfoItems: ImmutableList<BlockItem>,
) {
    data class Media(
        val mimetype: String?,
        val url: String,
    )

    data class Rarity(
        val rank: String,
        val label: String,
    )

    data class BlockItem(
        val title: TextReference,
        val titleTextEllipsis: TextEllipsis = TextEllipsis.End,
        val value: String,
        val valueTextEllipsis: TextEllipsis = TextEllipsis.End,
    )

    @Immutable
    sealed class SalePrice {
        data object Loading : SalePrice()
        data object Empty : SalePrice()
        data class Content(val value: String, val fiatValue: String) : SalePrice()
    }
}
