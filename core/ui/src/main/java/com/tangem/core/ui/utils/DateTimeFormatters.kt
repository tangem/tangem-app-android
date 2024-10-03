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
     * Determine if the time is in 12-hour format ("10:00 PM") for the current locale.
     */
    val is12HourFormat by lazy {
        /**
         * Two SS means, SHORT style for date and time.
         * If pattern contains "a", it means time is in 12 hour format.
         * [Documentation](https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html)
         */
        DateTimeFormat.patternForStyle("SS", Locale.getDefault()).contains("a")
    }

    /**
     * Example: "12:00 PM", "12:00"
     */
    val timeFormatter: DateTimeFormatter by lazy {
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
                .appendHourOfDay(2)
                .appendLiteral(':')
                .appendMinuteOfHour(2)
                .toFormatter()
                .withLocale(Locale.getDefault())
        }
    }

    /**
     * Example: "1 Jun, 2020", "1 Jun, 2020"
     */
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

    /**
     * Example: "31.06.2020", "06/31/2020"
     */
    val dateDDMMYYYY: DateTimeFormatter by lazy {
        getBestFormatterBySkeleton("dd.MM.yyyy")
    }

    /**
     * Example: "Jun 31, 2020", "31 Jun, 2020"
     */
    val dateMMMdd: DateTimeFormatter by lazy {
        getBestFormatterBySkeleton("MMM dd")
    }

    /**
     * Example: "2020"
     */
    val dateYYYY: DateTimeFormatter by lazy {
        getBestFormatterBySkeleton("yyyy")
    }

    /**
     * Example: "31.06.2020 12:00", "06/31/2020 12:00", "06/31/2020 12:00 PM"
     */
    val dateTimeFormatter: DateTimeFormatter by lazy {
        getBestFormatterBySkeleton("dd.MM.yyyy HH:mm")
    }

    fun formatDate(date: DateTime, formatter: DateTimeFormatter = dateFormatter): String {
        return formatter.print(date)
    }

    /**
     * Returns the best date and time format pattern for the given skeleton and the current locale.
     * (In API version < 24, there may be some problems with getting the best date and time format pattern.)
     *
     * @param skeleton The skeleton is an alternative to the pattern. The difference is that the pattern rigidly
     * defines the date/time format, while the skeleton specifies only the date/time components (year, month, day, etc.)
     * So the order of the components and separators (space, comma, etc.) is not taken into account.
     * @see [dateYYYY], [dateMMMdd], [dateDDMMYYYY], [dateTimeFormatter]
     */
    fun getBestFormatterBySkeleton(skeleton: String): DateTimeFormatter {
        val skeletonWithLocale = skeleton.replaceHourLetters()

        return DateTimeFormatterBuilder()
            .appendPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeletonWithLocale))
            .toFormatter()
            .withLocale(Locale.getDefault())
    }

    private fun String.replaceHourLetters(): String {
        return if (is12HourFormat) {
            this.replace('H', 'h').replace('k', 'K')
        } else {
            this.replace('h', 'H').replace('K', 'k')
        }
    }
}