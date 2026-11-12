package com.tangem.domain.polymarket

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketDepositWalletLockTest {

    private val lock = PolymarketDepositWalletLock()

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

    private companion object {
        val WALLET = UserWalletId("011")
        val OTHER_WALLET = UserWalletId("022")
        const val BATCH_RESULT = "submitted"
    }
}