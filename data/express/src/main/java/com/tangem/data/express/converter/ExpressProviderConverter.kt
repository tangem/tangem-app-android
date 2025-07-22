package com.tangem.data.express.converter

import com.tangem.datasource.api.express.models.response.ExchangeProvider
import com.tangem.datasource.api.express.models.response.ExchangeProviderType
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.utils.converter.Converter

internal class ExpressProviderConverter : Converter<ExchangeProvider, ExpressProvider> {

    override fun convert(value: ExchangeProvider): ExpressProvider {
        return ExpressProvider(
            providerId = value.id,
            rateTypes = emptyList(),
            name = value.name,
            type = convertExchangeType(value.type),
            imageLarge = value.imageLargeUrl,
            termsOfUse = value.termsOfUse,
            privacyPolicy = value.privacyPolicy,
            isRecommended = value.isRecommended,
            slippage = value.slippage,
        )
    }

    private fun convertExchangeType(type: ExchangeProviderType): ExpressProviderType {
        return when (type) {
            ExchangeProviderType.DEX -> ExpressProviderType.DEX
            ExchangeProviderType.CEX -> ExpressProviderType.CEX
            ExchangeProviderType.DEX_BRIDGE -> ExpressProviderType.DEX_BRIDGE
            ExchangeProviderType.ONRAMP -> ExpressProviderType.ONRAMP
        }
    }
}