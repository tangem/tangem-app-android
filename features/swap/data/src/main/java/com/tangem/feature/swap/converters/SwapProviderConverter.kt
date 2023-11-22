package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.ExchangeProvider
import com.tangem.datasource.api.express.models.response.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.utils.converter.Converter
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType as ExchangeProviderTypeDomain

class SwapProviderConverter : Converter<ExchangeProvider, SwapProvider> {
    override fun convert(value: ExchangeProvider): SwapProvider {
        return SwapProvider(
            providerId = value.id,
            name = value.name,
            type = convertExchangeType(value.type),
            imageLarge = value.imageLargeUrl,
        )
    }

    private fun convertExchangeType(type: ExchangeProviderType): ExchangeProviderTypeDomain {
        return when (type) {
            ExchangeProviderType.DEX -> ExchangeProviderTypeDomain.DEX
            ExchangeProviderType.CEX -> ExchangeProviderTypeDomain.CEX
        }
    }
}