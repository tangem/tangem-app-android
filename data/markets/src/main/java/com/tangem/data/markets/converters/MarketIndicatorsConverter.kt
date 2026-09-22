package com.tangem.data.markets.converters

import com.tangem.domain.markets.MarketIndicators
import com.tangem.store.datasource.markets.models.response.MarketIndicatorsResponse
import com.tangem.utils.converter.Converter

internal object MarketIndicatorsConverter : Converter<MarketIndicatorsResponse, MarketIndicators> {

    override fun convert(value: MarketIndicatorsResponse): MarketIndicators {
        return MarketIndicators(
            totalMarketCap = value.totalMarketCap,
            marketCapChange24h = value.marketCapChangePercentage24h?.movePointLeft(2),
            fearGreed = convertIndex(value = value.fearGreedValue, classification = value.fearGreedClassification),
            altcoinSeason = convertIndex(
                value = value.altcoinIndexValue,
                classification = value.altcoinIndexClassification,
            ),
        )
    }

    /** An index without a position or without a label cannot be rendered — drop it */
    private fun convertIndex(value: Int?, classification: String?): MarketIndicators.Index? {
        if (value == null || classification.isNullOrBlank()) return null

        return MarketIndicators.Index(value = value, classification = classification)
    }
}