package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.*
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.utils.converter.Converter
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType as ExchangeProviderTypeDomain
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast as SwapPairDomain
import com.tangem.feature.swap.domain.models.domain.SwapProvider as SwapPairProviderDomain

class SwapPairInfoConverter : Converter<SwapPairsWithProviders, List<SwapPairDomain>> {

    private val rateTypeConverter = RateTypeConverter()

    override fun convert(value: SwapPairsWithProviders): List<SwapPairDomain> {
        val providersAdditionalMap = value.providers.associateBy { it.id }
        return value.swapPair.map { pair ->
            SwapPairDomain(
                from = LeastTokenInfo(
                    contractAddress = pair.from.contractAddress,
                    network = pair.from.network,
                ),
                to = LeastTokenInfo(
                    contractAddress = pair.to.contractAddress,
                    network = pair.to.network,
                ),
                providers = pair.providers.mapNotNull {
                    convertProvider(it, providersAdditionalMap)
                },
            )
        }
    }

    private fun convertProvider(
        swapPairProvider: SwapPairProvider,
        providerAdditional: Map<String, ExchangeProvider>,
    ): SwapPairProviderDomain? {
        val additionalProvider = providerAdditional[swapPairProvider.providerId] ?: return null
        return SwapPairProviderDomain(
            providerId = swapPairProvider.providerId,
            rateTypes = swapPairProvider.rateTypes.map { rateTypeConverter.convert(it) },
            name = additionalProvider.name,
            type = convertExchangeType(additionalProvider.type),
            imageLarge = additionalProvider.imageLargeUrl,
        )
    }

    private fun convertExchangeType(type: ExchangeProviderType): ExchangeProviderTypeDomain {
        return when (type) {
            ExchangeProviderType.DEX -> ExchangeProviderTypeDomain.DEX
            ExchangeProviderType.CEX -> ExchangeProviderTypeDomain.CEX
        }
    }
}