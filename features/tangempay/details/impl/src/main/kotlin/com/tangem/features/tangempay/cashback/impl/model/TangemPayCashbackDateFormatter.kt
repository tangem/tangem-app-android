package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.utils.DateTimeFormatters
import org.joda.time.DateTime

internal class TangemPayCashbackDateFormatter {

    fun formatMonth(year: Int, month: Int): String =
        DateTimeFormatters.formatDate(DateTime(year, month, 1, 0, 0), DateTimeFormatters.dateMMMM)

    fun formatShortMonth(year: Int, month: Int): String =
        DateTimeFormatters.formatDate(DateTime(year, month, 1, 0, 0), DateTimeFormatters.dateMMM)

    fun formatMonthDay(date: DateTime): String = DateTimeFormatters.formatDate(date, DateTimeFormatters.dateDMMM)

    fun formatNumericDate(date: DateTime): String = DateTimeFormatters.formatDate(date, DateTimeFormatters.dateDDMMYYYY)

    fun formatWindow(start: DateTime, end: DateTime): String =
        DateTimeFormatters.formatDateRange(start, end, MONTH_DAY_SKELETON)

    private companion object {
        const val MONTH_DAY_SKELETON = "MMMMd"
    }
}