package com.tangem.features.nft.details.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

data class NFTAssetUM(
    val name: String,
    val media: Media,
    val topInfo: TopInfo,
    val traits: ImmutableList<BlockItem>,
    val baseInfoItems: ImmutableList<BlockItem>,
) {

    @Immutable
    sealed class TopInfo {
        data object Empty : TopInfo()
        data class Content(
            val title: TextReference?,
            val salePrice: SalePrice,
            val description: String?,
            val rarity: Rarity,
        ) : TopInfo()
    }

    @Immutable
    sealed class Media {
        data object Empty : Media()
        data class Content(
            val mimetype: String?,
            val url: String,
        ) : Media()
    }

    @Immutable
    sealed class SalePrice {
        data object Loading : SalePrice()
        data object Empty : SalePrice()
        data class Content(
            val value: BigDecimal,
            val symbol: String,
            val decimals: Int,
            val rate: BigDecimal?,
            val appCurrency: AppCurrency,
        ) : SalePrice()
    }

    @Immutable
    sealed class Rarity {
        data object Empty : Rarity()
        data class Content(
            val rank: String,
            val label: String,
            val showDivider: Boolean,
        ) : Rarity()
    }

    data class BlockItem(
        val title: TextReference,
        val titleTextEllipsis: TextEllipsis = TextEllipsis.End,
        val value: String,
        val valueTextEllipsis: TextEllipsis = TextEllipsis.End,
    )
}