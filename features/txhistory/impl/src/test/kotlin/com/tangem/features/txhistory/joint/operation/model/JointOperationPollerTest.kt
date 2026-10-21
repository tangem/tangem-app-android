package com.tangem.features.txhistory.joint.operation.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationFetchResult
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class JointOperationPollerTest {

    private val repository: JointOperationHistoryRepository = mockk()
    private val stored = MutableStateFlow<FinishedJointOperation?>(null)

    private val poller = JointOperationPoller(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
        stored.value = null
        every { repository.observeOperation(USER_WALLET_ID, OPERATION_ID) } returns stored
    }

    @Test
    fun `GIVEN an operation in flight WHEN the interval passes THEN it is fetched again`() = runTest {
        // Arrange
        coEvery { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) } returns
            JointOperationFetchResult.Updated.right() andThen JointOperationFetchResult.NotModified.right()
        val polling = backgroundScope.async { poller.poll(USER_WALLET_ID, OPERATION_ID) }

        // Act
        runCurrent()
        advanceTimeBy(JointOperationPoller.POLL_INTERVAL + 1.milliseconds)

        // Assert
        assertThat(polling.isActive).isTrue()
        coVerify(exactly = 2) { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) }
    }

    @Test
    fun `GIVEN a refresh request WHEN the poller waits THEN it fetches at once`() = runTest {
        // Arrange
        coEvery { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) } returns
            JointOperationFetchResult.NotModified.right()
        backgroundScope.async { poller.poll(USER_WALLET_ID, OPERATION_ID) }
        runCurrent()

        // Act
        poller.refresh()
        runCurrent()

        // Assert
        coVerify(exactly = 2) { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) }
    }

    @Test
    fun `GIVEN the fetch stores a finished operation WHEN polled THEN polling ends as finished`() = runTest {
        // Arrange
        coEvery { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) } answers {
            stored.value = finished
            JointOperationFetchResult.Updated.right()
        }

        // Act
        val end = poller.poll(USER_WALLET_ID, OPERATION_ID)

        // Assert
        assertThat(end).isEqualTo(JointOperationPoller.End.FINISHED)
        coVerify(exactly = 1) { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) }
    }

    @Test
    fun `GIVEN the operation is already finished WHEN polled THEN polling ends without a fetch`() = runTest {
        // Arrange
        stored.value = finished

        // Act
        val end = poller.poll(USER_WALLET_ID, OPERATION_ID)

        // Assert
        assertThat(end).isEqualTo(JointOperationPoller.End.FINISHED)
        coVerify(exactly = 0) { repository.fetchOperation(any(), any()) }
    }

    @Test
    fun `GIVEN the backend no longer knows the operation WHEN polled THEN polling ends as gone`() = runTest {
        // Arrange
        coEvery { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) } returns
            JointOperationFetchResult.Gone.right()

        // Act
        val end = poller.poll(USER_WALLET_ID, OPERATION_ID)

        // Assert
        assertThat(end).isEqualTo(JointOperationPoller.End.GONE)
    }

    @Test
    fun `GIVEN a failed fetch WHEN the interval passes THEN polling goes on`() = runTest {
        // Arrange
        coEvery { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) } returns
            IllegalStateException("offline").left() andThen JointOperationFetchResult.Gone.right()

        // Act
        val polling = backgroundScope.async { poller.poll(USER_WALLET_ID, OPERATION_ID) }
        advanceTimeBy(JointOperationPoller.POLL_INTERVAL + 1.milliseconds)

        // Assert
        assertThat(polling.await()).isEqualTo(JointOperationPoller.End.GONE)
        coVerify(exactly = 2) { repository.fetchOperation(USER_WALLET_ID, OPERATION_ID) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OPERATION_ID = "operation-1"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"

        val safeTx = SafeTx(
            to = "0xrecipient",
            value = BigInteger.ONE,
            data = "0x",
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger.ZERO,
            baseGas = BigInteger.ZERO,
            gasPrice = BigInteger.ZERO,
            gasToken = ZERO_ADDRESS,
            refundReceiver = ZERO_ADDRESS,
            nonce = BigInteger.ZERO,
        )

        val finished = FinishedJointOperation(
            operationId = OPERATION_ID,
            chainId = 1L,
            safeAddress = "0xsafe",
            safeTx = safeTx,
            status = FinishedJointOperation.Status.EXECUTED,
            initiator = null,
            signatures = emptyList(),
            createdAt = "2026-10-14T09:20:37.000Z",
            finishedAtMillis = 1_000L,
            txHash = null,
        )
    }
}