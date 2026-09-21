package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `bff-v2/v1/account/bank-accounts` — the recipients saved for bank off-ramp withdrawals.
 *
 * Everything below the account id is optional: the converter keeps every recipient it can identify — a name and
 * an account number — and drops only the ones it cannot tell apart.
 */
@JsonClass(generateAdapter = true)
data class BankAccountsResponse(
    @Json(name = "result") val result: List<BankAccount>?,
) {

    @JsonClass(generateAdapter = true)
    data class BankAccount(
        @Json(name = "id") val id: String,
        @Json(name = "display_name") val displayName: String?,
        @Json(name = "details") val details: Details?,
        @Json(name = "created_at") val createdAt: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Details(
        @Json(name = "currency") val currency: String?,
        @Json(name = "rail") val rail: String?,
        @Json(name = "third_party") val isThirdParty: Boolean?,
        @Json(name = "beneficiary") val beneficiary: Beneficiary?,
        @Json(name = "beneficiary_address") val beneficiaryAddress: Address?,
        @Json(name = "bank_name") val bankName: String?,
        @Json(name = "bank_address") val bankAddress: Address?,
        @Json(name = "account_number") val accountNumber: String?,
        @Json(name = "routing_number") val routingNumber: String?,
        @Json(name = "bank_account_type") val bankAccountType: String?,
    )

    /** [businessName] carries the company for a business beneficiary; [firstName] and [lastName] an individual. */
    @JsonClass(generateAdapter = true)
    data class Beneficiary(
        @Json(name = "type") val type: String?,
        @Json(name = "first_name") val firstName: String?,
        @Json(name = "last_name") val lastName: String?,
        @Json(name = "business_name") val businessName: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Address(
        @Json(name = "line1") val line1: String?,
        @Json(name = "line2") val line2: String?,
        @Json(name = "city") val city: String?,
        @Json(name = "region") val region: String?,
        @Json(name = "postal_code") val postalCode: String?,
        @Json(name = "country_code") val countryCode: String?,
    )
}