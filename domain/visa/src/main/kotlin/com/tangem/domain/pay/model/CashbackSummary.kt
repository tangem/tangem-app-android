package com.tangem.domain.pay.model

/**
 * Customer cashback summary from `GET /v1/customer/cashback/summary`.
 *
 * Drives the cashback widget on the Payment account screen and the Cashback screen header.
 */
sealed interface CashbackSummary {

    /** Cashback is active for the customer. */
    data class Enabled(
        val displayMode: CashbackDisplayMode,
        val cashback: TangemPayCashback,
    ) : CashbackSummary

    /** Customer blocked due to fraud; client shows the "Cashback deactivated" banner. */
    data object Deactivated : CashbackSummary

    /** Cashback not available for this cohort/region; client hides all cashback UI. */
    data object Disabled : CashbackSummary

    /** Unrecognized program status; treated as no cashback UI. */
    data object Unknown : CashbackSummary
}