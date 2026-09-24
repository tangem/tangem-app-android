package com.tangem.features.tangempay.withdraw.bank.form.model

/** The field name reported to analytics; it is a fixed product name, so it is never taken from the label. */
@Suppress("CyclomaticComplexMethod")
internal fun BankWithdrawFormField.toAnalyticsName(): String = when (this) {
    BankWithdrawFormField.AccountNumber -> "Account Number"
    BankWithdrawFormField.RoutingNumber -> "Routing Number"
    BankWithdrawFormField.FirstName -> "First Name"
    BankWithdrawFormField.LastName -> "Last Name"
    BankWithdrawFormField.BusinessName -> "Business Name"
    BankWithdrawFormField.Country -> "Country"
    BankWithdrawFormField.Region -> "Region"
    BankWithdrawFormField.City -> "City"
    BankWithdrawFormField.AddressLine1 -> "Address Line 1"
    BankWithdrawFormField.PostalCode -> "Postal Code"
    BankWithdrawFormField.BankName -> "Bank Name"
    BankWithdrawFormField.BankRegion -> "Bank Region"
    BankWithdrawFormField.BankCity -> "Bank City"
    BankWithdrawFormField.BankAddressLine1 -> "Bank Address Line 1"
    BankWithdrawFormField.BankPostalCode -> "Bank Postal Code"
}