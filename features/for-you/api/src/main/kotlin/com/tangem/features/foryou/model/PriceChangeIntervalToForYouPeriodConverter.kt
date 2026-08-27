package com.tangem.features.foryou.model

import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.utils.converter.Converter

/**
 * Maps a market-details chart [PriceChangeInterval] to the token summary [ForYouPeriod].
 *
 * Token summary only supports day/week/month, so any interval longer than a month is capped at
 * [ForYouPeriod.Month] — the token-summary business rule that lives solely in this feature.
 */
class PriceChangeIntervalToForYouPeriodConverter : Converter<PriceChangeInterval, ForYouPeriod> {

    override fun convert(value: PriceChangeInterval): ForYouPeriod = when (value) {
        PriceChangeInterval.H24 -> ForYouPeriod.Day
        PriceChangeInterval.WEEK -> ForYouPeriod.Week
        PriceChangeInterval.MONTH,
        PriceChangeInterval.MONTH3,
        PriceChangeInterval.MONTH6,
        PriceChangeInterval.YEAR,
        PriceChangeInterval.ALL_TIME,
        -> ForYouPeriod.Month
    }
}