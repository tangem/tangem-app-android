package com.tangem.data.txhistory.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.txhistory.repository.factory.ExpressTransactionAssetFactory
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.HistoryIndexDao
import com.tangem.datasource.local.txhistory.db.entity.HistoryIndexEntity
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**

 * exact for the index-backed one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RefactoredTxHistoryRepositoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val txHistoryItemsStore: TxHistoryItemsStore = mockk()
    private val expressHistoryDao: ExpressHistoryDao = mockk()
    private val historyIndexDao: HistoryIndexDao = mockk()
    private val expressTransactionAssetFactory: ExpressTransactionAssetFactory = mockk()
    private val cacheRegistry: CacheRegistry = mockk()
    private val currency: CryptoCurrency = mockk(relaxed = true)

    private val repository = RefactoredTxHistoryRepository(
        walletManagersFacade = walletManagersFacade,
        txHistoryItemsStore = txHistoryItemsStore,
        expressHistoryDao = expressHistoryDao,
        historyIndexDao = historyIndexDao,
        expressTransactionAssetFactory = expressTransactionAssetFactory,
        cacheRegistry = cacheRegistry,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, expressHistoryDao, historyIndexDao, expressTransactionAssetFactory)
        coEvery { walletManagersFacade.getDefaultAddress(any(), any()) } returns ADDRESS
        coEvery { expressTransactionAssetFactory.create(any(), any(), any(), any()) } returns emptyMap()
        every { expressHistoryDao.getProvidersById() } returns flowOf(emptyMap())
        every { expressHistoryDao.getCurrenciesByCode() } returns flowOf(emptyMap())
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetExpressHistory {

        @Test
        fun `GIVEN an on-chain bound WHEN getExpressHistory THEN the query bound is shifted back by the skew`() =
            runTest {
                // Arrange
                val bound = stubQueries()

                // Act
                repository.getExpressHistory(USER_WALLET_ID, currency, iso("2026-08-12T12:00:00Z")).first()

                // Assert
                assertThat(bound.captured).isEqualTo("2026-08-11T12:00:00Z")
            }

        @Test
        fun `GIVEN no loaded on-chain page WHEN getExpressHistory THEN there is no lower bound`() = runTest {
            // Arrange
            val bound = stubQueries()

            // Act
            repository.getExpressHistory(USER_WALLET_ID, currency, NO_LOWER_BOUND).first()

            // Assert
            assertThat(bound.captured).isEqualTo(EPOCH_ISO)
        }

        @Test
        fun `GIVEN a bound younger than the skew WHEN getExpressHistory THEN it is clamped to the epoch`() = runTest {
            // Arrange
            val bound = stubQueries()

            // Act
            repository.getExpressHistory(USER_WALLET_ID, currency, iso("1970-01-01T01:00:00Z")).first()

            // Assert
            assertThat(bound.captured).isEqualTo(EPOCH_ISO)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetIndexedExpressHistory {

        @Test
        fun `GIVEN an index page WHEN getIndexedExpressHistory THEN its sort time bounds the query unshifted`() =
            runTest {
                // Arrange
                val bound = stubQueries()
                every {
                    historyIndexDao.observePage(any(), any<HistoryIndexDao.Cursor>(), any())
                } returns flowOf(listOf(createIndexEntity(sortTimeMillis = iso("2026-08-12T12:00:00Z"))))

                // Act
                repository.getIndexedExpressHistory(USER_WALLET_ID, currency, limit = 50).first()

                // Assert
                assertThat(bound.captured).isEqualTo("2026-08-12T12:00:00Z")
            }
    }

    /** Stubs the three express queries and captures the `created_at` bound they are filtered by. */
    private fun stubQueries(): CapturingSlot<String> {
        val bound = slot<String>()
        every {
            expressHistoryDao.observeOutgoingSwaps(any(), any(), any(), capture(bound), any())
        } returns flowOf(emptyList())
        every {
            expressHistoryDao.observeIncomingSwaps(any(), any(), any(), capture(bound), any())
        } returns flowOf(emptyList())
        every {
            expressHistoryDao.observeIncomingOnramps(any(), any(), any(), capture(bound), any())
        } returns flowOf(emptyList())
        return bound
    }

    private fun createIndexEntity(sortTimeMillis: Long) = HistoryIndexEntity(
        type = HistoryIndexEntity.Type.EXCHANGE.value,
        entityId = "tx-1",
        address = ADDRESS,
        sortTimeMillis = sortTimeMillis,
    )

    private fun iso(value: String): Long = DateTime.parse(value).millis

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "01")
        const val ADDRESS = "addr"
        const val NO_LOWER_BOUND = 0L
        const val EPOCH_ISO = "1970-01-01T00:00:00Z"
    }
}