package com.tangem.data.pay.util

import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.spend.datasource.pay.models.response.BankAccountsResponse
import com.tangem.utils.converter.Converter

/**
 * Converts a saved recipient, or `null` when the backend describes it too partially to send money to — a
 * missing routing number or an unknown rail would only surface as a failed order at the very end of the wizard.
 */
internal object BankAccountConverter : Converter<BankAccountsResponse.BankAccount, BankAccount?> {

    private const val BUSINESS_TYPE = "BUSINESS"

    override fun convert(value: BankAccountsResponse.BankAccount): BankAccount? {
        val details = value.details?.let(::convertDetails) ?: return null
        return BankAccount(
            id = value.id,
            displayName = value.displayName?.takeIf(String::isNotBlank) ?: displayName(details.beneficiary),
            details = details,
        )
    }

    private fun convertDetails(value: BankAccountsResponse.Details): BankAccountDetails? {
        val beneficiary = value.beneficiary?.let(::convertBeneficiary) ?: return null
        return BankAccountDetails(
            currency = value.currency?.takeIf(String::isNotBlank) ?: return null,
            rail = convertRail(value.rail) ?: return null,
            isThirdParty = value.isThirdParty == true,
            beneficiary = beneficiary,
            beneficiaryAddress = value.beneficiaryAddress?.let(::convertAddress) ?: return null,
            bankName = value.bankName?.takeIf(String::isNotBlank) ?: return null,
            bankAddress = value.bankAddress?.let(::convertAddress) ?: return null,
            accountNumber = value.accountNumber?.takeIf(String::isNotBlank) ?: return null,
            routingNumber = value.routingNumber?.takeIf(String::isNotBlank) ?: return null,
            accountType = convertAccountType(value.bankAccountType),
        )
    }

    private fun convertBeneficiary(value: BankAccountsResponse.Beneficiary): Beneficiary? {
        val business = value.name?.takeIf(String::isNotBlank)
        if (value.type.equalsIgnoringCase(BUSINESS_TYPE)) {
            return business?.let(Beneficiary::Business)
        }
        val firstName = value.firstName?.takeIf(String::isNotBlank)
        val lastName = value.lastName?.takeIf(String::isNotBlank)
        return when {
            firstName != null && lastName != null -> Beneficiary.Individual(firstName, lastName)
            business != null -> Beneficiary.Business(business)
            else -> null
        }
    }

    private fun convertAddress(value: BankAccountsResponse.Address): PostalAddress? {
        return PostalAddress(
            line1 = value.line1?.takeIf(String::isNotBlank) ?: return null,
            line2 = value.line2?.takeIf(String::isNotBlank),
            city = value.city?.takeIf(String::isNotBlank) ?: return null,
            region = value.region?.takeIf(String::isNotBlank) ?: return null,
            postalCode = value.postalCode?.takeIf(String::isNotBlank) ?: return null,
            countryCode = value.countryCode?.takeIf(String::isNotBlank) ?: return null,
        )
    }

    private fun convertRail(value: String?): BankRail? = when {
        value.equalsIgnoringCase(BankRail.ACH.name) -> BankRail.ACH
        value.equalsIgnoringCase(BankRail.WIRE.name) -> BankRail.WIRE
        else -> null
    }

    /** Wire transfers carry no account type; the backend contract treats a missing one as a checking account. */
    private fun convertAccountType(value: String?): BankAccountType = when {
        value.equalsIgnoringCase(BankAccountType.SAVINGS.name) -> BankAccountType.SAVINGS
        else -> BankAccountType.CHECKING
    }

    private fun displayName(beneficiary: Beneficiary): String = when (beneficiary) {
        is Beneficiary.Individual -> "${beneficiary.firstName} ${beneficiary.lastName}"
        is Beneficiary.Business -> beneficiary.name
    }

    private fun String?.equalsIgnoringCase(other: String): Boolean = this.equals(other, ignoreCase = true)
}