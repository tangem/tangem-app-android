package com.tangem.domain.pay.model.bankofframp

/** Saved bank off-ramp recipient as returned by `GET /account/bank-accounts`. */
data class BankAccount(
    val id: String,
    val displayName: String,
    val details: BankAccountDetails,
)

/**
 * The hub's view of the saved recipients, taken from a single `GET /account/bank-accounts`: [recents] is the
 * shortlist, while [hasMoreAccounts] tells whether "See all" holds anyone the shortlist leaves out.
 */
data class RecentBankAccounts(
    val recents: List<BankAccount>,
    val hasMoreAccounts: Boolean,
)

/**
 * Only the beneficiary and the account number are guaranteed: a recipient saved through another Rain client can
 * come back described as partially as the backend has it. A recipient added in the wizard carries every field,
 * because the order the app sends requires them all.
 */
data class BankAccountDetails(
    val currency: String?,
    val rail: BankRail?,
    val isThirdParty: Boolean,
    val beneficiary: Beneficiary,
    val beneficiaryAddress: PostalAddress?,
    val bankName: String?,
    val bankAddress: PostalAddress?,
    val accountNumber: String,
    val routingNumber: String?,
    val accountType: BankAccountType?,
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