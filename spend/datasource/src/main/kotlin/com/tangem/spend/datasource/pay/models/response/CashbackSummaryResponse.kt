package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response from `GET /v1/customer/cashback/summary`.
 *
 * When [cashbackProgramStatus] is not `enabled`, the amount/period fields may be omitted.
 */
@JsonClass(generateAdapter = true)
data class CashbackSummaryResponse(
    @Json(name = "cashback_program_status") val cashbackProgramStatus: String,
    @Json(name = "cashback_display_mode") val cashbackDisplayMode: String?,
    @Json(name = "period") val period: Period?,
    @Json(name = "confirmed_amount") val confirmedAmount: BigDecimal?,
    @Json(name = "pending_amount") val pendingAmount: BigDecimal?,
    @Json(name = "currency") val currency: String?,
    @Json(name = "payout_currency") val payoutCurrency: String?,
    @Json(name = "payout_network") val payoutNetwork: String?,
) {

    @JsonClass(generateAdapter = true)
    data class Period(
        @Json(name = "year") val year: Int,
        @Json(name = "month") val month: Int,
        @Json(name = "payout_start_date") val payoutStartDate: String?,
        @Json(name = "payout_end_date") val payoutEndDate: String?,
    )
}