package com.tangem.domain.pay.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.pay.model.CustomerSummary.CustomerStatus
import com.tangem.domain.pay.model.CustomerSummary.KycStatus
import com.tangem.domain.pay.model.CustomerSummary.PaymentAccount
import com.tangem.domain.pay.model.MoneyAccountActivationState
import com.tangem.domain.pay.model.PaymentAccountActivationState
import com.tangem.domain.pay.model.PaymentAccountActivationState.Issuing
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCustomerStateConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN summary WHEN convert THEN expected customer state`(model: ConvertModel) {
        // Act
        val actual = TangemPayCustomerStateConverter.convert(model.summary)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "hidden flag is ignored",
            summary = summary(
                isHidden = true,
                kycStatus = KycStatus.APPROVED,
                payment = payment(COMPLETED),
                banking = banking(MoneyAccount.Activation.Status.DEACTIVATED),
            ),
            expected = accounts(
                payment = PaymentAccountActivationState.Completed(CARDS),
                money = MoneyAccountActivationState.Deactivated,
            ),
        ),
        ConvertModel(
            name = "blocked customer",
            summary = summary(customerStatus = CustomerStatus.BLOCKED, payment = payment(COMPLETED)),
            expected = TangemPayCustomerState.Blocked,
        ),
        ConvertModel(
            name = "closed customer with opened product",
            summary = summary(
                customerStatus = CustomerStatus.FORMER,
                kycStatus = KycStatus.APPROVED,
                payment = payment(COMPLETED),
                banking = banking(MoneyAccount.Activation.Status.DEACTIVATED),
            ),
            expected = TangemPayCustomerState.Former(
                paymentAccount = PaymentAccountActivationState.Completed(CARDS),
                moneyAccount = MoneyAccountActivationState.Deactivated,
            ),
        ),
        ConvertModel(
            name = "closed customer without products",
            summary = summary(customerStatus = CustomerStatus.FORMER, kycStatus = KycStatus.APPROVED),
            expected = TangemPayCustomerState.Former(
                paymentAccount = PaymentAccountActivationState.NotStarted,
                moneyAccount = MoneyAccountActivationState.NotStarted,
            ),
        ),
        ConvertModel(
            name = "nothing started, kyc required",
            summary = summary(kycStatus = KycStatus.REQUIRED),
            expected = TangemPayCustomerState.KycRequired,
        ),
        ConvertModel(
            name = "nothing started, kyc in progress",
            summary = summary(kycStatus = KycStatus.IN_PROGRESS),
            expected = TangemPayCustomerState.KycInProgress,
        ),
        ConvertModel(
            name = "nothing started, kyc declined",
            summary = summary(kycStatus = KycStatus.DECLINED),
            expected = TangemPayCustomerState.KycDeclined,
        ),
        ConvertModel(
            name = "orders created before kyc wait for it",
            summary = summary(
                kycStatus = KycStatus.IN_PROGRESS,
                payment = payment(PROCESSING, PaymentAccount.Activation.Step.AWAITING_KYC),
                banking = banking(
                    status = MoneyAccount.Activation.Status.PROCESSING,
                    step = MoneyAccount.Activation.Step.AWAITING_KYC,
                ),
            ),
            expected = TangemPayCustomerState.KycInProgress,
        ),
        ConvertModel(
            name = "nothing started, kyc approved",
            summary = summary(kycStatus = KycStatus.APPROVED),
            expected = TangemPayCustomerState.KycApproved,
        ),
        ConvertModel(
            name = "both products unavailable",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                payment = payment(PaymentAccount.Activation.Status.UNAVAILABLE),
                banking = banking(MoneyAccount.Activation.Status.UNAVAILABLE),
            ),
            expected = TangemPayCustomerState.KycApproved,
        ),
        ConvertModel(
            name = "payment issuing, money not started",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                payment = payment(PROCESSING, PaymentAccount.Activation.Step.ISSUING),
            ),
            expected = accounts(
                payment = Issuing(Issuing.Step.ISSUING),
                money = MoneyAccountActivationState.NotStarted,
            ),
        ),
        ConvertModel(
            name = "payment awaiting deposit, money unavailable",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                payment = payment(PROCESSING, PaymentAccount.Activation.Step.AWAITING_DEPOSIT),
                banking = banking(MoneyAccount.Activation.Status.UNAVAILABLE),
            ),
            expected = accounts(
                payment = Issuing(Issuing.Step.AWAITING_DEPOSIT),
                money = MoneyAccountActivationState.Unavailable,
            ),
        ),
        ConvertModel(
            name = "processing without step read as issuing",
            summary = summary(kycStatus = KycStatus.APPROVED, payment = payment(PROCESSING)),
            expected = accounts(
                payment = Issuing(Issuing.Step.ISSUING),
                money = MoneyAccountActivationState.NotStarted,
            ),
        ),
        ConvertModel(
            name = "payment not started, money issuing",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                banking = banking(MoneyAccount.Activation.Status.PROCESSING, MoneyAccount.Activation.Step.ISSUING),
            ),
            expected = accounts(
                payment = PaymentAccountActivationState.NotStarted,
                money = MoneyAccountActivationState.Issuing,
            ),
        ),
        ConvertModel(
            name = "payment failed",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                payment = payment(
                    status = FAILED,
                    step = PaymentAccount.Activation.Step.AWAITING_DEPOSIT,
                    failureReason = PaymentAccount.Activation.FailureReason.DEPOSIT_TIMEOUT,
                ),
            ),
            expected = accounts(
                payment = PaymentAccountActivationState.Failed(PaymentAccount.Activation.FailureReason.DEPOSIT_TIMEOUT),
                money = MoneyAccountActivationState.NotStarted,
            ),
        ),
        ConvertModel(
            name = "failed while awaiting kyc shows the failure",
            summary = summary(
                kycStatus = KycStatus.REQUIRED,
                payment = payment(
                    status = FAILED,
                    step = PaymentAccount.Activation.Step.AWAITING_KYC,
                    failureReason = PaymentAccount.Activation.FailureReason.TECHNICAL,
                ),
            ),
            expected = accounts(
                payment = PaymentAccountActivationState.Failed(PaymentAccount.Activation.FailureReason.TECHNICAL),
                money = MoneyAccountActivationState.AwaitingKyc(KycStatus.REQUIRED),
            ),
        ),
        ConvertModel(
            name = "failed without reason read as technical",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                banking = banking(MoneyAccount.Activation.Status.FAILED),
            ),
            expected = accounts(
                payment = PaymentAccountActivationState.NotStarted,
                money = MoneyAccountActivationState.Failed(MoneyAccount.Activation.FailureReason.TECHNICAL),
            ),
        ),
        ConvertModel(
            name = "payment completed, money deactivated",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                payment = payment(COMPLETED),
                banking = banking(MoneyAccount.Activation.Status.DEACTIVATED),
            ),
            expected = accounts(
                payment = PaymentAccountActivationState.Completed(CARDS),
                money = MoneyAccountActivationState.Deactivated,
            ),
        ),
        ConvertModel(
            name = "kyc reset with opened payment and money in progress",
            summary = summary(
                kycStatus = KycStatus.REQUIRED,
                payment = payment(COMPLETED),
                banking = banking(MoneyAccount.Activation.Status.PROCESSING, MoneyAccount.Activation.Step.ISSUING),
            ),
            expected = accounts(
                payment = PaymentAccountActivationState.Completed(CARDS),
                money = MoneyAccountActivationState.AwaitingKyc(KycStatus.REQUIRED),
            ),
        ),
        ConvertModel(
            name = "kyc declined with opened money and payment not started",
            summary = summary(
                kycStatus = KycStatus.DECLINED,
                banking = banking(MoneyAccount.Activation.Status.COMPLETED),
            ),
            expected = accounts(
                payment = PaymentAccountActivationState.AwaitingKyc(KycStatus.DECLINED),
                money = MoneyAccountActivationState.Completed,
            ),
        ),
        ConvertModel(
            name = "unknown customer status read as active",
            summary = summary(customerStatus = CustomerStatus.UNKNOWN, kycStatus = KycStatus.APPROVED),
            expected = TangemPayCustomerState.KycApproved,
        ),
        ConvertModel(
            name = "unknown kyc read as in progress",
            summary = summary(kycStatus = KycStatus.UNKNOWN),
            expected = TangemPayCustomerState.KycInProgress,
        ),
        ConvertModel(
            name = "unknown activation status read as processing",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                payment = payment(PaymentAccount.Activation.Status.UNKNOWN),
            ),
            expected = accounts(
                payment = Issuing(Issuing.Step.ISSUING),
                money = MoneyAccountActivationState.NotStarted,
            ),
        ),
        ConvertModel(
            name = "unknown step read as issuing",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                payment = payment(PROCESSING, PaymentAccount.Activation.Step.UNKNOWN),
            ),
            expected = accounts(
                payment = Issuing(Issuing.Step.ISSUING),
                money = MoneyAccountActivationState.NotStarted,
            ),
        ),
    )

    private fun accounts(payment: PaymentAccountActivationState, money: MoneyAccountActivationState) =
        TangemPayCustomerState.Accounts(paymentAccount = payment, moneyAccount = money)

    private fun payment(
        status: PaymentAccount.Activation.Status,
        step: PaymentAccount.Activation.Step? = null,
        failureReason: PaymentAccount.Activation.FailureReason? = null,
    ) = PaymentAccount.Activation(status = status, step = step, failureReason = failureReason)

    private fun banking(
        status: MoneyAccount.Activation.Status,
        step: MoneyAccount.Activation.Step? = null,
        failureReason: MoneyAccount.Activation.FailureReason? = null,
    ) = MoneyAccount.Activation(status = status, step = step, failureReason = failureReason)

    private fun summary(
        customerStatus: CustomerStatus = CustomerStatus.NEW,
        isHidden: Boolean = false,
        kycStatus: KycStatus = KycStatus.REQUIRED,
        payment: PaymentAccount.Activation = payment(PaymentAccount.Activation.Status.NOT_STARTED),
        banking: MoneyAccount.Activation = banking(MoneyAccount.Activation.Status.NOT_STARTED),
    ) = CustomerSummary(
        customerId = "customer_id",
        customerStatus = customerStatus,
        isHidden = isHidden,
        kycStatus = kycStatus,
        paymentAccount = PaymentAccount(id = null, activation = payment, cards = CARDS),
        moneyAccount = MoneyAccount(id = null, activation = banking),
    )

    data class ConvertModel(
        val name: String,
        val summary: CustomerSummary,
        val expected: TangemPayCustomerState,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val PROCESSING = PaymentAccount.Activation.Status.PROCESSING
        val FAILED = PaymentAccount.Activation.Status.FAILED
        val COMPLETED = PaymentAccount.Activation.Status.COMPLETED
        val CARDS = CustomerSummary.Cards(active = 1, frozen = 1, pending = 1)
    }
}