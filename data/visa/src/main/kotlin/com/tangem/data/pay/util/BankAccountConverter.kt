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
 * Converts a saved recipient, or `null` when the backend describes it too partially to tell it apart from another
 * one — without a name or an account number there is nothing to show in the list. Everything else is kept as sent:
 * the customer picks such a recipient by name, and the backend is the one that validates the transfer.
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
            currency = value.currency?.takeIf(String::isNotBlank),
            rail = convertRail(value.rail),
            isThirdParty = value.isThirdParty == true,
            beneficiary = beneficiary,
            beneficiaryAddress = value.beneficiaryAddress?.let(::convertAddress),
            bankName = value.bankName?.takeIf(String::isNotBlank),
            bankAddress = value.bankAddress?.let(::convertAddress),
            accountNumber = value.accountNumber?.takeIf(String::isNotBlank) ?: return null,
            routingNumber = value.routingNumber?.takeIf(String::isNotBlank),
            accountType = convertAccountType(value.bankAccountType),
        )
    }

    private fun convertBeneficiary(value: BankAccountsResponse.Beneficiary): Beneficiary? {
        val business = value.businessName?.takeIf(String::isNotBlank)
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

    /** An address the app cannot fill completely is no address at all: the order payload needs every line of it. */
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

    private fun convertAccountType(value: String?): BankAccountType? = when {
        value.equalsIgnoringCase(BankAccountType.CHECKING.name) -> BankAccountType.CHECKING
        value.equalsIgnoringCase(BankAccountType.SAVINGS.name) -> BankAccountType.SAVINGS
        else -> null
    }

    private fun displayName(beneficiary: Beneficiary): String = when (beneficiary) {
        is Beneficiary.Individual -> "${beneficiary.firstName} ${beneficiary.lastName}"
        is Beneficiary.Business -> beneficiary.name
    }

    private fun String?.equalsIgnoringCase(other: String): Boolean = this.equals(other, ignoreCase = true)
}