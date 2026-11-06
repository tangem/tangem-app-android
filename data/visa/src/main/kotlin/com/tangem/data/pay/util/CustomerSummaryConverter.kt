package com.tangem.data.pay.util

import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.pay.model.CustomerSummary.PaymentAccount
import com.tangem.spend.datasource.pay.models.response.CustomerSummaryResponse
import com.tangem.utils.converter.Converter

internal object CustomerSummaryConverter : Converter<CustomerSummaryResponse.Result, CustomerSummary> {

    override fun convert(value: CustomerSummaryResponse.Result): CustomerSummary {
        return CustomerSummary(
            customerId = value.customerId,
            customerStatus = CustomerSummary.CustomerStatus.fromString(value.customerStatus),
            isHidden = value.isHidden,
            kycStatus = CustomerSummary.KycStatus.fromString(value.kycStatus),
            paymentAccount = convertPaymentAccount(value.paymentAccount),
            moneyAccount = convertMoneyAccount(value.bankingAccount),
        )
    }

    private fun convertPaymentAccount(value: CustomerSummaryResponse.PaymentAccount): PaymentAccount {
        return PaymentAccount(
            id = value.id,
            activation = PaymentAccount.Activation(
                status = PaymentAccount.Activation.Status.fromString(value.activation.status),
                step = value.activation.step?.let(PaymentAccount.Activation.Step::fromString),
                failureReason = value.activation.failureReason
                    ?.let(PaymentAccount.Activation.FailureReason::fromString),
            ),
            cards = CustomerSummary.Cards(
                active = value.cards.active,
                frozen = value.cards.frozen,
                pending = value.cards.pending,
            ),
        )
    }

    private fun convertMoneyAccount(value: CustomerSummaryResponse.BankingAccount): MoneyAccount {
        return MoneyAccount(
            id = value.id,
            activation = MoneyAccount.Activation(
                status = MoneyAccount.Activation.Status.fromString(value.activation.status),
                step = value.activation.step?.let(MoneyAccount.Activation.Step::fromString),
                failureReason = value.activation.failureReason
                    ?.let(MoneyAccount.Activation.FailureReason::fromString),
            ),
        )
    }
}