package com.tangem.features.tangempay.withdraw.hub

import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress

internal fun bankAccount(
    id: String = "acc-1",
    beneficiary: Beneficiary = Beneficiary.Individual(firstName = "Andrei", lastName = "Melnikov"),
    accountNumber: String = "0000000000",
): BankAccount = BankAccount(
    id = id,
    displayName = when (beneficiary) {
        is Beneficiary.Individual -> "${beneficiary.firstName} ${beneficiary.lastName}"
        is Beneficiary.Business -> beneficiary.name
    },
    details = BankAccountDetails(
        currency = "USD",
        rail = BankRail.ACH,
        isThirdParty = false,
        beneficiary = beneficiary,
        beneficiaryAddress = postalAddress(),
        bankName = "Test Bank",
        bankAddress = postalAddress(),
        accountNumber = accountNumber,
        routingNumber = "021000021",
        accountType = BankAccountType.CHECKING,
    ),
)

private fun postalAddress(): PostalAddress = PostalAddress(
    line1 = "1 Main St",
    line2 = null,
    city = "New York",
    region = "NY",
    postalCode = "10001",
    countryCode = "US",
)