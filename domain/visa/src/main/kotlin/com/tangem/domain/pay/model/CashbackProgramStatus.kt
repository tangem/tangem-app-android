package com.tangem.domain.pay.model

/**
 * Cashback program availability for the customer, from `cashback_program_status`
 * in `GET /v1/customer/cashback/summary`.
 *
 * Unknown wire values resolve to [UNKNOWN].
 */
enum class CashbackProgramStatus {

    /** Cashback is active. */
    ENABLED,

    /** Customer blocked due to fraud behavior; client shows the "Cashback deactivated" banner. */
    DEACTIVATED,

    /** Cashback not available for this cohort/region; client silently hides all cashback UI. */
    DISABLED,

    /** Unrecognized status. */
    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String?): CashbackProgramStatus = when (value?.lowercase()) {
            "enabled" -> ENABLED
            "deactivated" -> DEACTIVATED
            "disabled", "unavailable" -> DISABLED
            else -> UNKNOWN
        }
    }
}