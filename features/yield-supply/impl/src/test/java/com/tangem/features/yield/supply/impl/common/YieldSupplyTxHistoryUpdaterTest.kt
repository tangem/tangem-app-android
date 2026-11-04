package com.tangem.features.yield.supply.impl.common

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class YieldSupplyTxHistoryUpdaterTest {

    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter = mockk()
    private val yieldSupplyRepository: YieldSupplyRepository = mockk()
    private val cryptoCurrency: CryptoCurrency = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(txHistoryContentUpdateEmitter, yieldSupplyRepository)
        coEvery { txHistoryContentUpdateEmitter.triggerUpdate() } returns Unit
    }

    @Test
    fun `GIVEN sent yield transaction WHEN scheduleUpdates THEN history reloads at once`() = runTest {
        // Arrange
        coEvery { yieldSupplyRepository.getPendingTxHashes(USER_WALLET_ID, cryptoCurrency) } returns listOf(TX_ID)

        // Act
        createUpdater().scheduleUpdates(USER_WALLET_ID, cryptoCurrency, listOf(TX_ID))
        runCurrent()

        // Assert
        coVerify(exactly = 1) { txHistoryContentUpdateEmitter.triggerUpdate() }
    }

    @Test
    fun `GIVEN transaction still pending WHEN checked THEN history does not reload again`() = runTest {
        // Arrange
        coEvery { yieldSupplyRepository.getPendingTxHashes(USER_WALLET_ID, cryptoCurrency) } returns listOf(TX_ID)

        // Act
        createUpdater().scheduleUpdates(USER_WALLET_ID, cryptoCurrency, listOf(TX_ID))
        advanceTimeBy(30_000)

        // Assert
        coVerify(exactly = 1) { txHistoryContentUpdateEmitter.triggerUpdate() }
    }

    @Test
    fun `GIVEN transaction no longer pending WHEN checked THEN history reloads and once more after explorer lag`() =
        runTest {
            // Arrange
            coEvery { yieldSupplyRepository.getPendingTxHashes(USER_WALLET_ID, cryptoCurrency) } returnsMany
                listOf(listOf(TX_ID), emptyList())

            // Act
            createUpdater().scheduleUpdates(USER_WALLET_ID, cryptoCurrency, listOf(TX_ID))
            advanceTimeBy(6_001)

            // Assert
            coVerify(exactly = 2) { txHistoryContentUpdateEmitter.triggerUpdate() }

            // Act
            advanceTimeBy(10_001)

            // Assert
            coVerify(exactly = 3) { txHistoryContentUpdateEmitter.triggerUpdate() }
        }

    private fun TestScope.createUpdater() = YieldSupplyTxHistoryUpdater(
        txHistoryContentUpdateEmitter = txHistoryContentUpdateEmitter,
        yieldSupplyRepository = yieldSupplyRepository,
        appCoroutineScope = object : AppCoroutineScope {
            override val coroutineContext = backgroundScope.coroutineContext
        },
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val TX_ID = "0x47fc"
    }
}