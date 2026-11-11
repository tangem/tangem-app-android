package com.tangem.domain.tangempay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.tangempay.TangemPayCustomerResidency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayCurrencyFactory
import com.tangem.domain.tangempay.flow.PaymentAccountStatusCache
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.tangempay.model.CustomerSummary.CustomerStatus
import com.tangem.domain.tangempay.model.CustomerSummary.PaymentAccount
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import com.tangem.domain.tangempay.model.CustomerSummary.KycStatus as SummaryKycStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolvePaymentAccountStatusFromSummaryUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk()
    private val getTangemPayTariffPlanStateUseCase: GetTangemPayTariffPlanStateUseCase = mockk()
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory = mockk()
    private val getTangemPayFiatRateUseCase: GetTangemPayFiatRateUseCase = mockk()
    private val paymentAccountStatusCache: PaymentAccountStatusCache = mockk()

    private val useCase = ResolvePaymentAccountStatusFromSummaryUseCase(
        onboardingRepository = onboardingRepository,
        getTangemPayTariffPlanStateUseCase = getTangemPayTariffPlanStateUseCase,
        tangemPayCurrencyFactory = tangemPayCurrencyFactory,
        getTangemPayFiatRateUseCase = getTangemPayFiatRateUseCase,
        paymentAccountStatusCache = paymentAccountStatusCache,
    )

    private val userWalletId = UserWalletId("011")
    private val residency = TangemPayCustomerResidency(countryCode = "US", regionCode = "NY")

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            onboardingRepository,
            getTangemPayTariffPlanStateUseCase,
            tangemPayCurrencyFactory,
            getTangemPayFiatRateUseCase,
            paymentAccountStatusCache,
        )
        coEvery { onboardingRepository.getCustomerResidency(userWalletId) } returns residency
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns TangemPayApiError.Unspecified.left()
        coEvery { paymentAccountStatusCache.getSyncOrNull(userWalletId) } returns null
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
            name = "failed shows card issue failure",
            summary = summary(
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.FAILED,
                paymentStep = PaymentAccount.Activation.Step.ISSUING,
                paymentFailureReason = PaymentAccount.Activation.FailureReason.RAIN_REJECTED,
            ),
            expected = PaymentAccountStatusValue.Error.CardIssueFailed(customerId = CUSTOMER_ID),
        ),
        ResolveModel(
            name = "closed with failed account requires full fetch",
            summary = summary(
                customerStatus = CustomerStatus.FORMER,
                kycStatus = SummaryKycStatus.APPROVED,
                payment = PaymentAccount.Activation.Status.FAILED,
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardIssueFailedDetails {

        private val failedSummary = summary(
            kycStatus = SummaryKycStatus.APPROVED,
            payment = PaymentAccount.Activation.Status.FAILED,
            paymentStep = PaymentAccount.Activation.Step.ISSUING,
            paymentFailureReason = PaymentAccount.Activation.FailureReason.RAIN_REJECTED,
        )
        private val tariff: TangemPayCustomerTariffPlan = mockk()
        private val tariffState: TangemPayTariffPlanState = mockk()
        private val networkInfos: List<CustomerInfo.NetworkInfo> = listOf(mockk())
        private val networkStatuses: List<PaymentNetworkStatus> = listOf(mockk())
        private val fiatRate = BigDecimal("0.99")
        private val fiatBalance = PaymentAccountStatusValue.FiatBalance(
            availableBalance = BigDecimal.TEN,
            currency = "USD",
        )

        @BeforeEach
        fun setUpDetails() {
            coEvery { getTangemPayTariffPlanStateUseCase(userWalletId = userWalletId, tariff = tariff) } returns
                tariffState
            coEvery { getTangemPayFiatRateUseCase() } returns fiatRate
            every {
                tangemPayCurrencyFactory.createNetworkStatuses(userWalletId, networkInfos, fiatRate)
            } returns networkStatuses
        }

        @Test
        fun `GIVEN customer info with balance WHEN invoke THEN card issue failure built from it`() = runTest {
            // Arrange
            coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
                customerInfo(fiatBalance = fiatBalance).right()

            // Act
            val actual = useCase(userWalletId = userWalletId, summary = failedSummary)

            // Assert
            assertThat(actual).isEqualTo(
                PaymentAccountStatusValue.Error.CardIssueFailed(
                    customerId = CUSTOMER_ID,
                    tariffPlan = tariffState,
                    balance = PaymentAccountStatusValue.Balance(fiatBalance = fiatBalance),
                    networks = networkStatuses,
                    fiatRate = fiatRate,
                ),
            )
        }

        @Test
        fun `GIVEN customer info with balance WHEN invoke THEN cache is not read`() = runTest {
            // Arrange
            coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
                customerInfo(fiatBalance = fiatBalance).right()

            // Act
            useCase(userWalletId = userWalletId, summary = failedSummary)

            // Assert
            coVerify(exactly = 0) { paymentAccountStatusCache.getSyncOrNull(any()) }
        }

        @Test
        fun `GIVEN customer info without balance WHEN invoke THEN cached balance used`() = runTest {
            // Arrange
            val cachedBalance = PaymentAccountStatusValue.Balance(fiatBalance = fiatBalance)
            val cached = AccountStatus.Payment(
                account = Account.Payment(userWalletId = userWalletId),
                value = PaymentAccountStatusValue.Error.CardIssueFailed(
                    customerId = CUSTOMER_ID,
                    balance = cachedBalance,
                ),
            )
            coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns
                customerInfo(fiatBalance = null).right()
            coEvery { paymentAccountStatusCache.getSyncOrNull(userWalletId) } returns cached

            // Act
            val actual = useCase(userWalletId = userWalletId, summary = failedSummary)

            // Assert
            assertThat(actual).isEqualTo(
                PaymentAccountStatusValue.Error.CardIssueFailed(
                    customerId = CUSTOMER_ID,
                    tariffPlan = tariffState,
                    balance = cachedBalance,
                    networks = networkStatuses,
                    fiatRate = fiatRate,
                ),
            )
        }

        @Test
        fun `GIVEN customer info fails WHEN invoke THEN bare card issue failure returned`() = runTest {
            // Act
            val actual = useCase(userWalletId = userWalletId, summary = failedSummary)

            // Assert
            assertThat(actual).isEqualTo(PaymentAccountStatusValue.Error.CardIssueFailed(customerId = CUSTOMER_ID))
            coVerify(exactly = 0) { getTangemPayTariffPlanStateUseCase(any(), any()) }
        }

        private fun customerInfo(fiatBalance: PaymentAccountStatusValue.FiatBalance?): CustomerInfo = mockk {
            every { tariffPlan } returns tariff
            every { this@mockk.fiatBalance } returns fiatBalance
            every { networks } returns networkInfos
        }
    }

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
        kycStatus: SummaryKycStatus = SummaryKycStatus.REQUIRED,
        payment: PaymentAccount.Activation.Status = PaymentAccount.Activation.Status.NOT_STARTED,
        paymentStep: PaymentAccount.Activation.Step? = null,
        paymentFailureReason: PaymentAccount.Activation.FailureReason? = null,
        banking: MoneyAccount.Activation.Status = MoneyAccount.Activation.Status.NOT_STARTED,
    ) = CustomerSummary(
        customerId = CUSTOMER_ID,
        customerStatus = customerStatus,
        isHidden = false,
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