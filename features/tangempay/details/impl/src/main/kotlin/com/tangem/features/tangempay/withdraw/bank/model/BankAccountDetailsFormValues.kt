package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField

/**
 * Inverse of [NewRecipientDraft.toBankAccountDetails]: lets the preview render a saved recipient and a recipient being
 * added through the same form-field rows. The postal address's second line has no form field and is dropped.
 */
internal fun BankAccountDetails.toFormValues(): Map<BankWithdrawFormField, String> = buildMap {
    put(BankWithdrawFormField.AccountNumber, accountNumber)
    put(BankWithdrawFormField.RoutingNumber, routingNumber)
    when (val beneficiary = beneficiary) {
        is Beneficiary.Individual -> {
            put(BankWithdrawFormField.FirstName, beneficiary.firstName)
            put(BankWithdrawFormField.LastName, beneficiary.lastName)
        }
        is Beneficiary.Business -> put(BankWithdrawFormField.BusinessName, beneficiary.name)
    }
    put(BankWithdrawFormField.Country, beneficiaryAddress.countryCode)
    put(BankWithdrawFormField.Region, beneficiaryAddress.region)
    put(BankWithdrawFormField.City, beneficiaryAddress.city)
    put(BankWithdrawFormField.AddressLine1, beneficiaryAddress.line1)
    put(BankWithdrawFormField.PostalCode, beneficiaryAddress.postalCode)
    put(BankWithdrawFormField.BankName, bankName)
    put(BankWithdrawFormField.BankCountry, bankAddress.countryCode)
    put(BankWithdrawFormField.BankRegion, bankAddress.region)
    put(BankWithdrawFormField.BankCity, bankAddress.city)
    put(BankWithdrawFormField.BankAddressLine1, bankAddress.line1)
    put(BankWithdrawFormField.BankPostalCode, bankAddress.postalCode)
}