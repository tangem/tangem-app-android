package com.tangem.core.ui.utils

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
    val timeFormatter by lazy {
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

    val dateFormatter by lazy {
        DateTimeFormatterBuilder()
            .appendDayOfMonth(1)
            .appendLiteral(' ')
            .appendMonthOfYearShortText()
            .appendLiteral(", ")
            .appendYear(4, 4)
            .toFormatter()
            .withLocale(Locale.getDefault())
    }

    fun formatTime(formatter: DateTimeFormatter = timeFormatter, time: DateTime): String {
        return formatter.print(time)
    }

    fun formatDate(formatter: DateTimeFormatter = dateFormatter, date: DateTime): String {
        return formatter.print(date)
    }
}