package com.tangem.domain.marketing.models

import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Display window gate: hidden when [now] is before `startAt` or after `endAt`,
 * both bounds inclusive and each optional. A bound that is not a valid ISO-8601 instant is reported via
 * [onInvalidDate] and treated as absent, so a backend format slip does not hide the banner.
 */
fun MarketingCampaign.isActiveAt(now: Instant, onInvalidDate: (String) -> Unit = {}): Boolean {
    val start = startAt.parseInstant(onInvalidDate)
    val end = endAt.parseInstant(onInvalidDate)
    if (start != null && now.isBefore(start)) return false
    if (end != null && now.isAfter(end)) return false
    return true
}

/**
 * Returns the earliest future transition instant after [now] when this campaign's visibility will change:
 * - at `startAt` (when it transitions from inactive to active)
 * - at `endAt + 1ms` (when it transitions from active to inactive, since bounds are inclusive)
 * Returns null if this campaign has no future visibility transitions.
 */
fun MarketingCampaign.nextWindowTransition(now: Instant): Instant? {
    val start = startAt.parseInstant()
    val end = endAt.parseInstant()?.plusMillis(1)
    return sequenceOf(start, end)
        .filterNotNull()
        .filter { it.isAfter(now) }
        .minOrNull()
}

/**
 * Returns the earliest future visibility transition across all campaigns in the list after [now],
 * or null if no campaign has a future transition.
 */
fun List<MarketingCampaign>.nextWindowTransition(now: Instant): Instant? {
    return asSequence()
        .mapNotNull { it.nextWindowTransition(now) }
        .minOrNull()
}

fun MarketingCampaign.validateDates(onInvalidDate: (String) -> Unit) {
    startAt.parseInstant(onInvalidDate)
    endAt.parseInstant(onInvalidDate)
}

internal fun String?.parseInstant(onInvalidDate: (String) -> Unit = {}): Instant? {
    if (this == null) return null
    return try {
        Instant.parse(this)
    } catch (_: DateTimeParseException) {
        onInvalidDate(this)
        null
    }
}