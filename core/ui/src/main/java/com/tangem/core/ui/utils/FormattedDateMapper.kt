package com.tangem.core.ui.utils

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.utils.StringsSigns
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/**
 * Maps an ISO 8601 date string into a [TextReference] with a human-friendly "time ago" label.
 */
fun mapFormattedDate(createdAt: String, now: DateTime = DateTime.now()): TextReference {
    val formattedDate = runCatching {
        getFormattedDate(createdAt = createdAt, now = now)
    }.getOrElse { FormattedDate.FullDate(createdAt) }

    return formattedDate.toTextReference()
}

/**
 * Maps an epoch millisecond [timestamp] into a [TextReference] with a human-friendly "time ago" label.
 */
fun mapFormattedDate(timestamp: Long, now: DateTime = DateTime.now()): TextReference {
    val formattedDate = runCatching {
        getFormattedDate(pastDateUtc = DateTime(timestamp, DateTimeZone.UTC), now = now)
    }.getOrElse { FormattedDate.FullDate(timestamp.toString()) }

    return formattedDate.toTextReference()
}

private fun FormattedDate.toTextReference(): TextReference = when (this) {
    is FormattedDate.FullDate -> TextReference.Str(value = date)
    is FormattedDate.HoursAgo -> TextReference.PluralRes(
        id = R.plurals.common_hours_time_ago,
        count = hours,
        formatArgs = wrappedList(hours),
    )
    is FormattedDate.MinutesAgo -> TextReference.PluralRes(
        id = R.plurals.common_minutes_time_ago,
        count = minutes,
        formatArgs = wrappedList(minutes),
    )
    is FormattedDate.Today -> TextReference.Combined(
        refs = WrappedList(
            data = listOf(
                TextReference.Res(R.string.common_today),
                TextReference.Str(StringsSigns.COMA_SIGN),
                TextReference.Str(StringsSigns.WHITE_SPACE),
                TextReference.Str(time),
            ),
        ),
    )
}