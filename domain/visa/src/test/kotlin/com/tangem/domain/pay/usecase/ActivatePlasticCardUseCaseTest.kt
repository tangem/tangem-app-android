package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CardActivationOrder
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ActivatePlasticCardUseCaseTest {

    private val orderRepository: CustomerOrderRepository = mockk()
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    private val appCoroutineScope = TestAppCoroutineScope(UnconfinedTestDispatcher() + SupervisorJob())

    private val useCase = ActivatePlasticCardUseCase(
        customerOrderRepository = orderRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        appCoroutineScope = appCoroutineScope,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, startTangemPayOrderPollingUseCase, paymentAccountStatusFetcher)
        coEvery { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) } returns Unit.right()
    }

    @Test
    fun `GIVEN the last digits match WHEN invoked THEN refreshes the account and starts polling`() = runTest {
        // Arrange
        val created = order(status = OrderStatus.PROCESSING)
        coEvery {
            orderRepository.createCardActivationOrder(
                userWalletId = USER_WALLET_ID,
                order = CardActivationOrder(
                    productInstanceId = PRODUCT_INSTANCE_ID,
                    lastFourDigits = LAST_FOUR_DIGITS,
                ),
                idempotencyKey = IDEMPOTENCY_KEY,
            )
        } returns created.right()
        coEvery { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) } returns true

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(created),
                userWalletId = USER_WALLET_ID,
                onOrderStateChange = null,
                timeout = null,
            )
        }
    }

    @Test
    fun `GIVEN the order is still processing WHEN invoked THEN succeeds without awaiting the poller`() = runTest {
        // Arrange
        coEvery { orderRepository.createCardActivationOrder(any(), any(), any()) } returns
            order(status = OrderStatus.PROCESSING).right()
        coEvery { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) } coAnswers { awaitCancellation() }

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @ParameterizedTest
    @MethodSource("provideSubmitErrorModels")
    fun `GIVEN the submit is rejected WHEN invoked THEN the typed error is returned`(
        model: SubmitErrorModel,
    ) = runTest {
        // Arrange
        coEvery { orderRepository.createCardActivationOrder(any(), any(), any()) } returns model.error.left()

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(model.error)
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("provideMalformedLastDigits")
    fun `GIVEN malformed last digits WHEN invoked THEN nothing is submitted`(lastFourDigits: String) = runTest {
        // Act
        val result = invokeUseCase(lastFourDigits = lastFourDigits)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { orderRepository.createCardActivationOrder(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the submit request throws WHEN invoked THEN collapses to Unspecified`() = runTest {
        // Arrange
        coEvery { orderRepository.createCardActivationOrder(any(), any(), any()) } throws
            IllegalStateException("Can not find customer address")

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN an already terminal order WHEN invoked THEN it is still handed to the poller`() = runTest {
        // Arrange
        val created = order(status = OrderStatus.COMPLETED)
        coEvery { orderRepository.createCardActivationOrder(any(), any(), any()) } returns created.right()
        coEvery { startTangemPayOrderPollingUseCase(any(), any(), any(), any()) } returns true

        // Act
        val result = invokeUseCase()

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo(orderId = ORDER_ID, orderStatus = OrderStatus.COMPLETED),
                userWalletId = USER_WALLET_ID,
                onOrderStateChange = null,
                timeout = null,
            )
        }
    }

    private suspend fun invokeUseCase(lastFourDigits: String = LAST_FOUR_DIGITS) = useCase(
        userWalletId = USER_WALLET_ID,
        activationOrder = CardActivationOrder(
            productInstanceId = PRODUCT_INSTANCE_ID,
            lastFourDigits = lastFourDigits,
        ),
        idempotencyKey = IDEMPOTENCY_KEY,
    )

    private fun provideSubmitErrorModels() = listOf(
        SubmitErrorModel(
            name = "wrong last digits",
            error = VisaApiError.CardActivationInvalidCardData,
        ),
        SubmitErrorModel(
            name = "target card is not physical",
            error = VisaApiError.CardActivationCardNotPhysical,
        ),
        SubmitErrorModel(
            name = "target card is already active",
            error = VisaApiError.CardActivationCardAlreadyActive,
        ),
        SubmitErrorModel(
            name = "target card is not ready for activation",
            error = VisaApiError.CardActivationCardNotReadyForActivation,
        ),
        SubmitErrorModel(
            name = "an activation order already exists",
            error = VisaApiError.CardActivationActiveOrderExists,
        ),
        SubmitErrorModel(
            name = "backend unavailable",
            error = VisaApiError.ServerUnavailable,
        ),
    )

    private fun provideMalformedLastDigits() = listOf("", "12", "12345", "12a4", " 123")

    internal data class SubmitErrorModel(val name: String, val error: VisaApiError) {
        override fun toString(): String = name
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("1234567890ABCDEF")
        const val PRODUCT_INSTANCE_ID = "pi-1"
        const val ORDER_ID = "activation-order-1"
        const val LAST_FOUR_DIGITS = "8252"
        const val IDEMPOTENCY_KEY = "6f1c9e2a-0b3d-4c5e-8a7b-9d0e1f2a3b4c"

        fun order(status: OrderStatus) = Order(
            id = ORDER_ID,
            customerId = "customer",
            type = OrderType.CARD_ACTIVATION_PLASTIC_RAIN,
            status = status,
            step = OrderStep.UNKNOWN,
            stepChangeCode = null,
            productInstanceId = PRODUCT_INSTANCE_ID,
            paymentAccountId = null,
            cardId = null,
            toTariffPlanId = null,
            withdrawTxHash = null,
            createdAt = null,
            updatedAt = null,
        )
    }
}