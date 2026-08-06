package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Per-transaction cashback on a spend transaction. `null` when cashback is not applicable
 * (customer on the cashback ignore list — `fraud`/`disabled` — or a non-spend transaction).
 *
 * Shared contract across the transaction endpoints (history list, single transaction) and the
 * cashback-details endpoint.
 */
@JsonClass(generateAdapter = true)
data class TransactionCashbackResponse(
    @Json(name = "status") val status: String, // "estimated" | "confirmed" | "excluded" | "awaiting_calculation"
    // USD, negative for refunds; null when status is "awaiting_calculation"
    @Json(name = "amount") val amount: BigDecimal? = null,
    @Json(name = "currency") val currency: String? = null, // null when status is "awaiting_calculation"
    @Json(name = "cap_trimmed") val isCapTrimmed: Boolean? = null,
    // "mcc_excluded" | "monthly_cap_reached" | "customer_blocklisted" | "merchant_country_excluded"
    @Json(name = "exclusion_reason") val exclusionReason: String? = null,
    @Json(name = "promotion_ids") val promotionIds: List<String>? = null,
)