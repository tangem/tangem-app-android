package com.tangem.data.pay

import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary

internal const val TEST_CLABE = "002010077777777771"

internal fun speiBankAccountDetails() = BankAccountDetails(
    currency = "MXN",
    rail = BankRail.SPEI,
    isThirdParty = false,
    beneficiary = Beneficiary.Individual(firstName = "Maria", lastName = "Lopez"),
    beneficiaryAddress = null,
    bankName = null,
    bankAddress = null,
    accountNumber = TEST_CLABE,
    routingNumber = null,
    accountType = null,
)