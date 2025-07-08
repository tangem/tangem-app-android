package com.tangem.features.nft.details.entity.transformer

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.features.nft.impl.R
import com.tangem.utils.transformer.Transformer

internal class NFTPriceChangeTransformer(
    private val appCurrency: AppCurrency,
    private val nftSalePrice: NFTSalePrice,
) : Transformer<NFTDetailsUM> {

    override fun transform(prevState: NFTDetailsUM): NFTDetailsUM {
        val topInfo = prevState.nftAsset.topInfo as? NFTAssetUM.TopInfo.Content ?: return prevState

        val salePrice = when (nftSalePrice) {
            is NFTSalePrice.Empty,
            is NFTSalePrice.Error,
            -> NFTAssetUM.SalePrice.Empty
            is NFTSalePrice.Loading -> NFTAssetUM.SalePrice.Loading
            is NFTSalePrice.Value -> NFTAssetUM.SalePrice.Content(
                isFlickering = false,
                cryptoPrice = stringReference(
                    nftSalePrice.value.format {
                        crypto(
                            symbol = nftSalePrice.symbol,
                            decimals = nftSalePrice.decimals,
                        )
                    },
                ),
                fiatPrice = stringReference(
                    nftSalePrice.fiatValue.format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                ),
            )
        }

        val hasSalePrice = salePrice !is NFTAssetUM.SalePrice.Empty

        val newTopInfo = if (
            topInfo.rarity is NFTAssetUM.Rarity.Empty &&
            topInfo.description.isNullOrEmpty() &&
            !hasSalePrice
        ) {
            NFTAssetUM.TopInfo.Empty
        } else {
            topInfo.copy(
                title = resourceReference(R.string.nft_details_last_sale_price).takeIf { hasSalePrice },
                salePrice = salePrice,
            )
        }

        return prevState.copy(
            nftAsset = prevState.nftAsset.copy(
                topInfo = newTopInfo,
            ),
        )
    }
}