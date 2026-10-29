package com.tangem.domain.moneyaccount.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.CustomerStatus
import com.tangem.domain.pay.model.CustomerSummary.KycStatus
import com.tangem.domain.pay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.pay.model.CustomerSummary.PaymentAccount
import com.tangem.domain.pay.model.CustomerSummary.MoneyAccount.Activation.Status as MoneyStatus
import com.tangem.domain.pay.model.CustomerSummary.PaymentAccount.Activation.Status as PaymentStatus
import com.tangem.domain.pay.usecase.GetCustomerSummaryUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PollTangemPayAccountStatusesUseCaseTest {

    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk()

    private val useCase = PollTangemPayAccountStatusesUseCase(
        getCustomerSummaryUseCase = getCustomerSummaryUseCase,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(getCustomerSummaryUseCase, paymentAccountStatusFetcher, moneyAccountStatusFetcher)
        coEvery { paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(USER_WALLET_ID)) } returns
            Unit.right()
        coEvery { moneyAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()
    }

    @Test
    fun `GIVEN terminal summary WHEN invoke THEN statuses fetched once and polling stops`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.ONLY_CACHE) } returns terminalSummary().right()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        assertThat(currentTime).isEqualTo(0)
        verifyFetched(times = 1)
    }

    @Test
    fun `GIVEN in progress summary WHEN it becomes terminal THEN polling stops after next interval`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.ONLY_CACHE) } returnsMany listOf(
            inProgressSummary().right(),
            terminalSummary().right(),
        )

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        assertThat(currentTime).isEqualTo(30.seconds.inWholeMilliseconds)
        verifyFetched(times = 2)
    }

    @Test
    fun `GIVEN no saved summary WHEN invoke THEN polling continues until summary is terminal`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.ONLY_CACHE) } returnsMany listOf(
            null.right(),
            terminalSummary().right(),
        )

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verifyFetched(times = 2)
    }

    @Test
    fun `GIVEN summary always in progress WHEN caller cancels THEN polling stops`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.ONLY_CACHE) } returns
            inProgressSummary().right()
        val job = launch { useCase(USER_WALLET_ID) }
        advanceTimeBy(1.minutes)
        runCurrent()

        // Act
        job.cancel()
        advanceTimeBy(5.minutes)

        // Assert
        verifyFetched(times = 3)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN summary state WHEN invoke THEN polling stops only on terminal state`(model: StateModel) =
        runTest {
            // Arrange
            coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.ONLY_CACHE) } returnsMany listOf(
                model.summary.right(),
                terminalSummary().right(),
            )

            // Act
            useCase(USER_WALLET_ID)

            // Assert
            verifyFetched(times = if (model.isTerminal) 1 else 2)
        }

    data class StateModel(val name: String, val summary: CustomerSummary, val isTerminal: Boolean) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        StateModel("hidden", summary(isHidden = true), isTerminal = true),
        StateModel("blocked", summary(customerStatus = CustomerStatus.BLOCKED), isTerminal = true),
        StateModel(
            name = "former with issuing accounts",
            summary = summary(
                customerStatus = CustomerStatus.FORMER,
                kycStatus = KycStatus.APPROVED,
                paymentStatus = PaymentStatus.PROCESSING,
                moneyStatus = MoneyStatus.PROCESSING,
            ),
            isTerminal = true,
        ),
        StateModel("kyc required", summary(kycStatus = KycStatus.REQUIRED), isTerminal = true),
        StateModel("kyc in progress", summary(kycStatus = KycStatus.IN_PROGRESS), isTerminal = false),
        StateModel("kyc unknown", summary(kycStatus = KycStatus.UNKNOWN), isTerminal = false),
        StateModel("kyc declined", summary(kycStatus = KycStatus.DECLINED), isTerminal = true),
        StateModel("kyc approved", summary(kycStatus = KycStatus.APPROVED), isTerminal = true),
        StateModel(
            name = "payment account issuing",
            summary = summary(kycStatus = KycStatus.APPROVED, paymentStatus = PaymentStatus.PROCESSING),
            isTerminal = false,
        ),
        StateModel(
            name = "payment account awaiting deposit",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                paymentStatus = PaymentStatus.PROCESSING,
                paymentStep = PaymentAccount.Activation.Step.AWAITING_DEPOSIT,
            ),
            isTerminal = false,
        ),
        StateModel(
            name = "money account awaiting kyc review",
            summary = summary(kycStatus = KycStatus.IN_PROGRESS, paymentStatus = PaymentStatus.COMPLETED),
            isTerminal = false,
        ),
        StateModel(
            name = "money account awaiting declined kyc",
            summary = summary(kycStatus = KycStatus.DECLINED, paymentStatus = PaymentStatus.COMPLETED),
            isTerminal = true,
        ),
        StateModel(
            name = "money account issuing",
            summary = summary(kycStatus = KycStatus.APPROVED, moneyStatus = MoneyStatus.PROCESSING),
            isTerminal = false,
        ),
        StateModel(
            name = "payment account failed",
            summary = summary(kycStatus = KycStatus.APPROVED, paymentStatus = PaymentStatus.FAILED),
            isTerminal = true,
        ),
        StateModel(
            name = "money account deactivated",
            summary = summary(kycStatus = KycStatus.APPROVED, moneyStatus = MoneyStatus.DEACTIVATED),
            isTerminal = true,
        ),
        StateModel(
            name = "both accounts completed",
            summary = summary(
                kycStatus = KycStatus.APPROVED,
                paymentStatus = PaymentStatus.COMPLETED,
                moneyStatus = MoneyStatus.COMPLETED,
            ),
            isTerminal = true,
        ),
    )

    private fun verifyFetched(times: Int) {
        coVerify(exactly = times) {
            paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(USER_WALLET_ID))
        }
        coVerify(exactly = times) { moneyAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    private fun inProgressSummary() = summary(kycStatus = KycStatus.IN_PROGRESS)

    private fun terminalSummary() = summary(kycStatus = KycStatus.DECLINED)

    private fun summary(
        isHidden: Boolean = false,
        customerStatus: CustomerStatus = CustomerStatus.NEW,
        kycStatus: KycStatus = KycStatus.REQUIRED,
        paymentStatus: PaymentStatus = PaymentStatus.NOT_STARTED,
        paymentStep: PaymentAccount.Activation.Step? = null,
        moneyStatus: MoneyStatus = MoneyStatus.NOT_STARTED,
    ) = CustomerSummary(
        customerId = "customer",
        customerStatus = customerStatus,
        isHidden = isHidden,
        kycStatus = kycStatus,
        paymentAccount = PaymentAccount(
            id = null,
            activation = PaymentAccount.Activation(status = paymentStatus, step = paymentStep, failureReason = null),
            cards = CustomerSummary.Cards(active = 0, frozen = 0, pending = 0),
        ),
        moneyAccount = MoneyAccount(
            id = null,
            activation = MoneyAccount.Activation(status = moneyStatus, step = null, failureReason = null),
        ),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}