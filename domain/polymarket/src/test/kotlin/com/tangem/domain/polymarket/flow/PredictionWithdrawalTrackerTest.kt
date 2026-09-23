package com.tangem.domain.polymarket.flow

import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionWithdrawalStore
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.interactor.PredictionWithdrawalFailure
import com.tangem.domain.polymarket.interactor.SubmitPredictionWithdrawalInteractor
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.domain.polymarket.model.PredictionWithdrawalBatch
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord
import com.tangem.domain.polymarket.model.PredictionWithdrawalState
import com.tangem.domain.polymarket.model.PredictionWithdrawalTrackingConfig
import com.tangem.domain.polymarket.usecase.GetPredictionWithdrawalStateUseCase
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class PredictionWithdrawalTrackerTest {

    private val store: PredictionWithdrawalStore = mockk()
    private val submitInteractor: SubmitPredictionWithdrawalInteractor = mockk()
    private val getState: GetPredictionWithdrawalStateUseCase = mockk()
    private val config = PredictionWithdrawalTrackingConfig(interval = 5.seconds, maxAttempts = 3)

    @BeforeEach
    fun resetMocks() {
        clearMocks(store, submitInteractor, getState)
        coEvery { store.clear(any()) } returns Unit
    }

    @Test
    fun `GIVEN a recorded id WHEN tracked THEN the state is polled every interval until confirmed and the record is cleared`() =
        runTest {
            // Arrange
            coEvery { store.get(WALLET_ID) } returns record(transactionId = TX_ID)
            coEvery { getState(walletId = WALLET_ID.stringValue, transactionId = TX_ID) } returnsMany listOf(
                receipt(PredictionWithdrawalState.IN_PROGRESS).right(),
                receipt(PredictionWithdrawalState.CONFIRMED).right(),
            )
            val tracker = createTracker()

            // Act
            tracker.track(WALLET_ID)
            advanceTimeBy(5.seconds)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { getState(walletId = WALLET_ID.stringValue, transactionId = TX_ID) }
            coVerify(exactly = 0) { store.clear(WALLET_ID) }
            advanceTimeBy(5.seconds)
            runCurrent()
            coVerify(exactly = 2) { getState(walletId = WALLET_ID.stringValue, transactionId = TX_ID) }
            coVerify(exactly = 1) { store.clear(WALLET_ID) }
            advanceTimeBy(5.seconds)
            runCurrent()
            coVerify(exactly = 2) { getState(walletId = WALLET_ID.stringValue, transactionId = TX_ID) }
        }

    @Test
    fun `GIVEN a record without an id WHEN tracked THEN the batch is resent once and its id is polled`() = runTest {
        // Arrange
        coEvery { store.get(WALLET_ID) } returns record(transactionId = null)
        coEvery {
            submitInteractor.resend(userWalletId = WALLET_ID, walletId = WALLET_ID.stringValue)
        } returns receipt(PredictionWithdrawalState.IN_PROGRESS).right()
        coEvery { getState(walletId = WALLET_ID.stringValue, transactionId = TX_ID) } returns
            receipt(PredictionWithdrawalState.CONFIRMED).right()
        val tracker = createTracker()

        // Act
        tracker.track(WALLET_ID)
        advanceTimeBy(5.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { submitInteractor.resend(userWalletId = WALLET_ID, walletId = WALLET_ID.stringValue) }
        coVerify(exactly = 1) { getState(walletId = WALLET_ID.stringValue, transactionId = TX_ID) }
        coVerify(exactly = 1) { store.clear(WALLET_ID) }
    }

    @Test
    fun `GIVEN the resend is refused WHEN tracked THEN nothing is polled and the record stays`() = runTest {
        // Arrange
        coEvery { store.get(WALLET_ID) } returns record(transactionId = null)
        coEvery { submitInteractor.resend(any(), any()) } returns
            PredictionWithdrawalFailure.Rejected(PredictionWithdrawError.NonceTaken(detail = null)).left()
        val tracker = createTracker()

        // Act
        tracker.track(WALLET_ID)
        advanceTimeBy(20.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 0) { getState(any(), any()) }
        coVerify(exactly = 0) { store.clear(any()) }
    }

    @Test
    fun `GIVEN the resend answers with a terminal receipt WHEN tracked THEN the record is cleared and nothing is polled`() =
        runTest {
            // Arrange
            coEvery { store.get(WALLET_ID) } returns record(transactionId = null)
            coEvery {
                submitInteractor.resend(userWalletId = WALLET_ID, walletId = WALLET_ID.stringValue)
            } returns receipt(PredictionWithdrawalState.CONFIRMED).right()
            val tracker = createTracker()

            // Act
            tracker.track(WALLET_ID)
            advanceTimeBy(20.seconds)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { store.clear(WALLET_ID) }
            coVerify(exactly = 0) { getState(any(), any()) }
        }

    @Test
    fun `GIVEN no record WHEN tracked THEN nothing is resent or polled`() = runTest {
        // Arrange
        coEvery { store.get(WALLET_ID) } returns null
        val tracker = createTracker()

        // Act
        tracker.track(WALLET_ID)
        advanceTimeBy(20.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 0) { submitInteractor.resend(any(), any()) }
        coVerify(exactly = 0) { getState(any(), any()) }
    }

    @Test
    fun `GIVEN leg one fails WHEN polled THEN the record is cleared and polling stops`() = runTest {
        // Arrange
        coEvery { store.get(WALLET_ID) } returns record(transactionId = TX_ID)
        coEvery { getState(any(), any()) } returns receipt(PredictionWithdrawalState.FAILED).right()
        val tracker = createTracker()

        // Act
        tracker.track(WALLET_ID)
        advanceTimeBy(20.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { getState(any(), any()) }
        coVerify(exactly = 1) { store.clear(WALLET_ID) }
    }

    @Test
    fun `GIVEN the finished record cannot be cleared WHEN polled THEN the clear is retried on the next poll`() = runTest {
        // Arrange
        coEvery { store.get(WALLET_ID) } returns record(transactionId = TX_ID)
        coEvery { getState(any(), any()) } returns receipt(PredictionWithdrawalState.CONFIRMED).right()
        coEvery { store.clear(WALLET_ID) } throws IllegalStateException("disk full") andThen Unit
        val tracker = createTracker()

        // Act
        tracker.track(WALLET_ID)
        advanceTimeBy(20.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { getState(any(), any()) }
        coVerify(exactly = 2) { store.clear(WALLET_ID) }
    }

    @Test
    fun `GIVEN the id is not this wallet's WHEN polled THEN polling stops and the record stays`() = runTest {
        // Arrange
        coEvery { store.get(WALLET_ID) } returns record(transactionId = TX_ID)
        coEvery { getState(any(), any()) } returns PredictionWithdrawError.NotFound.left()
        val tracker = createTracker()

        // Act
        tracker.track(WALLET_ID)
        advanceTimeBy(20.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { getState(any(), any()) }
        coVerify(exactly = 0) { store.clear(any()) }
    }

    @Test
    fun `GIVEN the network drops WHEN polled THEN polling continues up to the attempt cap`() = runTest {
        // Arrange
        coEvery { store.get(WALLET_ID) } returns record(transactionId = TX_ID)
        coEvery { getState(any(), any()) } returns PredictionWithdrawError.Network.left()
        val tracker = createTracker()

        // Act
        tracker.track(WALLET_ID)
        advanceTimeBy(60.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 3) { getState(any(), any()) }
        coVerify(exactly = 0) { store.clear(any()) }
    }

    @Test
    fun `GIVEN a loop is running WHEN tracked again THEN one loop runs`() = runTest {
        // Arrange
        coEvery { store.get(WALLET_ID) } returns record(transactionId = TX_ID)
        coEvery { getState(any(), any()) } returns receipt(PredictionWithdrawalState.IN_PROGRESS).right()
        val tracker = createTracker()

        // Act
        tracker.track(WALLET_ID)
        tracker.track(WALLET_ID)
        advanceTimeBy(5.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { store.get(WALLET_ID) }
        coVerify(exactly = 1) { getState(any(), any()) }
    }

    private fun TestScope.createTracker() = PredictionWithdrawalTracker(
        store = store,
        submitInteractor = submitInteractor,
        getState = getState,
        config = config,
        appScope = TestAppCoroutineScope(coroutineContext = StandardTestDispatcher(testScheduler)),
    )

    private fun record(transactionId: String?) = PredictionWithdrawalRecord(
        batch = PredictionWithdrawalBatch(
            ownerAddress = "0x00000000000000000000000000000000000000a1",
            depositWalletAddress = "0x00000000000000000000000000000000000000d2",
            nonce = "9",
            deadline = "4102444800",
            call = PolymarketApprovalCall(target = PolymarketContracts.COLLATERAL, value = "0", data = "0xa9059cbb"),
            signature = "0xsig",
        ),
        transactionId = transactionId,
    )

    private fun receipt(state: PredictionWithdrawalState) = PredictionWithdrawalReceipt(
        transactionId = TX_ID,
        state = state,
        transactionHash = null,
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        const val TX_ID = "0x4f2c"
    }
}