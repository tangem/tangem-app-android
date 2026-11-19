package com.tangem.domain.polymarket

import com.tangem.domain.polymarket.sessionkey.EXPIRED_DEADLINE
import com.tangem.domain.polymarket.sessionkey.FUTURE_DEADLINE
import com.tangem.domain.polymarket.sessionkey.FakePolymarketPendingAuthorizationStore
import com.tangem.domain.polymarket.sessionkey.createPendingAuthorization
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketDepositWalletLockTest {

    private val pendingAuthorizations = FakePolymarketPendingAuthorizationStore()
    private val lock = PolymarketDepositWalletLock(pendingAuthorizations)

    @Test
    fun `GIVEN a session key operation holds the wallet WHEN a batch starts THEN it is busy at once`() = runTest {
        // Arrange
        val operating = CompletableDeferred<Unit>()
        launch { lock.withSessionKeyOperation(WALLET) { operating.await() } }
        runCurrent()
        var isBatchRun = false

        // Act
        val actual = lock.withBatchOrBusy(WALLET) { isBatchRun = true }

        // Assert
        assertThat(actual).isEqualTo(PolymarketDepositWalletLock.Busy.left())
        assertThat(isBatchRun).isFalse()
        operating.complete(Unit)
    }

    @Test
    fun `GIVEN a session key operation holds another wallet WHEN a batch starts THEN it runs`() = runTest {
        // Arrange
        val operating = CompletableDeferred<Unit>()
        launch { lock.withSessionKeyOperation(OTHER_WALLET) { operating.await() } }
        runCurrent()

        // Act
        val actual = lock.withBatchOrBusy(WALLET) { BATCH_RESULT }

        // Assert
        assertThat(actual).isEqualTo(BATCH_RESULT.right())
        operating.complete(Unit)
    }

    @Test
    fun `GIVEN a batch holds the wallet WHEN another batch starts THEN it waits and runs after`() = runTest {
        // Arrange
        val submitting = CompletableDeferred<Unit>()
        launch { lock.withBatchOrBusy(WALLET) { submitting.await() } }
        runCurrent()

        // Act
        val second = async { lock.withBatchOrBusy(WALLET) { BATCH_RESULT } }
        runCurrent()
        val isDoneWhileFirstHolds = second.isCompleted
        submitting.complete(Unit)

        // Assert
        assertThat(isDoneWhileFirstHolds).isFalse()
        assertThat(second.await()).isEqualTo(BATCH_RESULT.right())
    }

    @Test
    fun `GIVEN a batch holds the wallet WHEN a session key operation starts THEN it waits and runs after`() =
        runTest {
            // Arrange
            val submitting = CompletableDeferred<Unit>()
            launch { lock.withBatchOrBusy(WALLET) { submitting.await() } }
            runCurrent()

            // Act
            val operation = async { lock.withSessionKeyOperation(WALLET) { BATCH_RESULT } }
            runCurrent()
            val isDoneWhileBatchHolds = operation.isCompleted
            submitting.complete(Unit)

            // Assert
            assertThat(isDoneWhileBatchHolds).isFalse()
            assertThat(operation.await()).isEqualTo(BATCH_RESULT)
        }

    @Test
    fun `GIVEN a batch waits behind a batch WHEN a session key operation takes the wallet first THEN it is busy`() =
        runTest {
            // Arrange
            val submitting = CompletableDeferred<Unit>()
            val operating = CompletableDeferred<Unit>()
            launch { lock.withBatchOrBusy(WALLET) { submitting.await() } }
            runCurrent()
            val waiting = async { lock.withBatchOrBusy(WALLET) { BATCH_RESULT } }
            runCurrent()

            // Act
            submitting.complete(Unit)
            launch { lock.withSessionKeyOperation(WALLET) { operating.await() } }
            runCurrent()

            // Assert
            assertThat(waiting.await()).isEqualTo(PolymarketDepositWalletLock.Busy.left())
            operating.complete(Unit)
        }

    @Test
    fun `GIVEN a session key operation throws WHEN a batch starts after THEN it runs`() = runTest {
        // Arrange
        runCatching { lock.withSessionKeyOperation(WALLET) { error("relay down") } }

        // Act
        val actual = lock.withBatchOrBusy(WALLET) { BATCH_RESULT }

        // Assert
        assertThat(actual).isEqualTo(BATCH_RESULT.right())
    }

    @Test
    fun `GIVEN a session key operation is cancelled WHEN a batch starts after THEN it runs`() = runTest {
        // Arrange
        val operation = launch { lock.withSessionKeyOperation(WALLET) { awaitCancellation() } }
        runCurrent()
        operation.cancel()
        runCurrent()

        // Act
        val actual = lock.withBatchOrBusy(WALLET) { BATCH_RESULT }

        // Assert
        assertThat(actual).isEqualTo(BATCH_RESULT.right())
    }

    @Test
    fun `GIVEN a batch is cancelled WHEN a session key operation starts after THEN it runs`() = runTest {
        // Arrange
        val batch = launch { lock.withBatchOrBusy(WALLET) { awaitCancellation() } }
        runCurrent()
        batch.cancel()
        runCurrent()

        // Act
        val actual = lock.withSessionKeyOperation(WALLET) { BATCH_RESULT }

        // Assert
        assertThat(actual).isEqualTo(BATCH_RESULT)
    }

    @Test
    fun `GIVEN an executable pending authorization WHEN a batch starts THEN it is busy and not run`() = runTest {
        // Arrange
        pendingAuthorizations.store(WALLET, createPendingAuthorization(deadline = FUTURE_DEADLINE))
        var isBatchRun = false

        // Act
        val actual = lock.withBatchOrBusy(WALLET) { isBatchRun = true }

        // Assert
        assertThat(actual).isEqualTo(PolymarketDepositWalletLock.Busy.left())
        assertThat(isBatchRun).isFalse()
    }

    @Test
    fun `GIVEN a batch waits for the wallet WHEN an authorization is kept before it gets the lock THEN it is busy`() =
        runTest {
            // Arrange
            val submitting = CompletableDeferred<Unit>()
            launch { lock.withBatchOrBusy(WALLET) { submitting.await() } }
            runCurrent()
            var isBatchRun = false
            val waiting = async { lock.withBatchOrBusy(WALLET) { isBatchRun = true } }
            runCurrent()

            // Act
            pendingAuthorizations.store(WALLET, createPendingAuthorization(deadline = FUTURE_DEADLINE))
            submitting.complete(Unit)

            // Assert
            assertThat(waiting.await()).isEqualTo(PolymarketDepositWalletLock.Busy.left())
            assertThat(isBatchRun).isFalse()
        }

    @Test
    fun `GIVEN a batch was refused for a pending authorization WHEN a session key operation starts THEN it runs`() =
        runTest {
            // Arrange
            pendingAuthorizations.store(WALLET, createPendingAuthorization(deadline = FUTURE_DEADLINE))
            lock.withBatchOrBusy(WALLET) { BATCH_RESULT }

            // Act
            val actual = lock.withSessionKeyOperation(WALLET) { BATCH_RESULT }

            // Assert
            assertThat(actual).isEqualTo(BATCH_RESULT)
        }

    @Test
    fun `GIVEN an expired pending authorization WHEN a batch starts THEN it runs`() = runTest {
        // Arrange
        pendingAuthorizations.store(WALLET, createPendingAuthorization(deadline = EXPIRED_DEADLINE))

        // Act
        val actual = lock.withBatchOrBusy(WALLET) { BATCH_RESULT }

        // Assert
        assertThat(actual).isEqualTo(BATCH_RESULT.right())
    }

    @Test
    fun `GIVEN a pending authorization 20 s before its deadline WHEN a batch starts THEN it is busy`() = runTest {
        // Arrange
        pendingAuthorizations.store(WALLET, createPendingAuthorization(deadline = deadlineIn(seconds = 20)))

        // Act
        val actual = fixedClockLock().withBatchOrBusy(WALLET) { BATCH_RESULT }

        // Assert
        assertThat(actual).isEqualTo(PolymarketDepositWalletLock.Busy.left())
    }

    @Test
    fun `GIVEN a pending authorization past its deadline WHEN a batch starts THEN it runs`() = runTest {
        // Arrange
        pendingAuthorizations.store(WALLET, createPendingAuthorization(deadline = deadlineIn(seconds = 0)))

        // Act
        val actual = fixedClockLock().withBatchOrBusy(WALLET) { BATCH_RESULT }

        // Assert
        assertThat(actual).isEqualTo(BATCH_RESULT.right())
    }

    @Test
    fun `GIVEN the pending authorization cannot be read WHEN a batch starts THEN it is busy and not run`() = runTest {
        // Arrange
        pendingAuthorizations.readFailure = PolymarketSessionKeyError.Failure(IllegalStateException("Keystore locked"))
        var isBatchRun = false

        // Act
        val actual = lock.withBatchOrBusy(WALLET) { isBatchRun = true }

        // Assert
        assertThat(actual).isEqualTo(PolymarketDepositWalletLock.Busy.left())
        assertThat(isBatchRun).isFalse()
    }

    @Test
    fun `GIVEN a pending authorization 40 s before its deadline WHEN a batch starts THEN it is busy`() = runTest {
        // Arrange
        pendingAuthorizations.store(WALLET, createPendingAuthorization(deadline = deadlineIn(seconds = 40)))

        // Act
        val actual = fixedClockLock().withBatchOrBusy(WALLET) { BATCH_RESULT }

        // Assert
        assertThat(actual).isEqualTo(PolymarketDepositWalletLock.Busy.left())
    }

    @Test
    fun `GIVEN a revocation that may still execute past the authorization WHEN a batch starts THEN it is busy`() =
        runTest {
            // Arrange
            val pending = createPendingAuthorization(deadline = deadlineIn(seconds = 0))
            pendingAuthorizations.store(
                WALLET,
                pending.copy(revocation = pending.revocation.copy(deadline = deadlineIn(seconds = 20))),
            )

            // Act
            val actual = fixedClockLock().withBatchOrBusy(WALLET) { BATCH_RESULT }

            // Assert
            assertThat(actual).isEqualTo(PolymarketDepositWalletLock.Busy.left())
        }

    private fun fixedClockLock() = PolymarketDepositWalletLock(pendingAuthorizations, Clock.fixed(NOW, ZoneOffset.UTC))

    private fun deadlineIn(seconds: Long) = NOW.plusSeconds(seconds).epochSecond.toString()

    private companion object {
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)
        val WALLET = UserWalletId("011")
        val OTHER_WALLET = UserWalletId("022")
        const val BATCH_RESULT = "submitted"
    }
}