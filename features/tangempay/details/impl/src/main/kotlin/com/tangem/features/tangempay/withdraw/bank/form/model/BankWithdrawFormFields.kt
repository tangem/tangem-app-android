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

    private val regionFields = setOf(BankWithdrawFormField.Region)

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

    fun visible(
        fields: List<BankWithdrawFormField>,
        values: Map<BankWithdrawFormField, String>,
    ): ImmutableList<BankWithdrawFormField> = fields
        .filterNot { field -> field in regionFields && values[field].isNullOrBlank() }
        .toImmutableList()
}