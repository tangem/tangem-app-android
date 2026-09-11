package com.tangem.features.tangempay.withdraw.bank.form.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** Ordered inputs of each form step as laid out in Figma; a business recipient has one name field instead of two. */
internal object BankWithdrawFormFields {

    val bankDetails: ImmutableList<BankWithdrawFormField> = persistentListOf(
        BankWithdrawFormField.BankName,
        BankWithdrawFormField.BankCountry,
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
}