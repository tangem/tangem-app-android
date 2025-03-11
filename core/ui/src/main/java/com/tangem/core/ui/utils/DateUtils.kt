package com.tangem.core.ui.utils

import android.text.format.DateUtils
import com.tangem.utils.extensions.isToday
import com.tangem.utils.extensions.isYesterday
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatter

/**
 * If [this] timestamp is today or yesterday, returns relative date,
 * otherwise returns formatting date.
 */
fun Long.toDateFormatWithTodayYesterday(formatter: DateTimeFormatter = DateTimeFormatters.dateFormatter): String {
    val localDate = DateTime(this, DateTimeZone.getDefault())
    return if (localDate.isToday() || localDate.isYesterday()) {
        DateUtils.getRelativeTimeSpanString(
            this,
            DateTime.now().millis,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
    } else {
        DateTimeFormatters.formatDate(formatter = formatter, date = localDate)
    }
}

/**
 * Returns formatted time according to [formatter].
 */
fun Long.toTimeFormat(formatter: DateTimeFormatter = DateTimeFormatters.timeFormatter): String {
    return formatAsDateTime(formatter)
}

/**
 * Returns formatted date-time according to [formatter].
 */
fun Long.formatAsDateTime(formatter: DateTimeFormatter): String {
    return DateTimeFormatters.formatDate(date = DateTime(this, DateTimeZone.getDefault()), formatter = formatter)
}