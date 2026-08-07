package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.utils.DateTimeFormatters
import org.joda.time.DateTime

internal class TangemPayCashbackDateFormatter {

    fun formatMonth(year: Int, month: Int): String =
        DateTimeFormatters.formatDate(DateTime(year, month, 1, 0, 0), DateTimeFormatters.dateMMMM)

    fun formatShortMonth(year: Int, month: Int): String =
        DateTimeFormatters.formatDate(DateTime(year, month, 1, 0, 0), DateTimeFormatters.dateMMM)

    fun formatMonthDay(date: DateTime): String = DateTimeFormatters.formatDate(date, DateTimeFormatters.dateMMMMd)

    fun formatNumericDate(date: DateTime): String = DateTimeFormatters.formatDate(date, DateTimeFormatters.dateDDMMYYYY)

    fun formatWindow(start: DateTime, end: DateTime): String {
        val isSameMonth = start.year == end.year && start.monthOfYear == end.monthOfYear
        return if (isSameMonth) {
            "${formatMonthDay(start)}–${end.dayOfMonth}"
        } else {
            "${formatMonthDay(start)} – ${formatMonthDay(end)}"
        }
    }
}