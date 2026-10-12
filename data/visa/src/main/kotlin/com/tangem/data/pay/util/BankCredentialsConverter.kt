package com.tangem.data.pay.util

import com.tangem.domain.models.account.BankCredentials
import com.tangem.spend.datasource.pay.models.response.BankCredentialsResponse
import com.tangem.utils.converter.Converter

internal object BankCredentialsConverter : Converter<BankCredentialsResponse.Result, BankCredentials> {
    override fun convert(value: BankCredentialsResponse.Result): BankCredentials {
        return BankCredentials(
            type = value.type.orEmpty(),
            beneficiaryName = value.beneficiaryName.orEmpty(),
            beneficiaryAddress = value.beneficiaryAddress.orEmpty(),
            beneficiaryBankName = value.beneficiaryBankName.orEmpty(),
            beneficiaryBankAddress = value.beneficiaryBankAddress.orEmpty(),
            accountNumber = value.accountNumber.orEmpty(),
            routingNumber = value.routingNumber.orEmpty(),
            transferMessage = value.transferMessage?.takeIf(String::isNotBlank),
            depositFee = value.fee?.amount?.toBigDecimalOrNull(),
            depositLimit = convertDepositLimit(value.depositLimit),
        )
    }

    private fun convertDepositLimit(value: BankCredentialsResponse.DepositLimit?): BankCredentials.DepositLimit? {
        val amount = value?.amount?.toBigDecimalOrNull() ?: return null
        val period = value.period?.let(BankCredentials.DepositLimit.Period::fromString)
        return BankCredentials.DepositLimit(
            amount = amount,
            period = period ?: BankCredentials.DepositLimit.Period.UNKNOWN,
        )
    }
}