package com.tangem.test.mock.pay

import com.tangem.domain.models.account.BankCredentials
import java.math.BigDecimal

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
        transferMessage: String? = null,
        depositFee: BigDecimal? = null,
        depositLimit: BankCredentials.DepositLimit? = null,
    ) = BankCredentials(
        type = type,
        beneficiaryName = beneficiaryName,
        beneficiaryAddress = beneficiaryAddress,
        beneficiaryBankName = beneficiaryBankName,
        beneficiaryBankAddress = beneficiaryBankAddress,
        accountNumber = accountNumber,
        routingNumber = routingNumber,
        transferMessage = transferMessage,
        depositFee = depositFee,
        depositLimit = depositLimit,
    )
}