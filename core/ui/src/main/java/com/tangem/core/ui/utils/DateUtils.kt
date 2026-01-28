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

/**
 * Parses an ISO 8601 date string and compares it to the current UTC time.
 *

 * @param now The current date to compare against.
 * @return A [FormattedDate] subclass.
 */
@Suppress("MagicNumber")
fun getFormattedDate(createdAt: String, now: DateTime): FormattedDate {
    val pastDateUtc = try {
        DateTime.parse(createdAt)
    } catch (_: Exception) {
        return FormattedDate.FullDate(createdAt)
    }

    val pastDateLocal = pastDateUtc.withZone(DateTimeZone.getDefault())
    val isToday = pastDateLocal.isToday()

    val diffInMillis = now.millis - pastDateUtc.millis
    val diffInMinutes = diffInMillis / (1000 * 60)
    val diffInHours = diffInMillis / (1000 * 60 * 60)

    return when {
        diffInMinutes < 1 -> FormattedDate.MinutesAgo(1)
        diffInMinutes < 60 -> FormattedDate.MinutesAgo(diffInMinutes.toInt())
        diffInHours < 12 && isToday -> FormattedDate.HoursAgo(diffInHours.toInt())
        isToday -> {
            val timeString = DateTimeFormatters.timeFormatter.print(pastDateLocal)
            FormattedDate.Today(timeString)
        }
        else -> {
            val dateString = DateTimeFormatters.localFullDate.print(pastDateLocal)
            FormattedDate.FullDate(dateString)
        }
    }
}

/**
 * Representing different formatted date representations.
 */
sealed class FormattedDate {
    data class MinutesAgo(val minutes: Int) : FormattedDate()
    data class HoursAgo(val hours: Int) : FormattedDate()
    data class Today(val time: String) : FormattedDate()
    data class FullDate(val date: String) : FormattedDate()
}