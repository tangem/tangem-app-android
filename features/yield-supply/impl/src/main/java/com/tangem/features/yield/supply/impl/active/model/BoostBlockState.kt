package com.tangem.features.yield.supply.impl.active.model

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

/** How long the awaiting-payout copy stays visible after the qualification period ends, before the block is hidden. */
private val AWAITING_PAYOUT_WINDOW = 14.days

/** What the boost block on the active screen should display, derived solely from the qualification end date. */
internal sealed interface BoostBlockState {

    /** Qualification period is still running — show the countdown. */
    data class DaysLeft(val days: Int) : BoostBlockState

    /** Qualification period is over — show the awaiting-payout copy. */
    data object AwaitingPayout : BoostBlockState

    /** No qualification end date, or the awaiting-payout window has elapsed — show nothing. */
    data object Hidden : BoostBlockState
}

internal fun resolveBoostBlockState(qualificationEndDate: Instant?, now: Instant): BoostBlockState = when {
    qualificationEndDate == null -> BoostBlockState.Hidden
    now >= qualificationEndDate + AWAITING_PAYOUT_WINDOW -> BoostBlockState.Hidden
    now >= qualificationEndDate -> BoostBlockState.AwaitingPayout
    else -> {
        val remaining = qualificationEndDate - now
        val fullDays = remaining.inWholeDays
        val daysLeft = if (remaining > fullDays.days) fullDays + 1 else fullDays
        BoostBlockState.DaysLeft(days = daysLeft.toInt())
    }
}