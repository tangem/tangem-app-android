package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.RateType
import com.tangem.datasource.api.express.models.response.SwapPair
import com.tangem.datasource.api.express.models.response.SwapPairProvider
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast as SwapPairDomain
import com.tangem.feature.swap.domain.models.domain.SwapProvider as SwapPairProviderDomain
import com.tangem.feature.swap.domain.models.domain.RateType as RateTypeDomain
import com.tangem.utils.converter.Converter

class SwapPairInfoConverter : Converter<SwapPair, SwapPairDomain> {

    override fun convert(value: SwapPair): SwapPairDomain {
        return SwapPairDomain(
            from = LeastTokenInfo(
                contractAddress = value.from.contractAddress,
                network = value.from.network,
            ),
            to = LeastTokenInfo(
                contractAddress = value.to.contractAddress,
                network = value.to.network,
            ),
            providers = value.providers.map {
                convertProvider(it)
            },
        )
    }

    private fun convertProvider(swapPairProvider: SwapPairProvider): SwapPairProviderDomain {
        return SwapPairProviderDomain(
            providerId = swapPairProvider.providerId,
            rateTypes = swapPairProvider.rateTypes.map { convertRateType(it) },
        )
    }

    private fun convertRateType(rateType: RateType): RateTypeDomain {
        return when (rateType) {
            RateType.FIXED -> RateTypeDomain.FIXED
            RateType.FLOAT -> RateTypeDomain.FLOAT
        }
    }
}