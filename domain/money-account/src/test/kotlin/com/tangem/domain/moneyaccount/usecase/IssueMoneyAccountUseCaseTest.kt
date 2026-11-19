package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.IssueMoneyAccountError
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.MoneyAccountDepositCredentials
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IssueMoneyAccountUseCaseTest {

    private val activateMoneyAccountUseCase: ActivateMoneyAccountUseCase = mockk()
    private val orderRepository: TangemPayOrderRepository = mockk()
    private val pollTangemPayAccountStatusesUseCase: PollTangemPayAccountStatusesUseCase = mockk()

    private val useCase = IssueMoneyAccountUseCase(
        activateMoneyAccountUseCase = activateMoneyAccountUseCase,
        orderRepository = orderRepository,
        pollTangemPayAccountStatusesUseCase = pollTangemPayAccountStatusesUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(activateMoneyAccountUseCase, orderRepository, pollTangemPayAccountStatusesUseCase)
        every { pollTangemPayAccountStatusesUseCase(any()) } returns emptyFlow()
    }

    @Test
    fun `GIVEN activation succeeds WHEN invoke THEN banking account issue order is created and status polling started`() = runTest {
        // Arrange
        coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns CREDENTIALS.right()
        coEvery { orderRepository.createOrder(USER_WALLET_ID, any(), any()) } returns mockk<TangemPayOrder>().right()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            orderRepository.createOrder(
                userWalletId = USER_WALLET_ID,
                request = CustomerOrderRequest.AccountIssueBanking(depositCredentials = CREDENTIALS),
                idempotencyKey = any(),
            )
        }
        verify(exactly = 1) { pollTangemPayAccountStatusesUseCase(USER_WALLET_ID) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN activation fails WHEN invoke THEN mapped error and no order created`(model: ActivationErrorModel) =
        runTest {
            // Arrange
            coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns model.activationError.left()

            // Act
            val actual = useCase(USER_WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(model.expected.left())
            coVerify(exactly = 0) { orderRepository.createOrder(any(), any(), any()) }
        }

    @Test
    fun `GIVEN order creation fails WHEN invoke THEN failed`() = runTest {
        // Arrange
        coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns CREDENTIALS.right()
        coEvery {
            orderRepository.createOrder(USER_WALLET_ID, any(), any())
        } returns TangemPayApiError.Unspecified.left()

        // Act
        val actual: Either<IssueMoneyAccountError, Unit> = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(IssueMoneyAccountError.Failed.left())
        verify(exactly = 0) { pollTangemPayAccountStatusesUseCase(any()) }
    }

    private fun provideTestModels() = listOf(
        ActivationErrorModel(MoneyAccountActivationError.UserCancelled, IssueMoneyAccountError.UserCancelled),
        ActivationErrorModel(MoneyAccountActivationError.PersistenceFailed, IssueMoneyAccountError.Failed),
        ActivationErrorModel(MoneyAccountActivationError.Unknown, IssueMoneyAccountError.Failed),
    )

    internal data class ActivationErrorModel(
        val activationError: MoneyAccountActivationError,
        val expected: IssueMoneyAccountError,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val CREDENTIALS = MoneyAccountDepositCredentials(address = "0xDEPOSIT", signature = "0xSIGNATURE")
    }
}