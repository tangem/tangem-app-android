package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.GetCoinIndicatorsResponse.Asset
import com.tangem.datasource.api.markets.models.response.GetCoinIndicatorsResponse.Asset.Indicator
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.CoinIndicators.Reading
import com.tangem.utils.converter.Converter

internal object CoinIndicatorsConverter : Converter<Asset, CoinIndicators> {

    override fun convert(value: Asset): CoinIndicators {
        return CoinIndicators(
            symbol = value.symbol,
            readings = value.indicators.mapNotNull(::convertReading),
        )
    }

    /** Readings of a type or timeframe unknown to this app version cannot be displayed — drop them */
    private fun convertReading(dto: Indicator): Reading? {
        val type = convertType(dto.type) ?: return null
        val timeframe = convertTimeframe(dto.timeframe) ?: return null

        return Reading(
            type = type,
            name = dto.name,
            timeframe = timeframe,
            value = dto.value,
            signal = convertSignal(dto.label),
            updatedAt = dto.updatedAt,
        )
    }

    private fun convertType(dto: Indicator.Type): Reading.Type? {
        return when (dto) {
            Indicator.Type.RSI -> Reading.Type.RSI
            Indicator.Type.MACD -> Reading.Type.MACD
            Indicator.Type.MA_CROSS -> Reading.Type.MA_CROSS
            Indicator.Type.GALAXY_SCORE -> Reading.Type.GALAXY_SCORE
            Indicator.Type.SENTIMENT -> Reading.Type.SENTIMENT
            Indicator.Type.UNKNOWN -> null
        }
    }

    private fun convertTimeframe(dto: Indicator.Timeframe): Reading.Timeframe? {
        return when (dto) {
            Indicator.Timeframe.H24 -> Reading.Timeframe.DAY
            Indicator.Timeframe.D7 -> Reading.Timeframe.WEEK
            Indicator.Timeframe.M1 -> Reading.Timeframe.MONTH
            Indicator.Timeframe.UNKNOWN -> null
        }
    }

    private fun convertSignal(dto: Indicator.Signal): Reading.Signal {
        return when (dto) {
            Indicator.Signal.POSITIVE -> Reading.Signal.POSITIVE
            Indicator.Signal.NEGATIVE -> Reading.Signal.NEGATIVE
            Indicator.Signal.NEUTRAL -> Reading.Signal.NEUTRAL
            Indicator.Signal.INSUFFICIENT_DATA -> Reading.Signal.INSUFFICIENT_DATA
            Indicator.Signal.NOT_AVAILABLE,
            Indicator.Signal.UNKNOWN,
            -> Reading.Signal.NOT_AVAILABLE
        }
    }
}