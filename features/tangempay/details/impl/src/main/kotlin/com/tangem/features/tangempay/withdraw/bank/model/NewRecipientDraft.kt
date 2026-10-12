package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields

/** Answers collected for a recipient the user is adding; a step's answer stays null until that step is taken. */
internal data class NewRecipientDraft(
    /** A business recipient is always a third-party transfer; only the personal flow is asked about it. */
    val isThirdParty: Boolean? = null,
    val accountType: BankAccountType? = null,
    val fields: Map<BankWithdrawFormField, String> = emptyMap(),
    /**
     * What a picked country or state is called; [fields] keeps the code Rain's payload needs. The map always has
     * the same keys as [fields], and an empty name means the value has none — typed by hand, or nothing picked —
     * so a later step's blank entry overrides a stale name when the two maps are merged with `+`.
     */
    val displayNames: Map<BankWithdrawFormField, String> = emptyMap(),
) {

    companion object {

        /** A business recipient never sees the Type of transfer step, and Rain still requires the flag for them. */
        fun forEntry(entry: BankWithdrawEntry): NewRecipientDraft =
            NewRecipientDraft(isThirdParty = true.takeIf { entry == BankWithdrawEntry.NewBusiness })
    }
}

internal fun NewRecipientDraft.isComplete(isBusiness: Boolean): Boolean {
    if (isThirdParty == null || accountType == null) return false
    val formFields = BankWithdrawFormFields.recipientDetails(isBusiness = isBusiness) +
        BankWithdrawFormFields.bankDetails
    return formFields.all { field -> !fields[field].isNullOrBlank() }
}

/** The order payload for a recipient added in this wizard, or null until the rail and every form step are in place. */
internal fun BankWithdrawDraft.toNewBankAccountDetails(): BankAccountDetails? {
    val isBusiness = entry == BankWithdrawEntry.NewBusiness
    val rail = destination?.rail ?: return null
    if (!newRecipient.isComplete(isBusiness = isBusiness)) return null
    return newRecipient.toBankAccountDetails(rail = rail, isBusiness = isBusiness)
}

internal fun NewRecipientDraft.toBankAccountDetails(rail: BankRail, isBusiness: Boolean): BankAccountDetails {
    require(isComplete(isBusiness = isBusiness)) { "New recipient draft is incomplete" }
    val beneficiary = if (isBusiness) {
        Beneficiary.Business(name = value(BankWithdrawFormField.BusinessName))
    } else {
        Beneficiary.Individual(
            firstName = value(BankWithdrawFormField.FirstName),
            lastName = value(BankWithdrawFormField.LastName),
        )
    }
    return BankAccountDetails(
        currency = USD_CURRENCY,
        rail = rail,
        isThirdParty = requireNotNull(isThirdParty),
        beneficiary = beneficiary,
        beneficiaryAddress = postalAddress(
            address = RECIPIENT_ADDRESS,
            countryCode = value(BankWithdrawFormField.Country).uppercase(),
        ),
        bankName = value(BankWithdrawFormField.BankName),
        // Rain's bank rails are US-only: the form has no bank country field and the order always sends US.
        bankAddress = postalAddress(address = BANK_ADDRESS, countryCode = BankWithdrawFormFields.US_COUNTRY_CODE),
        accountNumber = value(BankWithdrawFormField.AccountNumber),
        routingNumber = value(BankWithdrawFormField.RoutingNumber),
        accountType = requireNotNull(accountType),
    )
}

private fun NewRecipientDraft.postalAddress(address: AddressFields, countryCode: String): PostalAddress {
    val region = fields[address.region].orEmpty().trim()
    return PostalAddress(
        line1 = value(address.line1),
        line2 = null,
        city = value(address.city),
        region = if (countryCode == BankWithdrawFormFields.US_COUNTRY_CODE) region.uppercase() else region,
        postalCode = value(address.postalCode),
        countryCode = countryCode,
    )
}

private fun NewRecipientDraft.value(field: BankWithdrawFormField): String = fields.getValue(field).trim()

private data class AddressFields(
    val region: BankWithdrawFormField,
    val city: BankWithdrawFormField,
    val line1: BankWithdrawFormField,
    val postalCode: BankWithdrawFormField,
)

private val RECIPIENT_ADDRESS = AddressFields(
    region = BankWithdrawFormField.Region,
    city = BankWithdrawFormField.City,
    line1 = BankWithdrawFormField.AddressLine1,
    postalCode = BankWithdrawFormField.PostalCode,
)

private val BANK_ADDRESS = AddressFields(
    region = BankWithdrawFormField.BankRegion,
    city = BankWithdrawFormField.BankCity,
    line1 = BankWithdrawFormField.BankAddressLine1,
    postalCode = BankWithdrawFormField.BankPostalCode,
)

private const val USD_CURRENCY = "USD"