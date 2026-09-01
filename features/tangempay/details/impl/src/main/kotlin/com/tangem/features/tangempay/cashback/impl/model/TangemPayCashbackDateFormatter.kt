package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.utils.DateTimeFormatters
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

internal class TangemPayCashbackDateFormatter {

    fun formatMonth(year: Int, month: Int): String =
        DateTimeFormatters.formatStandaloneMonth(DateTime(year, month, 1, 0, 0))

    fun formatShortMonth(year: Int, month: Int): String =
        DateTimeFormatters.formatStandaloneShortMonth(DateTime(year, month, 1, 0, 0))

    fun formatMonthDay(date: DateTime): String = DateTimeFormatters.formatDate(date, DateTimeFormatters.dateDMMM)

    fun formatNumericDate(date: DateTime): String = NUMERIC_DATE_FORMATTER.print(date)

    fun formatWindow(start: DateTime?, end: DateTime?): String? {
        if (start == null || end == null) return null
        return DateTimeFormatters.formatDateRange(start, end, MONTH_DAY_SKELETON)
    }

    private companion object {
        const val MONTH_DAY_SKELETON = "MMMMd"

        // ICU's best-pattern lookup localizes the numeric skeleton to "MM/dd/yyyy" in some locales,
        // while this date must always be dot-separated ([REDACTED_TASK_KEY])
        val NUMERIC_DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy")
    }
}