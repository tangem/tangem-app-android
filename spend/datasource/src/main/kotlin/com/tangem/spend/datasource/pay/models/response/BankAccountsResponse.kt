package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `bff-v2/v1/account/bank-accounts` — the recipients saved for bank off-ramp withdrawals.
 *
 * Everything below the account id is optional: a recipient whose details the backend cannot fill is still
 * listed, and the converter drops it rather than showing a half-built recipient.
 */
@Serializable
data class BankAccountsResponse(
    @SerialName("result") val result: List<BankAccount>?,
) {

    @Serializable
    data class BankAccount(
        @SerialName("id") val id: String,
        @SerialName("display_name") val displayName: String?,
        @SerialName("details") val details: Details?,
        @SerialName("created_at") val createdAt: String?,
    )

    @Serializable
    data class Details(
        @SerialName("currency") val currency: String?,
        @SerialName("rail") val rail: String?,
        @SerialName("third_party") val isThirdParty: Boolean?,
        @SerialName("beneficiary") val beneficiary: Beneficiary?,
        @SerialName("beneficiary_address") val beneficiaryAddress: Address?,
        @SerialName("bank_name") val bankName: String?,
        @SerialName("bank_address") val bankAddress: Address?,
        @SerialName("account_number") val accountNumber: String?,
        @SerialName("routing_number") val routingNumber: String?,
        @SerialName("bank_account_type") val bankAccountType: String?,
    )

    /** [businessName] carries the company for a business beneficiary; [firstName] and [lastName] an individual. */
    @Serializable
    data class Beneficiary(
        @SerialName("type") val type: String?,
        @SerialName("first_name") val firstName: String?,
        @SerialName("last_name") val lastName: String?,
        @SerialName("business_name") val businessName: String?,
    )

    @Serializable
    data class Address(
        @SerialName("line1") val line1: String?,
        @SerialName("line2") val line2: String?,
        @SerialName("city") val city: String?,
        @SerialName("region") val region: String?,
        @SerialName("postal_code") val postalCode: String?,
        @SerialName("country_code") val countryCode: String?,
    )
}