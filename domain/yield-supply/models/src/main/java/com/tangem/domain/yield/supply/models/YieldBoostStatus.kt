package com.tangem.domain.yield.supply.models

import kotlinx.datetime.Instant

sealed interface YieldBoostStatus {

    data object NotStarted : YieldBoostStatus

    /**
     * User is enrolled in the boost (backend `active` or `completed`).
     *
     * The boost block on the active screen is driven entirely by [qualificationEndDate], which the backend
     * computes as the end of the bonus-accrual period:
     *  - `null` — nothing is shown;
     *  - in the future — days left until the date;
     *  - reached / passed — awaiting payout.
     */
    data class Enrolled(
        val tokenName: String,
        val networkId: String,
        val moduleAddress: String,
        val userAddress: String,
        val contractAddress: String,
        val qualificationEndDate: Instant?,
    ) : YieldBoostStatus

    data class Disqualified(val reason: Reason) : YieldBoostStatus {

        enum class Reason { FROD, LESS_THAN_1_USD, CLOSED, UNKNOWN }
    }
}