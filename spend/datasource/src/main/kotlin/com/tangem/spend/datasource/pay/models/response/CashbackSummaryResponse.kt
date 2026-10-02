package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET /v1/customer/cashback/summary`.
 */
@Serializable
data class CashbackSummaryResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("cashback_program_status") val cashbackProgramStatus: String,
        @SerialName("cashback_display_mode") val cashbackDisplayMode: String,
        @SerialName("period") val period: Period,
        @SerialName("payout_currency") val payoutCurrency: String?,
        @SerialName("confirmed_amount") val confirmedAmount: SerializedBigDecimal,
        @SerialName("total_earned_amount") val totalEarnedAmount: SerializedBigDecimal?,
        @SerialName("previous_payout_end_date") val previousPayoutEndDate: String?,
        @SerialName("previous_payout_amount") val previousPayoutAmount: SerializedBigDecimal?,
        @SerialName("currency") val currency: String,
    )

    @Serializable
    data class Period(
        @SerialName("year") val year: Int,
        @SerialName("month") val month: Int,
        @SerialName("payout_start_date") val payoutStartDate: String?,
        @SerialName("payout_end_date") val payoutEndDate: String?,
    )
}