package com.tangem.data.pay.util

import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.spend.datasource.pay.models.response.CustomerSummaryResponse
import com.tangem.utils.converter.Converter

internal object CustomerSummaryConverter : Converter<CustomerSummaryResponse.Result, CustomerSummary> {

    override fun convert(value: CustomerSummaryResponse.Result): CustomerSummary {
        return CustomerSummary(
            customerId = value.customerId,
            customerStatus = CustomerInfo.State.fromString(value.customerStatus),
            isHidden = value.isHidden,
            kycStatus = KycStatus.fromString(value.kycStatus),
            paymentAccount = CustomerSummary.PaymentAccount(
                id = value.paymentAccount.id,
                activation = convertActivation(value.paymentAccount.activation),
                cards = CustomerSummary.Cards(
                    active = value.paymentAccount.cards.active,
                    frozen = value.paymentAccount.cards.frozen,
                    pending = value.paymentAccount.cards.pending,
                ),
            ),
            bankingAccount = CustomerSummary.BankingAccount(
                id = value.bankingAccount.id,
                activation = convertActivation(value.bankingAccount.activation),
            ),
        )
    }

    private fun convertActivation(value: CustomerSummaryResponse.Activation): CustomerSummary.Activation {
        return CustomerSummary.Activation(
            status = CustomerSummary.Activation.Status.fromString(value.status),
            step = value.step?.let(CustomerSummary.Activation.Step::fromString),
            failureReason = value.failureReason?.let(CustomerSummary.Activation.FailureReason::fromString),
        )
    }
}