package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.TangemPayCloseCardRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CloseTangemPayCardUseCaseTest {

    private val closeCardRepository: TangemPayCloseCardRepository = mockk(relaxUnitFun = true)
    private val startPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(closeCardRepository, startPollingUseCase, paymentAccountStatusFetcher)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN closeCard fails or returns CANCELED order WHEN invoke THEN returns Left and skips store, fetch and polling`(
        closeCardResult: Either<TangemPayApiError, TangemPayOrderInfo>,
    ) = runTest {
        // Arrange
        val useCase = createUseCase()
        coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns closeCardResult

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { closeCardRepository.storeCloseOrderId(any(), any()) }
        coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
        coVerify(exactly = 0) { startPollingUseCase(any(), any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(OrderStatus::class, names = ["PROCESSING", "COMPLETED"])
    fun `GIVEN closeCard returns PROCESSING or COMPLETED order WHEN invoke THEN stores order id, fetches status and starts polling`(
        status: OrderStatus,
    ) = runTest {
        // Arrange
        val useCase = createUseCase()
        val order = TangemPayOrderInfo(ORDER_ID, status)
        coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
        coEvery { closeCardRepository.storeCloseOrderId(CARD_ID, ORDER_ID) } returns Unit.right()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()
        coEvery { startPollingUseCase(order, USER_WALLET_ID, any()) } returns true

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerifyOrder {
            closeCardRepository.storeCloseOrderId(CARD_ID, ORDER_ID)
            paymentAccountStatusFetcher.invoke(USER_WALLET_ID)
            startPollingUseCase(order, USER_WALLET_ID, any())
        }
        coVerify(exactly = 1) { closeCardRepository.storeCloseOrderId(CARD_ID, ORDER_ID) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
        coVerify(exactly = 1) { startPollingUseCase(order, USER_WALLET_ID, any()) }
    }

    private fun provideTestModels(): List<Either<TangemPayApiError, TangemPayOrderInfo>> = listOf(
        TangemPayApiError.Unspecified.left(),
        TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED).right(),
    )

    private fun createUseCase() = CloseTangemPayCardUseCase(
        closeCardRepository = closeCardRepository,
        startTangemPayOrderPollingUseCase = startPollingUseCase,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val CARD_ID = "card-test-id"
        const val ORDER_ID = "order-test-1"
    }
}