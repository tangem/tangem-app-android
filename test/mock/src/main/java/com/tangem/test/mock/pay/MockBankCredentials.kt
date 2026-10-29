package com.tangem.test.mock.pay

import com.tangem.domain.models.account.BankCredentials

object MockBankCredentials {

    @Suppress("LongParameterList")
    fun bankCredentials(
        type: String = "fiat",
        beneficiaryName: String = "RAIN PAYMENTS INC",
        beneficiaryAddress: String = "123 Finance St, New York, NY 10001, US",
        beneficiaryBankName: String = "Partner Bank, N.A.",
        beneficiaryBankAddress: String = "456 Bank Ave, New York, NY 10002, US",
        accountNumber: String = "9876543210",
        routingNumber: String = "021000021",
        rail: String? = "ACH",
        transferMessage: String? = null,
        depositFee: BankCredentials.DepositFee? = null,
        depositLimit: BankCredentials.DepositLimit? = null,
    ) = BankCredentials(
        type = type,
        beneficiaryName = beneficiaryName,
        beneficiaryAddress = beneficiaryAddress,
        beneficiaryBankName = beneficiaryBankName,
        beneficiaryBankAddress = beneficiaryBankAddress,
        accountNumber = accountNumber,
        routingNumber = routingNumber,
        rail = rail,
        transferMessage = transferMessage,
        depositFee = depositFee,
        depositLimit = depositLimit,
    )
}