package com.tangem.data.express.converter

import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.utils.converter.Converter

/**
 * Converts an [Asset] from the data layer to an [ExpressAsset] in the domain layer.
 *
[REDACTED_AUTHOR]
 */
internal object ExpressAssetConverter : Converter<Asset, ExpressAsset> {

    override fun convert(value: Asset): ExpressAsset {
        return ExpressAsset(
            id = ExpressAsset.ID(
                networkId = value.network,
                contractAddress = value.contractAddress,
            ),
            isExchangeAvailable = value.exchangeAvailable,
            isOnrampAvailable = value.onrampAvailable,
        )
    }
}