package com.tangem.domain.pay.model.bankofframp

/** Saved bank off-ramp recipient as returned by `GET /account/bank-accounts`. */
data class BankAccount(
    val id: String,
    val displayName: String,
    val details: BankAccountDetails,
)

data class BankAccountDetails(
    val currency: String,
    val rail: BankRail,
    val isThirdParty: Boolean,
    val beneficiary: Beneficiary,
    val beneficiaryAddress: PostalAddress,
    val bankName: String,
    val bankAddress: PostalAddress,
    val accountNumber: String,
    val routingNumber: String,
    val accountType: BankAccountType,
)

sealed interface Beneficiary {
    data class Individual(val firstName: String, val lastName: String) : Beneficiary
    data class Business(val name: String) : Beneficiary
}

data class PostalAddress(
    val line1: String,
    val line2: String?,
    val city: String,
    val region: String,
    val postalCode: String,
    val countryCode: String,
)

enum class BankRail { ACH, WIRE }

enum class BankAccountType { CHECKING, SAVINGS }