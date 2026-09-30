package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for creating a bank off-ramp withdrawal order (`BANK_OFFRAMP_RAIN`).
 *
 * Distinct from the card-issue [OrderRequest], which requires a `customer_wallet_address` and a
 * `specification_name` this order carries none of. The recipient is described either by [Data.bankAccountId] —
 * one already saved on the backend — or by the whole [Data.bankAccount]; the unused one stays null and is
 * left out of the body.
 *
 * [idempotencyKey] is sent here as well as in the `Idempotency-Key` header: the backend reads the body key on
 * this endpoint today, the header is what its contract asks for.
 */
@Serializable
data class BankOfframpPlaceOrderRequest(
    @SerialName("data") val data: Data,
    @SerialName("idempotency_key") val idempotencyKey: String,
) {

    @Serializable
    data class Data(
        @SerialName("chain_id") val chainId: Long,
        @SerialName("amount_in_cents") val amountInCents: Long,
        @SerialName("token_contract_address") val tokenContractAddress: String,
        @SerialName("bank_account_id") val bankAccountId: String? = null,
        @SerialName("bank_account") val bankAccount: BankAccount? = null,
        @SerialName("type") val type: String = "BANK_OFFRAMP_RAIN",
    )

    /** `display_name` is not sent: the backend names a recipient created by the order after its beneficiary. */
    @Serializable
    data class BankAccount(
        @SerialName("details") val details: Details,
    )

    /** Every field is required here, unlike the saved recipients the backend sends back. */
    @Serializable
    data class Details(
        @SerialName("currency") val currency: String,
        @SerialName("rail") val rail: String,
        @SerialName("third_party") val isThirdParty: Boolean,
        @SerialName("beneficiary") val beneficiary: Beneficiary,
        @SerialName("beneficiary_address") val beneficiaryAddress: Address,
        @SerialName("bank_name") val bankName: String,
        @SerialName("bank_address") val bankAddress: Address,
        @SerialName("account_number") val accountNumber: String,
        @SerialName("routing_number") val routingNumber: String,
        @SerialName("bank_account_type") val bankAccountType: String,
    )

    /** [businessName] carries the company for a business beneficiary; [firstName] and [lastName] an individual. */
    @Serializable
    data class Beneficiary(
        @SerialName("type") val type: String,
        @SerialName("first_name") val firstName: String? = null,
        @SerialName("last_name") val lastName: String? = null,
        @SerialName("business_name") val businessName: String? = null,
    )

    @Serializable
    data class Address(
        @SerialName("line1") val line1: String,
        @SerialName("line2") val line2: String? = null,
        @SerialName("city") val city: String,
        @SerialName("region") val region: String,
        @SerialName("postal_code") val postalCode: String,
        @SerialName("country_code") val countryCode: String,
    )
}