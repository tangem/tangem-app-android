package com.tangem.features.markets.details.impl.model.formatter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.formatAsDateTime
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.features.markets.impl.R
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.math.BigDecimal

internal object MarketsDateTimeFormatters {

    private const val H24_MILLIS = 24L * 60 * 60 * 1000
    private const val WEEK_MILLIS = 7L * H24_MILLIS

    private val dateTimeMMMFormatter by lazy {
        DateTimeFormatters.getBestFormatterBySkeleton("dd MMM hh:mm")
    }

    private val dateFormatter = DateTimeFormatters.dateDDMMYYYY

    internal fun getChartXFormatterByInterval(interval: PriceChangeInterval): (BigDecimal) -> String {
        return when (interval) {
            PriceChangeInterval.H24 -> { value: BigDecimal ->
                value.toLong().formatAsDateTime(DateTimeFormatters.timeFormatter)
            }
            PriceChangeInterval.WEEK,
            PriceChangeInterval.MONTH,
            PriceChangeInterval.MONTH3,
            PriceChangeInterval.MONTH6,
            -> { value ->
                value.toLong().formatAsDateTime(DateTimeFormatters.dateMMMdd)
            }
            PriceChangeInterval.YEAR -> { value ->
                value.toLong().formatAsDateTime(DateTimeFormatters.dateMMMdd)
            }
            PriceChangeInterval.ALL_TIME -> { value ->
                value.toLong().formatAsDateTime(DateTimeFormatters.dateYYYY)
            }
        }
    }

    internal fun formatDateByInterval(interval: PriceChangeInterval, startTimestamp: Long): TextReference {
        return when (interval) {
            PriceChangeInterval.H24 -> resourceReference(R.string.common_today)
            PriceChangeInterval.WEEK,
            PriceChangeInterval.MONTH,
            PriceChangeInterval.MONTH3,
            -> {
                resourceReference(
                    R.string.common_range_with_space,
                    wrappedList(
                        stringReference(
                            startTimestamp.formatAsDateTime(MarketsDateTimeFormatters.dateTimeMMMFormatter),
                        ),
                        resourceReference(R.string.common_now),
                    ),
                )
            }
            PriceChangeInterval.MONTH6,
            PriceChangeInterval.YEAR,
            -> {
                resourceReference(
                    R.string.common_range_with_space,
                    wrappedList(
                        stringReference(
                            startTimestamp.formatAsDateTime(MarketsDateTimeFormatters.dateFormatter),
                        ),
                        resourceReference(R.string.common_now),
                    ),
                )
            }
            PriceChangeInterval.ALL_TIME -> resourceReference(R.string.common_all)
        }
    }

    internal fun formatDateByIntervalWithMarker(
        interval: PriceChangeInterval,
        markerTimestamp: BigDecimal,
    ): TextReference {
        return when (interval) {
            PriceChangeInterval.H24,
            PriceChangeInterval.WEEK,
            PriceChangeInterval.MONTH,
            PriceChangeInterval.MONTH3,
            -> {
                resourceReference(
                    R.string.common_range_with_space,
                    wrappedList(
                        stringReference(
                            markerTimestamp.toLong().formatAsDateTime(MarketsDateTimeFormatters.dateTimeMMMFormatter),
                        ),
                        resourceReference(R.string.common_now),
                    ),
                )
            }
            PriceChangeInterval.MONTH6,
            PriceChangeInterval.YEAR,
            PriceChangeInterval.ALL_TIME,
            -> {
                resourceReference(
                    R.string.common_range_with_space,
                    wrappedList(
                        stringReference(
                            markerTimestamp.toLong().formatAsDateTime(MarketsDateTimeFormatters.dateFormatter),
                        ),
                        resourceReference(R.string.common_now),
                    ),
                )
            }
        }
    }

    @Suppress("MagicNumber")
    fun getStartTimestampByInterval(interval: PriceChangeInterval, currentTimestamp: Long): Long {
        return when (interval) {
            PriceChangeInterval.H24 -> currentTimestamp - H24_MILLIS
            PriceChangeInterval.WEEK -> currentTimestamp - WEEK_MILLIS
            PriceChangeInterval.MONTH -> DateTime(currentTimestamp, DateTimeZone.UTC).minusMonths(1).millis
            PriceChangeInterval.MONTH3 -> DateTime(currentTimestamp, DateTimeZone.UTC).minusMonths(3).millis
            PriceChangeInterval.MONTH6 -> DateTime(currentTimestamp, DateTimeZone.UTC).minusMonths(6).millis
            PriceChangeInterval.YEAR -> DateTime(currentTimestamp, DateTimeZone.UTC).minusYears(1).millis
            PriceChangeInterval.ALL_TIME -> 0
        }
    }
}