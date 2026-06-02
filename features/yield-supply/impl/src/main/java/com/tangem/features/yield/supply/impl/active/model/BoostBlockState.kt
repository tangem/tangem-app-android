package com.tangem.features.yield.supply.impl.active.model

import kotlinx.datetime.Instant

/** What the boost block on the active screen should display, derived solely from the qualification end date. */
internal sealed interface BoostBlockState {

    /** Qualification period is still running — show the countdown. */
    data class DaysLeft(val days: Int) : BoostBlockState

    /** Qualification period is over — show the awaiting-payout copy. */
    data object AwaitingPayout : BoostBlockState

    /** No qualification end date — show nothing. */
    data object Hidden : BoostBlockState
}

internal fun resolveBoostBlockState(qualificationEndDate: Instant?, now: Instant): BoostBlockState = when {
    qualificationEndDate == null -> BoostBlockState.Hidden
    now >= qualificationEndDate -> BoostBlockState.AwaitingPayout
    else -> BoostBlockState.DaysLeft(days = (qualificationEndDate - now).inWholeDays.toInt())
}