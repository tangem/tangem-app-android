package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class AwaitBankOfframpSignatureUseCaseTest {

    private val repository: BankOfframpOrderRepository = mockk()
    private val useCase = AwaitBankOfframpSignatureUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN order processing twice then awaiting signature WHEN invoke THEN hash returned after three polls`() = runTest {
        // Arrange
        coEvery { repository.getOrderState(TEST_USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
            BankOfframpOrderState.Processing(OrderStep.UNKNOWN).right(),
            BankOfframpOrderState.Processing(OrderStep.UNKNOWN).right(),
            BankOfframpOrderState.AwaitingSignature(payloadHash = "0xhash").right(),
        )

        // Act
        val actual = useCase(TEST_USER_WALLET_ID, ORDER_ID)

        // Assert
        assertThat(actual).isEqualTo("0xhash".right())
        coVerify(exactly = 3) { repository.getOrderState(TEST_USER_WALLET_ID, ORDER_ID) }
    }

    @Test
    fun `GIVEN order canceled WHEN invoke THEN unknown error`() = runTest {
        // Arrange
        coEvery { repository.getOrderState(TEST_USER_WALLET_ID, ORDER_ID) } returns BankOfframpOrderState.Canceled.right()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID, ORDER_ID)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(BankOfframpError.Unknown::class.java)
    }

    @Test
    fun `GIVEN order failed WHEN invoke THEN its error is returned`() = runTest {
        // Arrange
        coEvery { repository.getOrderState(TEST_USER_WALLET_ID, ORDER_ID) } returns
            BankOfframpOrderState.Failed(BankOfframpError.WithdrawalInProgress).right()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID, ORDER_ID)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.WithdrawalInProgress.left())
    }

    @Test
    fun `GIVEN order never leaves processing WHEN timeout elapses THEN four polls and polling timeout error`() =
        runTest {
            // Arrange
            coEvery { repository.getOrderState(TEST_USER_WALLET_ID, ORDER_ID) } returns
                BankOfframpOrderState.Processing(OrderStep.UNKNOWN).right()

            // Act
            val actual = useCase(TEST_USER_WALLET_ID, ORDER_ID, timeout = 12.seconds)

            // Assert
            assertThat(actual).isEqualTo(BankOfframpError.PollingTimeout.left())
            coVerify(exactly = 4) { repository.getOrderState(TEST_USER_WALLET_ID, ORDER_ID) }
        }

    private companion object {
        const val ORDER_ID = "order-1"
    }
}