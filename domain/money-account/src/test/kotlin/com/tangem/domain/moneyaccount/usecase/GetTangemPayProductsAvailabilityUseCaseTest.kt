package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.tangempay.model.CustomerSummary.PaymentAccount
import com.tangem.domain.tangempay.usecase.GetCustomerSummaryUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetTangemPayProductsAvailabilityUseCaseTest {

    private val isMoneyAccountPathOccupiedUseCase: IsMoneyAccountPathOccupiedUseCase = mockk()
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase = mockk()
    private val userWalletId: UserWalletId = mockk()

    private val useCase = GetTangemPayProductsAvailabilityUseCase(
        isMoneyAccountPathOccupiedUseCase = isMoneyAccountPathOccupiedUseCase,
        getCustomerSummaryUseCase = getCustomerSummaryUseCase,
    )

    private val allProducts = setOf(TangemPayProduct.BANKING_ACCOUNT, TangemPayProduct.PAYMENT_ACCOUNT)

    @BeforeEach
    fun setup() {
        clearMocks(isMoneyAccountPathOccupiedUseCase, getCustomerSummaryUseCase)
        coEvery { getCustomerSummaryUseCase(userWalletId) } returns summary().right()
        coEvery { isMoneyAccountPathOccupiedUseCase(userWalletId) } returns false
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN account activation status WHEN invoke THEN product availability matches status`(
        model: StatusModel,
    ) = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(userWalletId) } returns
            summary(moneyStatus = model.moneyStatus, paymentStatus = model.paymentStatus).right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(model.expected.right())
    }

    private fun provideTestModels(): List<StatusModel> {
        val activatableMoneyStatuses = setOf(
            MoneyAccount.Activation.Status.NOT_STARTED,
            MoneyAccount.Activation.Status.FAILED,
        )
        val activatablePaymentStatuses = setOf(
            PaymentAccount.Activation.Status.NOT_STARTED,
            PaymentAccount.Activation.Status.FAILED,
        )
        val moneyModels = MoneyAccount.Activation.Status.entries.map { status ->
            StatusModel(
                moneyStatus = status,
                paymentStatus = PaymentAccount.Activation.Status.NOT_STARTED,
                expected = if (status in activatableMoneyStatuses) {
                    allProducts
                } else {
                    setOf(TangemPayProduct.PAYMENT_ACCOUNT)
                },
            )
        }
        val paymentModels = PaymentAccount.Activation.Status.entries.map { status ->
            StatusModel(
                moneyStatus = MoneyAccount.Activation.Status.NOT_STARTED,
                paymentStatus = status,
                expected = if (status in activatablePaymentStatuses) {
                    allProducts
                } else {
                    setOf(TangemPayProduct.BANKING_ACCOUNT)
                },
            )
        }
        return moneyModels + paymentModels
    }

    @Test
    fun `GIVEN money account path occupied WHEN invoke THEN banking account is excluded`() = runTest {
        // Arrange
        coEvery { isMoneyAccountPathOccupiedUseCase(userWalletId) } returns true

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(setOf(TangemPayProduct.PAYMENT_ACCOUNT).right())
    }

    @Test
    fun `GIVEN money account completed WHEN invoke THEN path is not checked`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(userWalletId) } returns
            summary(moneyStatus = MoneyAccount.Activation.Status.COMPLETED).right()

        // Act
        useCase(userWalletId)

        // Assert
        coVerify(exactly = 0) { isMoneyAccountPathOccupiedUseCase(any()) }
    }

    @Test
    fun `GIVEN both accounts completed WHEN invoke THEN no products returned`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(userWalletId) } returns summary(
            moneyStatus = MoneyAccount.Activation.Status.COMPLETED,
            paymentStatus = PaymentAccount.Activation.Status.COMPLETED,
        ).right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(emptySet<TangemPayProduct>().right())
    }

    @Test
    fun `GIVEN customer summary fails WHEN invoke THEN returns error`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(userWalletId) } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(TangemPayApiError.Unspecified.left())
        coVerify(exactly = 0) { isMoneyAccountPathOccupiedUseCase(any()) }
    }

    @Test
    fun `GIVEN customer summary is null WHEN invoke THEN returns error`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(userWalletId) } returns null.right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(TangemPayApiError.UnknownWithoutCode.left())
    }

    private fun summary(
        moneyStatus: MoneyAccount.Activation.Status = MoneyAccount.Activation.Status.NOT_STARTED,
        paymentStatus: PaymentAccount.Activation.Status = PaymentAccount.Activation.Status.NOT_STARTED,
    ) = CustomerSummary(
        customerId = "customer_id",
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

    data class StatusModel(
        val moneyStatus: MoneyAccount.Activation.Status,
        val paymentStatus: PaymentAccount.Activation.Status,
        val expected: Set<TangemPayProduct>,
    ) {
        override fun toString(): String = "money=$moneyStatus, payment=$paymentStatus"
    }
}