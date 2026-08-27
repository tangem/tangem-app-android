package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

internal class StartTangemPayOrderPollingUseCaseTest {

    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    private val useCase = StartTangemPayOrderPollingUseCase(
        cardDetailsRepository = cardDetailsRepository,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
    )

    @Test
    fun `GIVEN order already COMPLETED WHEN invoke THEN returns true without polling`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
        coVerify(exactly = 0) { cardDetailsRepository.getOrderInfo(any(), any()) }
    }

    @Test
    fun `GIVEN order already CANCELED WHEN invoke THEN returns false without polling`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED)
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isFalse()
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
        coVerify(exactly = 0) { cardDetailsRepository.getOrderInfo(any(), any()) }
    }

    @Test
    fun `GIVEN processing order WHEN poll returns COMPLETED THEN returns true and fetches status`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returns TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 1) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN processing order WHEN poll returns CANCELED THEN returns false and fetches status`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returns TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED).right()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isFalse()
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN new order WHEN getOrderInfo fails once then returns COMPLETED THEN returns true after two polls`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.NEW)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returnsMany listOf(
            VisaApiError.Unspecified.left(),
            TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right(),
        )
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 2) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
    }

    @Test
    fun `GIVEN processing order WHEN multiple non-final polls then COMPLETED THEN returns true after all polls`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returnsMany listOf(
            TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING).right(),
            TangemPayOrderInfo(ORDER_ID, OrderStatus.NEW).right(),
            TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right(),
        )
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 3) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN order already being polled WHEN invoke again for same order THEN returns false without a second poll`() =
        runTest {
            // Arrange — first poller never reaches a terminal status, so it keeps polling.
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            coEvery { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) } returns
                TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING).right()

            // Act — start the first poller, let it register the order and park in its poll delay,
            // then invoke again for the same order.
            val firstPoller = launch { useCase(order, USER_WALLET_ID) }
            runCurrent()
            val secondResult = useCase(order, USER_WALLET_ID)

            // Assert — the duplicate invoke is a no-op (no extra getOrderInfo, no status fetch).
            assertThat(secondResult).isFalse()
            coVerify(exactly = 1) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
            coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }

            firstPoller.cancel()
        }

    @Test
    fun `GIVEN processing order WHEN step changes while non-terminal THEN onOrderStateChange gets every new order`() =
        runTest {
            // Arrange — a step-only transition (PROCESSING/AWAITING_DEPOSIT) must be reported, which is only
            // observable because the step is part of the order identity.
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            val awaitingDeposit = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING, OrderStep.AWAITING_DEPOSIT)
            val completed = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
            val changes = mutableListOf<TangemPayOrderInfo>()
            coEvery {
                cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
            } returnsMany listOf(awaitingDeposit.right(), completed.right())
            coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

            // Act
            val result = useCase(order, USER_WALLET_ID, onOrderStateChange = { changes.add(it) })

            // Assert — the terminal state is reported as well, so callers see the whole transition chain.
            assertThat(result).isTrue()
            assertThat(changes).containsExactly(awaitingDeposit, completed).inOrder()
        }

    @Test
    fun `GIVEN order already in target step WHEN poll returns the same step THEN onOrderStateChange still reports it`() =
        runTest {
            // Arrange — a restored order can already sit in AWAITING_DEPOSIT; the first poll then returns a
            // value equal to the incoming one and must still be reported.
            val awaitingDeposit = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING, OrderStep.AWAITING_DEPOSIT)
            val completed = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
            val changes = mutableListOf<TangemPayOrderInfo>()
            coEvery {
                cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
            } returnsMany listOf(awaitingDeposit.right(), completed.right())
            coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

            // Act
            val result = useCase(awaitingDeposit, USER_WALLET_ID, onOrderStateChange = { changes.add(it) })

            // Assert
            assertThat(result).isTrue()
            assertThat(changes).containsExactly(awaitingDeposit, completed).inOrder()
        }

    @Test
    fun `GIVEN processing order WHEN poll returns terminal THEN terminal is reported before status fetch`() = runTest {
        // Arrange — recording both the callback and the fetch proves callers can clear the order hint before
        // the refresh that would otherwise re-issue GET /order/{id} for the just-resolved order.
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        val events = mutableListOf<String>()
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returns TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } answers {
            events.add("fetch")
            Unit.right()
        }

        // Act
        val result = useCase(
            order = order,
            userWalletId = USER_WALLET_ID,
            onOrderStateChange = { events.add(if (it.orderStatus.isTerminal) "terminal" else "changed") },
        )

        // Assert
        assertThat(result).isTrue()
        assertThat(events).containsExactly("terminal", "fetch").inOrder()
    }

    @Test
    fun `GIVEN order already being polled WHEN invoke again THEN onOrderStateChange is not invoked for duplicate`() =
        runTest {
            // Arrange — first poller never reaches terminal, so it keeps polling.
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            coEvery { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) } returns
                TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING).right()
            var duplicateNotified = false

            // Act — start the first poller, then invoke again for the same order.
            val firstPoller = launch { useCase(order, USER_WALLET_ID) }
            runCurrent()
            val secondResult = useCase(order, USER_WALLET_ID, onOrderStateChange = { duplicateNotified = true })

            // Assert — the duplicate is a no-op: it must not report state for the live poller, otherwise the
            // caller would clear the hint of that poller and hide the in-flight order from the status.
            assertThat(secondResult).isFalse()
            assertThat(duplicateNotified).isFalse()

            firstPoller.cancel()
        }

    @Test
    fun `GIVEN processing order WHEN backend reports order not found THEN resolves as CANCELED and stops`() = runTest {
        // GIVEN
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        val changes = mutableListOf<TangemPayOrderInfo>()
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returns VisaApiError.OrderNotFound.left()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        // WHEN
        val result = useCase(order, USER_WALLET_ID, onOrderStateChange = { changes.add(it) })

        // THEN
        assertThat(result).isFalse()
        assertThat(changes).containsExactly(TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED))
        coVerify(exactly = 1) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN long outage WHEN backend recovers THEN order is still polled to terminal`() = runTest {
        // GIVEN
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        val outage = List(size = 100) { VisaApiError.ServerUnavailable.left() }
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returnsMany outage + TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        // WHEN
        val result = useCase(order, USER_WALLET_ID)

        // THEN
        assertThat(result).isTrue()
        coVerify(exactly = 101) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
    }

    @Test
    fun `GIVEN non-terminal order WHEN polling THEN polls at a flat 5s cadence`() = runTest {
        // GIVEN
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returnsMany listOf(
            TangemPayOrderInfo(ORDER_ID, OrderStatus.NEW).right(),
            VisaApiError.ServerUnavailable.left(),
            TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right(),
        )
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        // WHEN
        val result = useCase(order, USER_WALLET_ID)

        // THEN
        assertThat(result).isTrue()
        assertThat(testScheduler.currentTime).isEqualTo(10_000L)
    }

    @Test
    fun `GIVEN never-terminal order WHEN invoke with a short timeout THEN returns false and stops polling`() =
        runTest {
            // Arrange — the order never reaches a terminal status across any number of polls.
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            coEvery { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) } returns
                TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING).right()

            // Act — timeout (100ms) is far shorter than the 3s polling delay, so it elapses mid-poll.
            val result = useCase(order, USER_WALLET_ID, timeout = 100.milliseconds)

            // Assert — timed out without ever reaching a terminal status, so no status fetch either.
            assertThat(result).isFalse()
            coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
        }

    @Test
    fun `GIVEN order already COMPLETED WHEN invoke with a timeout THEN returns true without polling`() = runTest {
        // A terminal order resolves immediately regardless of timeout — no polling to time out on.
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID, timeout = 100.milliseconds)

        assertThat(result).isTrue()
        coVerify(exactly = 0) { cardDetailsRepository.getOrderInfo(any(), any()) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val ORDER_ID = "order-test-1"
    }
}