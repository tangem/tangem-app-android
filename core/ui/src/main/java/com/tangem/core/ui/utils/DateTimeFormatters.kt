package com.tangem.core.ui.utils

import android.text.format.DateFormat
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import java.util.Locale

@Suppress("MagicNumber")
object DateTimeFormatters {

    /**
     * Two SS means, SHORT style for date and time.
     * If pattern contains "a", it means time is in 12 hour format.
     * [Documentation](https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html)
     */
    val timeFormatter: DateTimeFormatter by lazy {
        val is12HourFormat = DateTimeFormat.patternForStyle("SS", Locale.getDefault()).contains("a")
        if (is12HourFormat) {
            DateTimeFormatterBuilder()
                .appendClockhourOfHalfday(1)
                .appendLiteral(':')
                .appendMinuteOfHour(2)
                .appendLiteral(" ")
                .appendHalfdayOfDayText()
                .toFormatter()
                .withLocale(Locale.getDefault())
        } else {
            DateTimeFormatterBuilder()
                .appendHourOfDay(1)
                .appendLiteral(':')
                .appendMinuteOfHour(2)
                .toFormatter()
                .withLocale(Locale.getDefault())
        }
    }

    val dateFormatter: DateTimeFormatter by lazy {
        DateTimeFormatterBuilder()
            .appendDayOfMonth(1)
            .appendLiteral(' ')
            .appendMonthOfYearShortText()
            .appendLiteral(", ")
            .appendYear(4, 4)
            .toFormatter()
            .withLocale(Locale.getDefault())
    }

    val dateDDMMYYYY: DateTimeFormatter by lazy {
        DateTimeFormatterBuilder()
            .appendPattern("dd.MM.yyyy")
            .toFormatter()
            .withLocale(Locale.getDefault())
    }

    /**
     * In API version < 24, there may be some problems with getting the best date and time format pattern.
     */
    val dateMMMdd: DateTimeFormatter by lazy {
        getBestFormatterBySkeleton("dd MMM")
    }

    val dateYYYY: DateTimeFormatter by lazy {
        getBestFormatterBySkeleton("yyyy")
    }

    val dateTimeFormatter: DateTimeFormatter by lazy {
        DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
    }

    fun formatDate(date: DateTime, formatter: DateTimeFormatter = dateFormatter): String {
        return formatter.print(date)
    }

    fun getBestFormatterBySkeleton(skeleton: String): DateTimeFormatter {
        return DateTimeFormatterBuilder()
            .appendPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton))
            .toFormatter()
            .withLocale(Locale.getDefault())
    }
}