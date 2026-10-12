package com.tangem.features.tangempay.withdraw.bank.form.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** Ordered inputs of each form step as laid out in Figma; a business recipient has one name field instead of two. */
internal object BankWithdrawFormFields {

    const val US_COUNTRY_CODE = "US"

    val bankDetails: ImmutableList<BankWithdrawFormField> = persistentListOf(
        BankWithdrawFormField.BankName,
        BankWithdrawFormField.BankRegion,
        BankWithdrawFormField.BankCity,
        BankWithdrawFormField.BankAddressLine1,
        BankWithdrawFormField.BankPostalCode,
    )

    fun recipientDetails(isBusiness: Boolean): ImmutableList<BankWithdrawFormField> = buildList {
        add(BankWithdrawFormField.AccountNumber)
        add(BankWithdrawFormField.RoutingNumber)
        if (isBusiness) {
            add(BankWithdrawFormField.BusinessName)
        } else {
            add(BankWithdrawFormField.FirstName)
            add(BankWithdrawFormField.LastName)
        }
        add(BankWithdrawFormField.Country)
        add(BankWithdrawFormField.Region)
        add(BankWithdrawFormField.City)
        add(BankWithdrawFormField.AddressLine1)
        add(BankWithdrawFormField.PostalCode)
    }.toImmutableList()

    /** Rain wants a two-letter state code for a US address; the rest of the world sends a free-text region. */
    fun isUsRecipientCountry(values: Map<BankWithdrawFormField, String>): Boolean =
        values[BankWithdrawFormField.Country]?.trim().equals(US_COUNTRY_CODE, ignoreCase = true)
}

/**
 * The geo API returns ISO 3166-2 ("US-NY"); Rain's address wants the bare subdivision ("NY"). The case of the
 * country part is not part of the contract, so "us-NY" is stripped just the same.
 */
internal fun String.subdivisionCode(countryCode: String): String {
    val prefix = "$countryCode-"
    return if (startsWith(prefix = prefix, ignoreCase = true)) drop(n = prefix.length) else this
}