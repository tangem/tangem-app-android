package com.tangem.spend.datasource.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for creating a Virtual Account on-ramp order (VA MVP0, TWI-1638).
 *
 * `wallet_address` is the customer's managing (collateral-managing) wallet address; `payment_account_address`
 * is the existing collateral address. Distinct from the card-issue [OrderRequest] contract.
 */
@JsonClass(generateAdapter = true)
data class VirtualAccountOrderRequest(
    @Json(name = "data") val data: Data,
    @Json(name = "idempotency_key") val idempotencyKey: String,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "deposit_address") val depositAddress: String,
        @Json(name = "type") val type: String = "ACCOUNT_ISSUE_VIRTUAL_RAIN",
        @Json(name = "specification_name") val specificationName: String = "SP_000006",
    )
}