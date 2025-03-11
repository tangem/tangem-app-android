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
            allProviders = value.providers.mapNotNull { convertLeastProvider(it) },
        )
    }

    private fun convertLeastProvider(exchangeProvider: ExchangeProvider): SwapProvider? {
        val type = convertExchangeType(exchangeProvider.type) ?: return null
        return SwapProvider(
            providerId = exchangeProvider.id,
            rateTypes = emptyList(),
            name = exchangeProvider.name,
            type = type,
            imageLarge = exchangeProvider.imageLargeUrl,
            termsOfUse = exchangeProvider.termsOfUse,
            privacyPolicy = exchangeProvider.privacyPolicy,
            isRecommended = exchangeProvider.isRecommended,
            slippage = exchangeProvider.slippage,
        )
    }

    private fun convertProvider(
        swapPairProvider: SwapPairProvider,
        providerAdditional: Map<String, ExchangeProvider>,
    ): SwapPairProviderDomain? {
        val additionalProvider = providerAdditional[swapPairProvider.providerId] ?: return null
        val type = convertExchangeType(additionalProvider.type) ?: return null
        return SwapPairProviderDomain(
            providerId = swapPairProvider.providerId,
            rateTypes = swapPairProvider.rateTypes.map { rateTypeConverter.convert(it) },
            name = additionalProvider.name,
            type = type,
            imageLarge = additionalProvider.imageLargeUrl,
            termsOfUse = additionalProvider.termsOfUse,
            privacyPolicy = additionalProvider.privacyPolicy,
            isRecommended = additionalProvider.isRecommended,
            slippage = additionalProvider.slippage,
        )
    }

    private fun convertExchangeType(type: ExchangeProviderType): ExchangeProviderTypeDomain? {
        return when (type) {
            ExchangeProviderType.DEX -> ExchangeProviderTypeDomain.DEX
            ExchangeProviderType.CEX -> ExchangeProviderTypeDomain.CEX
            ExchangeProviderType.DEX_BRIDGE -> ExchangeProviderTypeDomain.DEX_BRIDGE
            ExchangeProviderType.ONRAMP -> null
        }
    }
}