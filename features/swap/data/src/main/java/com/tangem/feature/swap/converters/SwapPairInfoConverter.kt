package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.*
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.PairsWithProviders
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.utils.converter.Converter
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType as ExchangeProviderTypeDomain
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast as SwapPairDomain
import com.tangem.feature.swap.domain.models.domain.SwapProvider as SwapPairProviderDomain

class SwapPairInfoConverter : Converter<SwapPairsWithProviders, PairsWithProviders> {

    private val rateTypeConverter = RateTypeConverter()

    override fun convert(value: SwapPairsWithProviders): PairsWithProviders {
        val providersAdditionalMap = value.providers.associateBy { it.id }
        val pairs = value.swapPair.map { pair ->
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
        return PairsWithProviders(
            pairs = pairs,
            allProviders = value.providers.map { convertLeastProvider(it) },
        )
    }

    private fun convertLeastProvider(exchangeProvider: ExchangeProvider): SwapProvider {
        return SwapProvider(
            providerId = exchangeProvider.id,
            rateTypes = emptyList(),
            name = exchangeProvider.name,
            type = convertExchangeType(exchangeProvider.type),
            imageLarge = exchangeProvider.imageLargeUrl,
            termsOfUse = exchangeProvider.termsOfUse,
            privacyPolicy = exchangeProvider.privacyPolicy,
            isRecommended = exchangeProvider.isRecommended,
        )
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
            termsOfUse = additionalProvider.termsOfUse,
            privacyPolicy = additionalProvider.privacyPolicy,
            isRecommended = additionalProvider.isRecommended,
        )
    }

    private fun convertExchangeType(type: ExchangeProviderType): ExchangeProviderTypeDomain {
        return when (type) {
            ExchangeProviderType.DEX -> ExchangeProviderTypeDomain.DEX
            ExchangeProviderType.CEX -> ExchangeProviderTypeDomain.CEX
            ExchangeProviderType.DEX_BRIDGE -> ExchangeProviderTypeDomain.DEX_BRIDGE
        }
    }
}