package com.tangem.features.feed.ui.utils

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.FormattedDate
import com.tangem.core.ui.utils.getFormattedDate
import com.tangem.features.feed.impl.R
import com.tangem.utils.StringsSigns
import org.joda.time.DateTime

internal fun mapFormattedDate(createdAt: String): TextReference {
    val formattedDate = getFormattedDate(
        createdAt = createdAt,
        now = DateTime.now(),
    )
    return when (formattedDate) {
        is FormattedDate.FullDate -> TextReference.Str(value = formattedDate.date)
        is FormattedDate.HoursAgo -> TextReference.PluralRes(
            id = R.plurals.news_published_hours_ago,
            count = formattedDate.hours,
            formatArgs = wrappedList(formattedDate.hours),
        )
        is FormattedDate.MinutesAgo -> TextReference.PluralRes(
            id = R.plurals.news_published_minutes_ago,
            count = formattedDate.minutes,
            formatArgs = wrappedList(formattedDate.minutes),
        )
        is FormattedDate.Today -> TextReference.Combined(
            refs = WrappedList(
                data = listOf(
                    TextReference.Res(R.string.common_today),
                    TextReference.Str(StringsSigns.COMA_SIGN),
                    TextReference.Str(StringsSigns.WHITE_SPACE),
                    TextReference.Str(formattedDate.time),
                ),
            ),
        )
    }
}