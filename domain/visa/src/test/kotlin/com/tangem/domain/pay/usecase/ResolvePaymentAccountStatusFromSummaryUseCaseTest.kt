package com.tangem.domain.pay.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCustomerResidency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.pay.model.CustomerSummary.CustomerStatus
import com.tangem.domain.pay.model.CustomerSummary.PaymentAccount
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.domain.pay.model.CustomerSummary.KycStatus as SummaryKycStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolvePaymentAccountStatusFromSummaryUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk()

    private val useCase = ResolvePaymentAccountStatusFromSummaryUseCase(onboardingRepository = onboardingRepository)

    private val userWalletId = UserWalletId("011")
    private val residency = TangemPayCustomerResidency(countryCode = "US", regionCode = "NY")

    @BeforeEach
    fun resetMocks() {
        clearMocks(onboardingRepository)
        coEvery { onboardingRepository.getCustomerResidency(userWalletId) } returns residency
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN summary WHEN invoke THEN expected status`(model: ResolveModel) = runTest {
        // Act
        val actual = useCase(userWalletId = userWalletId, summary = model.summary)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        ResolveModel(
            name = "hidden",
            summary = summary(isHidden = true),
            expected = PaymentAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "blocked",
            summary = summary(customerStatus = CustomerStatus.BLOCKED),
            expected = PaymentAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "closed without account",
            summary = summary(customerStatus = CustomerStatus.FORMER),
            expected = PaymentAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "closed with completed account requires full fetch",
            summary = summary(
                customerStatus = CustomerStatus.FORMER,
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.COMPLETED,
            ),
            expected = null,
        ),
        ResolveModel(
            name = "kyc required",
            summary = summary(kycStatus = SummaryKycStatus.REQUIRED),
            expected = kycRequired(),
        ),
        ResolveModel(
            name = "kyc in progress",
            summary = summary(kycStatus = SummaryKycStatus.IN_PROGRESS),
            expected = underReview(KycStatus.PENDING),
        ),
        ResolveModel(
            name = "kyc declined",
            summary = summary(kycStatus = SummaryKycStatus.DECLINED),
            expected = underReview(KycStatus.REJECTED),
        ),
        ResolveModel(
            name = "verified",
            summary = summary(kycStatus = SummaryKycStatus.APPROVED),
            expected = verified(),
        ),
        ResolveModel(
            name = "payment not started while money issuing",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                banking = MoneyAccount.Activation.Status.PROCESSING,
            ),
            expected = PaymentAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "payment unavailable while money issuing",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.UNAVAILABLE,
                banking = MoneyAccount.Activation.Status.PROCESSING,
            ),
            expected = PaymentAccountStatusValue.Empty,
        ),
        ResolveModel(
            name = "payment awaits kyc after reset",
            summary = summary(
                kycStatus = SummaryKycStatus.REQUIRED,
                banking = MoneyAccount.Activation.Status.COMPLETED,
            ),
            expected = kycRequired(),
        ),
        ResolveModel(
            name = "payment awaits declined kyc",
            summary = summary(
                kycStatus = SummaryKycStatus.DECLINED,
                banking = MoneyAccount.Activation.Status.COMPLETED,
            ),
            expected = underReview(KycStatus.REJECTED),
        ),
        ResolveModel(
            name = "issuing requires full fetch",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.PROCESSING,
                paymentStep = PaymentAccount.Activation.Step.ISSUING,
            ),
            expected = null,
        ),
        ResolveModel(
            name = "failed requires full fetch",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.FAILED,
                paymentStep = PaymentAccount.Activation.Step.ISSUING,
                paymentFailureReason = PaymentAccount.Activation.FailureReason.RAIN_REJECTED,
            ),
            expected = null,
        ),
        ResolveModel(
            name = "awaiting deposit requires full fetch",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.PROCESSING,
                paymentStep = PaymentAccount.Activation.Step.AWAITING_DEPOSIT,
            ),
            expected = null,
        ),
        ResolveModel(
            name = "completed requires full fetch",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.COMPLETED,
            ),
            expected = null,
        ),
    )

    private fun verified() = PaymentAccountStatusValue.Verified(source = StatusSource.ACTUAL)

    private fun kycRequired() = PaymentAccountStatusValue.KycRequired(
        source = StatusSource.ACTUAL,
        customerId = CUSTOMER_ID,
        customerResidency = residency,
    )

    private fun underReview(kycStatus: KycStatus) = PaymentAccountStatusValue.UnderReview(
        source = StatusSource.ACTUAL,
        kycStatus = kycStatus,
        customerId = CUSTOMER_ID,
    )

    private fun summary(
        customerStatus: CustomerStatus = CustomerStatus.NEW,
        isHidden: Boolean = false,
        kycStatus: SummaryKycStatus = SummaryKycStatus.REQUIRED,
        payment: PaymentAccount.Activation.Status = PaymentAccount.Activation.Status.NOT_STARTED,
        paymentStep: PaymentAccount.Activation.Step? = null,
        paymentFailureReason: PaymentAccount.Activation.FailureReason? = null,
        banking: MoneyAccount.Activation.Status = MoneyAccount.Activation.Status.NOT_STARTED,
    ) = CustomerSummary(
        customerId = CUSTOMER_ID,
        customerStatus = customerStatus,
        isHidden = isHidden,
        kycStatus = kycStatus,
        paymentAccount = PaymentAccount(
            id = null,
            activation = PaymentAccount.Activation(
                status = payment,
                step = paymentStep,
                failureReason = paymentFailureReason,
            ),
            cards = CustomerSummary.Cards(active = 0, frozen = 0, pending = 0),
        ),
        moneyAccount = MoneyAccount(
            id = null,
            activation = MoneyAccount.Activation(status = banking, step = null, failureReason = null),
        ),
    )

    data class ResolveModel(
        val name: String,
        val summary: CustomerSummary,
        val expected: PaymentAccountStatusValue?,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val CUSTOMER_ID = "customer_id"
    }
}