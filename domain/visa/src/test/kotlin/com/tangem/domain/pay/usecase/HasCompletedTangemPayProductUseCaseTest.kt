package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.pay.model.CustomerSummary.PaymentAccount
import com.tangem.domain.visa.error.VisaApiError
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
internal class HasCompletedTangemPayProductUseCaseTest {

    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase = mockk()

    private val useCase = HasCompletedTangemPayProductUseCase(getCustomerSummaryUseCase = getCustomerSummaryUseCase)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getCustomerSummaryUseCase)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN product statuses WHEN invoke THEN returns whether any product is completed`(model: StatusesModel) =
        runTest {
            // Arrange
            coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.CACHE_FIRST) } returns
                customerSummary(paymentStatus = model.paymentStatus, moneyStatus = model.moneyStatus).right()

            // Act
            val actual = useCase(USER_WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(model.expected.right())
        }

    @Test
    fun `GIVEN summary request fails WHEN invoke THEN returns error`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.CACHE_FIRST) } returns
            VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    @Test
    fun `GIVEN no summary WHEN invoke THEN returns error`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.CACHE_FIRST) } returns null.right()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.UnknownWithoutCode.left())
    }

    data class StatusesModel(
        val paymentStatus: PaymentAccount.Activation.Status,
        val moneyStatus: MoneyAccount.Activation.Status,
        val expected: Boolean,
    )

    private fun provideTestModels() = listOf(
        StatusesModel(PaymentAccount.Activation.Status.COMPLETED, MoneyAccount.Activation.Status.NOT_STARTED, true),
        StatusesModel(PaymentAccount.Activation.Status.NOT_STARTED, MoneyAccount.Activation.Status.COMPLETED, true),
        StatusesModel(PaymentAccount.Activation.Status.COMPLETED, MoneyAccount.Activation.Status.COMPLETED, true),
        StatusesModel(PaymentAccount.Activation.Status.NOT_STARTED, MoneyAccount.Activation.Status.NOT_STARTED, false),
        StatusesModel(PaymentAccount.Activation.Status.PROCESSING, MoneyAccount.Activation.Status.PROCESSING, false),
        StatusesModel(PaymentAccount.Activation.Status.FAILED, MoneyAccount.Activation.Status.DEACTIVATED, false),
        StatusesModel(PaymentAccount.Activation.Status.UNAVAILABLE, MoneyAccount.Activation.Status.UNAVAILABLE, false),
        StatusesModel(PaymentAccount.Activation.Status.UNKNOWN, MoneyAccount.Activation.Status.UNKNOWN, false),
    )

    private fun customerSummary(
        paymentStatus: PaymentAccount.Activation.Status,
        moneyStatus: MoneyAccount.Activation.Status,
    ) = CustomerSummary(
        customerId = "customer",
        customerStatus = CustomerSummary.CustomerStatus.ACTIVE,
        isHidden = false,
        kycStatus = CustomerSummary.KycStatus.APPROVED,
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