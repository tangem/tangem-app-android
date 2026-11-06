package com.tangem.data.txhistory.fetcher

import com.tangem.data.txhistory.repository.JointOperationHistorySyncer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryDeltaResponse
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryResponse
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultJointOperationTxHistoryFetcherTest {

    private val syncer: JointOperationHistorySyncer = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(syncer)
    }

    @Test
    fun `GIVEN no sync state WHEN triggered THEN the delta runs after the first page without waiting for the walk`() =
        runTest {
            // Arrange
            coEvery { syncer.syncState(WALLET_ID) } returnsMany listOf(null, incompleteSyncState())
            coEvery { syncer.fetchHistory(WALLET_ID, any()) } returns historyResponse(hasMore = true) coAndThen
                { slowLastHistoryPage() }
            coEvery { syncer.fetchHistoryDelta(WALLET_ID, any()) } returns deltaResponse(hasMore = false)
            val fetcher = createFetcher(createUtils())

            // Act
            fetcher.invoke(TxHistoryFetchTrigger.WalletSelected(walletId = WALLET_ID))
            runCurrent()

            // Assert
            coVerifyOrder {
                syncer.fetchHistory(WALLET_ID, any())
                syncer.fetchHistoryDelta(WALLET_ID, any())
            }
            coVerify(exactly = 2) { syncer.fetchHistory(WALLET_ID, any()) }
            advanceUntilIdle()
        }

    @Test
    fun `GIVEN walk in progress WHEN triggered again THEN the delta runs again and the walk is not restarted`() =
        runTest {
            // Arrange
            coEvery { syncer.syncState(WALLET_ID) } returns incompleteSyncState()
            coEvery { syncer.fetchHistory(WALLET_ID, any()) } coAnswers { slowLastHistoryPage() }
            coEvery { syncer.fetchHistoryDelta(WALLET_ID, any()) } returns deltaResponse(hasMore = false)
            val fetcher = createFetcher(createUtils())

            // Act
            fetcher.invoke(TxHistoryFetchTrigger.WalletSelected(walletId = WALLET_ID))
            runCurrent()
            fetcher.invoke(TxHistoryFetchTrigger.JointHistoryPoll(walletId = WALLET_ID))
            runCurrent()

            // Assert
            coVerify(exactly = 2) { syncer.fetchHistoryDelta(WALLET_ID, any()) }
            coVerify(exactly = 1) { syncer.fetchHistory(WALLET_ID, any()) }
            advanceUntilIdle()
        }

    @Test
    fun `GIVEN the first history page fails WHEN triggered THEN the delta does not run`() = runTest {
        // Arrange
        coEvery { syncer.syncState(WALLET_ID) } returns null
        coEvery { syncer.fetchHistory(WALLET_ID, any()) } throws IllegalStateException("offline")
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.WalletSelected(walletId = WALLET_ID))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { syncer.fetchHistoryDelta(any(), any()) }
    }

    @Test
    fun `GIVEN completed initial walk WHEN polled THEN only the delta runs until it has no more`() = runTest {
        // Arrange
        coEvery { syncer.syncState(WALLET_ID) } returns completedSyncState()
        coEvery { syncer.fetchHistoryDelta(WALLET_ID, any()) } returnsMany listOf(
            deltaResponse(hasMore = true),
            deltaResponse(hasMore = false),
        )
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.JointHistoryPoll(walletId = WALLET_ID))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { syncer.fetchHistory(any(), any()) }
        coVerify(exactly = 2) { syncer.fetchHistoryDelta(WALLET_ID, any()) }
    }

    @Test
    fun `GIVEN delta drops the sync state WHEN polled THEN the delta is not retried`() = runTest {
        // Arrange
        coEvery { syncer.syncState(WALLET_ID) } returns completedSyncState()
        coEvery { syncer.fetchHistoryDelta(WALLET_ID, any()) } returns null
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.JointHistoryPoll(walletId = WALLET_ID))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { syncer.fetchHistoryDelta(WALLET_ID, any()) }
    }

    private fun TestScope.createUtils(): DefaultTxHistoryFetcherUtils = DefaultTxHistoryFetcherUtils(
        appScope = TestAppCoroutineScope(testScope = this),
        analyticsEventHandler = mockk(relaxed = true),
        analyticsExceptionHandler = mockk(relaxed = true),
    )

    private fun createFetcher(utils: DefaultTxHistoryFetcherUtils) = DefaultJointOperationTxHistoryFetcher(
        walletId = WALLET_ID,
        utils = utils,
        syncer = syncer,
    )

    private fun historyResponse(hasMore: Boolean) = JointOperationHistoryResponse(
        items = emptyList(),
        pagination = JointOperationHistoryResponse.Pagination(
            endCursor = "end",
            startDeltaCursor = null,
            hasMore = hasMore,
        ),
    )

    private suspend fun slowLastHistoryPage(): JointOperationHistoryResponse {
        delay(SLOW_PAGE_MILLIS)
        return historyResponse(hasMore = false)
    }

    private fun deltaResponse(hasMore: Boolean) = JointOperationHistoryDeltaResponse(
        items = emptyList(),
        pagination = JointOperationHistoryDeltaResponse.Pagination(startCursor = "start", hasMore = hasMore),
    )

    private fun completedSyncState() = syncState(isInitialCompleted = true)

    private fun incompleteSyncState() = syncState(isInitialCompleted = false)

    private fun syncState(isInitialCompleted: Boolean) = HistorySyncStateEntity(
        type = HistorySyncStateEntity.Type.JOINT_OPERATION.name,
        userWalletId = WALLET_ID.stringValue,
        isInitialCompleted = isInitialCompleted,
        afterCursor = "end",
        deltaCursor = "delta",
    )

    private companion object {
        val WALLET_ID: UserWalletId = MockAccounts.userWalletId
        const val SLOW_PAGE_MILLIS = 10_000L
    }
}