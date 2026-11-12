package com.tangem.domain.tangempay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CachePolicy
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.model.CustomerSummary.CustomerStatus
import com.tangem.domain.tangempay.model.CustomerSummary.KycStatus
import com.tangem.domain.tangempay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.tangempay.model.CustomerSummary.PaymentAccount
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsTangemPayCustomerVerifiedUseCaseTest {

    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase = mockk()

    private val useCase = IsTangemPayCustomerVerifiedUseCase(getCustomerSummaryUseCase = getCustomerSummaryUseCase)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getCustomerSummaryUseCase)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN customer summary WHEN invoke THEN returns whether customer is verified`(model: SummaryModel) =
        runTest {
            // Arrange
            coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.REFRESH) } returns
                model.summary.right()

            // Act
            val actual = useCase(USER_WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(model.expected.right())
        }

    @Test
    fun `GIVEN summary request fails WHEN invoke THEN returns error`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.REFRESH) } returns
            TangemPayApiError.Unspecified.left()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(TangemPayApiError.Unspecified.left())
    }

    @Test
    fun `GIVEN no summary WHEN invoke THEN returns error`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.REFRESH) } returns null.right()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(TangemPayApiError.UnknownWithoutCode.left())
    }

    data class SummaryModel(val summary: CustomerSummary, val expected: Boolean)

    private fun provideTestModels() = listOf(
        SummaryModel(summary = customerSummary(), expected = true),
        SummaryModel(
            summary = customerSummary(
                paymentStatus = PaymentAccount.Activation.Status.UNAVAILABLE,
                moneyStatus = MoneyAccount.Activation.Status.UNAVAILABLE,
            ),
            expected = true,
        ),
        SummaryModel(
            summary = customerSummary(paymentStatus = PaymentAccount.Activation.Status.COMPLETED),
            expected = false,
        ),
        SummaryModel(
            summary = customerSummary(paymentStatus = PaymentAccount.Activation.Status.PROCESSING),
            expected = false,
        ),
        SummaryModel(
            summary = customerSummary(moneyStatus = MoneyAccount.Activation.Status.COMPLETED),
            expected = false,
        ),
        SummaryModel(
            summary = customerSummary(moneyStatus = MoneyAccount.Activation.Status.DEACTIVATED),
            expected = false,
        ),
        SummaryModel(
            summary = customerSummary(moneyStatus = MoneyAccount.Activation.Status.FAILED),
            expected = false,
        ),
        SummaryModel(summary = customerSummary(kycStatus = KycStatus.REQUIRED), expected = false),
        SummaryModel(summary = customerSummary(kycStatus = KycStatus.IN_PROGRESS), expected = false),
        SummaryModel(summary = customerSummary(customerStatus = CustomerStatus.BLOCKED), expected = false),
        SummaryModel(summary = customerSummary(customerStatus = CustomerStatus.FORMER), expected = false),
    )

    private fun customerSummary(
        customerStatus: CustomerStatus = CustomerStatus.ACTIVE,
        kycStatus: KycStatus = KycStatus.APPROVED,
        paymentStatus: PaymentAccount.Activation.Status = PaymentAccount.Activation.Status.NOT_STARTED,
        moneyStatus: MoneyAccount.Activation.Status = MoneyAccount.Activation.Status.NOT_STARTED,
    ) = CustomerSummary(
        customerId = "customer",
        customerStatus = customerStatus,
        isHidden = false,
        kycStatus = kycStatus,
        paymentAccount = PaymentAccount(
            id = null,
            activation = PaymentAccount.Activation(status = paymentStatus, step = null, failureReason = null),
            cards = CustomerSummary.Cards(active = 0, frozen = 0, pending = 0),
        ),
        moneyAccount = MoneyAccount(
            id = null,
            activation = MoneyAccount.Activation(status = moneyStatus, step = null, failureReason = null),
        ),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
    }
}