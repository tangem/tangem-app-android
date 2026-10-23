package com.tangem.domain.pay.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.pay.model.CustomerSummary.CustomerStatus
import com.tangem.domain.pay.model.CustomerSummary.PaymentAccount
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.domain.pay.model.CustomerSummary.KycStatus as SummaryKycStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveMoneyAccountStatusFromSummaryUseCaseTest {

    private val useCase = ResolveMoneyAccountStatusFromSummaryUseCase()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN summary WHEN invoke THEN expected status`(model: ResolveModel) {
        // Act
        val actual = useCase(model.summary)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ResolveModel(
            name = "hidden",
            summary = summary(isHidden = true, banking = MoneyAccount.Activation.Status.COMPLETED),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "blocked",
            summary = summary(customerStatus = CustomerStatus.BLOCKED),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "closed without account",
            summary = summary(customerStatus = CustomerStatus.FORMER),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "closed with deactivated account requires full fetch",
            summary = summary(
                customerStatus = CustomerStatus.FORMER,
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.DEACTIVATED,
            ),
            expected = null,
        ),
        ResolveModel(
            name = "closed with completed account requires full fetch",
            summary = summary(
                customerStatus = CustomerStatus.FORMER,
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.COMPLETED,
            ),
            expected = null,
        ),
        ResolveModel(
            name = "not started before kyc",
            summary = summary(kycStatus = SummaryKycStatus.REQUIRED),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "not started after kyc",
            summary = summary(kycStatus = SummaryKycStatus.APPROVED),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "not started while payment issuing",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.PROCESSING,
                paymentStep = PaymentAccount.Activation.Step.ISSUING,
            ),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "unavailable",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.UNAVAILABLE,
            ),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "processing awaits kyc",
            summary = summary(
                kycStatus = SummaryKycStatus.IN_PROGRESS,
                banking = MoneyAccount.Activation.Status.PROCESSING,
                bankingStep = MoneyAccount.Activation.Step.AWAITING_KYC,
            ),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "processing awaits kyc after reset while payment completed",
            summary = summary(
                customerStatus = CustomerStatus.ACTIVE,
                kycStatus = SummaryKycStatus.REQUIRED,
                payment = PaymentAccount.Activation.Status.COMPLETED,
                banking = MoneyAccount.Activation.Status.PROCESSING,
                bankingStep = MoneyAccount.Activation.Step.AWAITING_KYC,
            ),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "processing issuing",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.PROCESSING,
                bankingStep = MoneyAccount.Activation.Step.ISSUING,
            ),
            expected = MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.ACTUAL,
                customerId = CUSTOMER_ID,
            ),
        ),
        ResolveModel(
            name = "failed",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.FAILED,
                bankingStep = MoneyAccount.Activation.Step.ISSUING,
                bankingFailureReason = MoneyAccount.Activation.FailureReason.RAIN_REJECTED,
            ),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "closed with failed account",
            summary = summary(
                customerStatus = CustomerStatus.FORMER,
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.FAILED,
                bankingStep = MoneyAccount.Activation.Step.ISSUING,
                bankingFailureReason = MoneyAccount.Activation.FailureReason.TECHNICAL,
            ),
            expected = MoneyAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "completed requires full fetch",
            summary = summary(
                customerStatus = CustomerStatus.ACTIVE,
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.COMPLETED,
            ),
            expected = null,
        ),
        ResolveModel(
            name = "deactivated requires full fetch",
            summary = summary(
                customerStatus = CustomerStatus.ACTIVE,
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.DEACTIVATED,
            ),
            expected = null,
        ),
    )

    private fun summary(
        customerStatus: CustomerStatus = CustomerStatus.NEW,
        isHidden: Boolean = false,
        kycStatus: SummaryKycStatus = SummaryKycStatus.REQUIRED,
        payment: PaymentAccount.Activation.Status = PaymentAccount.Activation.Status.NOT_STARTED,
        paymentStep: PaymentAccount.Activation.Step? = null,
        banking: MoneyAccount.Activation.Status = MoneyAccount.Activation.Status.NOT_STARTED,
        bankingStep: MoneyAccount.Activation.Step? = null,
        bankingFailureReason: MoneyAccount.Activation.FailureReason? = null,
    ) = CustomerSummary(
        customerId = CUSTOMER_ID,
        customerStatus = customerStatus,
        isHidden = isHidden,
        kycStatus = kycStatus,
        paymentAccount = PaymentAccount(
            id = null,
            activation = PaymentAccount.Activation(status = payment, step = paymentStep, failureReason = null),
            cards = CustomerSummary.Cards(active = 0, frozen = 0, pending = 0),
        ),
        moneyAccount = MoneyAccount(
            id = null,
            activation = MoneyAccount.Activation(
                status = banking,
                step = bankingStep,
                failureReason = bankingFailureReason,
            ),
        ),
    )

    data class ResolveModel(
        val name: String,
        val summary: CustomerSummary,
        val expected: MoneyAccountStatusValue?,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val CUSTOMER_ID = "customer_id"
    }
}