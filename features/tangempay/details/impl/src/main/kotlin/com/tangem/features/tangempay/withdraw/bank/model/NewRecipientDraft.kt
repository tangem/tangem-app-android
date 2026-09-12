package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields

/** Answers collected for a recipient the user is adding; each wizard step fills its own part. */
internal data class NewRecipientDraft(
    /** Only the personal flow asks this; a business recipient never sees the Type of transfer step. */
    val isThirdParty: Boolean = false,
    val accountType: BankAccountType = BankAccountType.CHECKING,
    val fields: Map<BankWithdrawFormField, String> = emptyMap(),
)

internal fun NewRecipientDraft.isComplete(isBusiness: Boolean): Boolean {
    val required = BankWithdrawFormFields.visible(
        fields = BankWithdrawFormFields.recipientDetails(isBusiness = isBusiness) + BankWithdrawFormFields.bankDetails,
        values = fields,
    )
    return required.all { field -> !fields[field].isNullOrBlank() }
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
        isThirdParty = isThirdParty,
        beneficiary = beneficiary,
        beneficiaryAddress = postalAddress(RECIPIENT_ADDRESS),
        bankName = value(BankWithdrawFormField.BankName),
        bankAddress = postalAddress(BANK_ADDRESS),
        accountNumber = value(BankWithdrawFormField.AccountNumber),
        routingNumber = value(BankWithdrawFormField.RoutingNumber),
        accountType = accountType,
    )
}

private fun NewRecipientDraft.postalAddress(address: AddressFields): PostalAddress {
    val countryCode = value(address.country).uppercase()
    val region = fields[address.region].orEmpty().trim()
    return PostalAddress(
        line1 = value(address.line1),
        line2 = null,
        city = value(address.city),
        region = if (countryCode == US_COUNTRY_CODE) region.uppercase() else region,
        postalCode = value(address.postalCode),
        countryCode = countryCode,
    )
}

private fun NewRecipientDraft.value(field: BankWithdrawFormField): String = fields.getValue(field).trim()

private data class AddressFields(
    val country: BankWithdrawFormField,
    val region: BankWithdrawFormField,
    val city: BankWithdrawFormField,
    val line1: BankWithdrawFormField,
    val postalCode: BankWithdrawFormField,
)

private val RECIPIENT_ADDRESS = AddressFields(
    country = BankWithdrawFormField.Country,
    region = BankWithdrawFormField.Region,
    city = BankWithdrawFormField.City,
    line1 = BankWithdrawFormField.AddressLine1,
    postalCode = BankWithdrawFormField.PostalCode,
)

private val BANK_ADDRESS = AddressFields(
    country = BankWithdrawFormField.BankCountry,
    region = BankWithdrawFormField.BankRegion,
    city = BankWithdrawFormField.BankCity,
    line1 = BankWithdrawFormField.BankAddressLine1,
    postalCode = BankWithdrawFormField.BankPostalCode,
)

private const val USD_CURRENCY = "USD"
private const val US_COUNTRY_CODE = "US"