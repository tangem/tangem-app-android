package com.tangem.common.ui.expressStatus.state

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList

/**
 * Builds express status subtitle by combining [activeStatus] with formatted [date].
 *
 * Separator rules:
 * - Relative date (MinutesAgo / HoursAgo PluralRes) — " ~ ".
 * - Absolute date (Today / FullDate) — " "; Today's leading Res is decapitalized when a status
 *   prefix is present.
 * - Either side empty — the other is returned as is (no modifications).
 */
fun buildExpressStatusSubtitle(activeStatus: TextReference, date: TextReference): TextReference {
    val hasStatus = !activeStatus.isNullOrEmpty()
    val hasDate = !date.isNullOrEmpty()
    return when {
        !hasStatus && !hasDate -> TextReference.EMPTY
        hasStatus && !hasDate -> activeStatus
        !hasStatus && hasDate -> date
        else -> combineWithStatus(activeStatus, date)
    }
}

private fun combineWithStatus(status: TextReference, date: TextReference): TextReference {
    val shouldUseTilde = date.isRelativeTimeAgo()
    val separator = if (shouldUseTilde) stringReference(value = " ~ ") else stringReference(value = " ")
    val datePart = if (shouldUseTilde) date else date.decapitalizeToday()
    return TextReference.Combined(refs = wrappedList(status, separator, datePart))
}

private fun TextReference.isRelativeTimeAgo(): Boolean {
    return this is TextReference.PluralRes &&
        (id == R.plurals.common_minutes_time_ago || id == R.plurals.common_hours_time_ago)
}

private fun TextReference.decapitalizeToday(): TextReference {
    if (this !is TextReference.Combined) return this
    val patched = refs.data.map { ref ->
        if (ref is TextReference.Res && ref.id == R.string.common_today) {
            ref.copy(shouldDecapitalize = true)
        } else {
            ref
        }
    }
    return TextReference.Combined(refs = WrappedList(data = patched))
}