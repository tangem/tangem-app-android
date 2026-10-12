package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField

/**
 * Inverse of [NewRecipientDraft.toBankAccountDetails]: lets the preview render a saved recipient and a recipient being
 * added through the same form-field rows. The postal address's second line and the bank country have no form field and
 * are dropped, and a value the backend never sent produces no row at all.
 */
internal fun BankAccountDetails.toFormValues(): Map<BankWithdrawFormField, String> {
    val individual = beneficiary as? Beneficiary.Individual
    val business = beneficiary as? Beneficiary.Business
    return listOf(
        BankWithdrawFormField.AccountNumber to accountNumber,
        BankWithdrawFormField.RoutingNumber to routingNumber,
        BankWithdrawFormField.FirstName to individual?.firstName,
        BankWithdrawFormField.LastName to individual?.lastName,
        BankWithdrawFormField.BusinessName to business?.name,
        BankWithdrawFormField.Country to beneficiaryAddress?.countryCode,
        BankWithdrawFormField.Region to beneficiaryAddress?.region,
        BankWithdrawFormField.City to beneficiaryAddress?.city,
        BankWithdrawFormField.AddressLine1 to beneficiaryAddress?.line1,
        BankWithdrawFormField.PostalCode to beneficiaryAddress?.postalCode,
        BankWithdrawFormField.BankName to bankName,
        BankWithdrawFormField.BankRegion to bankAddress?.region,
        BankWithdrawFormField.BankCity to bankAddress?.city,
        BankWithdrawFormField.BankAddressLine1 to bankAddress?.line1,
        BankWithdrawFormField.BankPostalCode to bankAddress?.postalCode,
    ).mapNotNull { (field, value) ->
        value?.takeIf(String::isNotBlank)?.let { field to it }
    }.toMap()
}