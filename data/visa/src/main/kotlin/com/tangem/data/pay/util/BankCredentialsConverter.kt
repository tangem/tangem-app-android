package com.tangem.data.pay.util

import com.tangem.datasource.api.pay.models.response.BankCredentialsResponse
import com.tangem.domain.models.account.BankCredentials
import com.tangem.utils.converter.Converter

internal object BankCredentialsConverter : Converter<BankCredentialsResponse, BankCredentials> {
    override fun convert(value: BankCredentialsResponse): BankCredentials {
        return BankCredentials(
            type = value.type.orEmpty(),
            beneficiaryName = value.beneficiaryName.orEmpty(),
            beneficiaryAddress = value.beneficiaryAddress.orEmpty(),
            beneficiaryBankName = value.beneficiaryBankName.orEmpty(),
            beneficiaryBankAddress = value.beneficiaryBankAddress.orEmpty(),
            accountNumber = value.accountNumber.orEmpty(),
            routingNumber = value.routingNumber.orEmpty(),
        )
    }
}