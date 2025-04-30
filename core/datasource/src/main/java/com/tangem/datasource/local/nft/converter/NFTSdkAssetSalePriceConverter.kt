package com.tangem.datasource.local.nft.converter

import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.blockchain.nft.models.NFTAsset.SalePrice as SDKSalePrice

internal class NFTSdkAssetSalePriceConverter(
    private val assetId: NFTAsset.Identifier,
) : TwoWayConverter<SDKSalePrice, NFTSalePrice.Value> {
    override fun convert(value: SDKSalePrice): NFTSalePrice.Value {
        return NFTSalePrice.Value(
            assetId = assetId,
            value = value.value,
            symbol = value.symbol,
        )
    }

    override fun convertBack(value: NFTSalePrice.Value): SDKSalePrice {
        return SDKSalePrice(
            symbol = value.symbol,
            value = value.value,
        )
    }
}