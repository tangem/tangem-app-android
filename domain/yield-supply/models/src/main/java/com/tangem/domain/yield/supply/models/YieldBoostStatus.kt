package com.tangem.domain.yield.supply.models

import kotlinx.datetime.Instant

sealed interface YieldBoostStatus {

    data object NotStarted : YieldBoostStatus

    /** User entered boost, qualification period is still running. */
    data class Active(
        val tokenName: String,
        val networkId: String,
        val moduleAddress: String,
        val userAddress: String,
        val contractAddress: String,
        val activationDate: Instant,
        val qualificationEndDate: Instant,
    ) : YieldBoostStatus

    /** Boost has finished (backend `completed`). */
    data class Completed(
        val tokenName: String,
        val networkId: String,
        val moduleAddress: String,
        val userAddress: String,
        val contractAddress: String,
        val activationDate: Instant,
        val qualificationEndDate: Instant,
    ) : YieldBoostStatus

    data class Disqualified(val reason: Reason) : YieldBoostStatus {

        enum class Reason { FROD, LESS_THAN_1_USD, CLOSED, UNKNOWN }
    }
}