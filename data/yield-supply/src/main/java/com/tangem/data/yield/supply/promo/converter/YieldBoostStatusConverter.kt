package com.tangem.data.yield.supply.promo.converter

import com.tangem.datasource.api.promotion.models.YieldBoostStatusResponse
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import kotlinx.datetime.Instant

internal object YieldBoostStatusConverter {

    private const val STATUS_NOT_STARTED = "notstarted"
    private const val STATUS_ACTIVE = "active"
    private const val STATUS_COMPLETED = "completed"
    private const val STATUS_DISQUALIFIED = "disqualified"

    private const val REASON_FROD = "frod"
    private const val REASON_LESS_THAN_1_USD = "less1usd"
    private const val REASON_CLOSED = "closed"

    fun convert(dto: YieldBoostStatusResponse): YieldBoostStatus = when (dto.promoEnrollmentStatus.lowercase()) {
        STATUS_ACTIVE, STATUS_COMPLETED -> dto.toEnrolled()
        STATUS_DISQUALIFIED -> YieldBoostStatus.Disqualified(reason = dto.disqualificationReason.toReason())
        STATUS_NOT_STARTED -> YieldBoostStatus.NotStarted
        else -> YieldBoostStatus.NotStarted // forward-compat: unknown status → treat as NotStarted
    }

    /**
     * Backend `"active"` / `"completed"` → [YieldBoostStatus.Enrolled].
     *
     * An unparseable / missing `qualificationEndDate` is kept as `null` (block hidden) — never downgraded to
     * [YieldBoostStatus.NotStarted], which would re-prompt an already-enrolled user to join.
     */
    private fun YieldBoostStatusResponse.toEnrolled(): YieldBoostStatus.Enrolled = YieldBoostStatus.Enrolled(
        tokenName = tokenName.orEmpty(),
        networkId = networkId.orEmpty(),
        moduleAddress = moduleAddress.orEmpty(),
        userAddress = userAddress.orEmpty(),
        contractAddress = contractAddress.orEmpty(),
        qualificationEndDate = qualificationEndDate?.let { runCatching { Instant.parse(it) }.getOrNull() },
    )

    private fun String?.toReason(): YieldBoostStatus.Disqualified.Reason = when (this?.lowercase()) {
        REASON_FROD -> YieldBoostStatus.Disqualified.Reason.FROD
        REASON_LESS_THAN_1_USD -> YieldBoostStatus.Disqualified.Reason.LESS_THAN_1_USD
        REASON_CLOSED -> YieldBoostStatus.Disqualified.Reason.CLOSED
        else -> YieldBoostStatus.Disqualified.Reason.UNKNOWN
    }
}