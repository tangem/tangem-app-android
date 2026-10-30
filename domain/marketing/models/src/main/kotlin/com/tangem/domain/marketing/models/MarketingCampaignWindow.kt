package com.tangem.domain.marketing.models

import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Display window gate (mirrors iOS `isActive`): hidden when [now] is before `startAt` or after `endAt`,
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

private fun String?.parseInstant(onInvalidDate: (String) -> Unit): Instant? {
    if (this == null) return null
    return try {
        Instant.parse(this)
    } catch (_: DateTimeParseException) {
        onInvalidDate(this)
        null
    }
}