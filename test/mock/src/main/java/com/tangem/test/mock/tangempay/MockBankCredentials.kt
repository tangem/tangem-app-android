package com.tangem.test.mock.tangempay

import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.domain.tangempay.model.BankCredentials
import com.tangem.domain.tangempay.model.FeeCalculationType
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
        rail: String? = "ACH",
        transferMessage: String? = null,
        depositFee: BankCredentials.DepositFee? = null,
        depositLimit: BankCredentials.DepositLimit? = null,
        exchangeRate: BankCredentials.ExchangeRate? = null,
        amountLimits: AmountLimits? = null,
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
        exchangeRate = exchangeRate,
        amountLimits = amountLimits,
    )

    fun speiBankCredentials(
        accountNumber: String = "646180157000000004",
        transferMessage: String? = null,
        depositFee: BankCredentials.DepositFee? = BankCredentials.DepositFee(
            calculationType = FeeCalculationType.PERCENTAGE,
            amount = BigDecimal("1.00"),
            fixedAmount = BigDecimal("10.00"),
            currency = "MXN",
        ),
        amountLimits: AmountLimits? = AmountLimits(min = BigDecimal("2"), max = null, currency = "USD"),
        exchangeRate: BankCredentials.ExchangeRate? = BankCredentials.ExchangeRate(
            rate = BigDecimal("18.541575"),
            base = "USD",
            quote = "MXN",
        ),
    ) = bankCredentials(
        beneficiaryName = "",
        beneficiaryAddress = "",
        beneficiaryBankName = "",
        beneficiaryBankAddress = "",
        accountNumber = accountNumber,
        routingNumber = "",
        rail = "SPEI",
        transferMessage = transferMessage,
        depositFee = depositFee,
        exchangeRate = exchangeRate,
        amountLimits = amountLimits,
    )
}