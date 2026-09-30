package com.tangem.spend.datasource.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for creating a bank off-ramp withdrawal order (`BANK_OFFRAMP_RAIN`).
 *
 * Distinct from the card-issue [OrderRequest], which requires a `customer_wallet_address` and a
 * `specification_name` this order carries none of. The recipient is described either by [Data.bankAccountId] —
 * one already saved on the backend — or by the whole [Data.bankAccount]; the unused one stays null and Moshi
 * leaves it out of the body.
 *
 * [idempotencyKey] is sent here as well as in the `Idempotency-Key` header: the backend reads the body key on
 * this endpoint today, the header is what its contract asks for.
 */
@JsonClass(generateAdapter = true)
data class BankOfframpPlaceOrderRequest(
    @Json(name = "data") val data: Data,
    @Json(name = "idempotency_key") val idempotencyKey: String,
) {

    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "chain_id") val chainId: Long,
        @Json(name = "amount_in_cents") val amountInCents: Long,
        @Json(name = "token_contract_address") val tokenContractAddress: String,
        @Json(name = "bank_account_id") val bankAccountId: String? = null,
        @Json(name = "bank_account") val bankAccount: BankAccount? = null,
        @Json(name = "type") val type: String = "BANK_OFFRAMP_RAIN",
    )

    /** `display_name` is not sent: the backend names a recipient created by the order after its beneficiary. */
    @JsonClass(generateAdapter = true)
    data class BankAccount(
        @Json(name = "details") val details: Details,
    )

    /** Every field is required here, unlike the saved recipients the backend sends back. */
    @JsonClass(generateAdapter = true)
    data class Details(
        @Json(name = "currency") val currency: String,
        @Json(name = "rail") val rail: String,
        @Json(name = "third_party") val isThirdParty: Boolean,
        @Json(name = "beneficiary") val beneficiary: Beneficiary,
        @Json(name = "beneficiary_address") val beneficiaryAddress: Address,
        @Json(name = "bank_name") val bankName: String,
        @Json(name = "bank_address") val bankAddress: Address,
        @Json(name = "account_number") val accountNumber: String,
        @Json(name = "routing_number") val routingNumber: String,
        @Json(name = "bank_account_type") val bankAccountType: String,
    )

    /** [businessName] carries the company for a business beneficiary; [firstName] and [lastName] an individual. */
    @JsonClass(generateAdapter = true)
    data class Beneficiary(
        @Json(name = "type") val type: String,
        @Json(name = "first_name") val firstName: String? = null,
        @Json(name = "last_name") val lastName: String? = null,
        @Json(name = "business_name") val businessName: String? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Address(
        @Json(name = "line1") val line1: String,
        @Json(name = "line2") val line2: String? = null,
        @Json(name = "city") val city: String,
        @Json(name = "region") val region: String,
        @Json(name = "postal_code") val postalCode: String,
        @Json(name = "country_code") val countryCode: String,
    )
}