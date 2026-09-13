package com.tangem.data.txhistory.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.txhistory.repository.factory.ExpressTransactionAssetFactory
import com.tangem.data.txhistory.repository.factory.ExpressTxByIdFactory
import com.tangem.datasource.local.txhistory.TxHistoryItemsStore
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.HistoryIndexDao
import com.tangem.datasource.local.txhistory.db.entity.HistoryIndexEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.ExpressTx
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
import kotlinx.coroutines.flow.toList
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
        expressTxByIdFactory = ExpressTxByIdFactory(expressTransactionAssetFactory),
        cacheRegistry = cacheRegistry,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, expressHistoryDao, historyIndexDao, expressTransactionAssetFactory)
        coEvery { walletManagersFacade.getDefaultAddress(any(), any()) } returns ADDRESS
        coEvery { walletManagersFacade.usedDynamicAddresses(any(), any()) } returns null
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WatchedAddresses {

        @Test
        fun `GIVEN dynamic addresses WHEN getExpressHistory THEN they are queried along with the default one`() =
            runTest {
                // Arrange
                val addresses = stubQueriesCapturingAddresses()
                coEvery { walletManagersFacade.usedDynamicAddresses(any(), any()) } returns
                    listOf(DYNAMIC_ADDRESS, ADDRESS)

                // Act
                repository.getExpressHistory(USER_WALLET_ID, currency, NO_LOWER_BOUND).first()

                // Assert — the default address is not repeated even though it is reported as used
                assertThat(addresses.captured).containsExactly(ADDRESS, DYNAMIC_ADDRESS).inOrder()
            }

        @Test
        fun `GIVEN dynamic addresses WHEN getIndexedExpressHistory THEN the index page spans all of them`() = runTest {
            // Arrange
            stubQueries()
            coEvery { walletManagersFacade.usedDynamicAddresses(any(), any()) } returns listOf(DYNAMIC_ADDRESS)
            val addresses = slot<List<String>>()
            every {
                historyIndexDao.observePage(capture(addresses), any<HistoryIndexDao.Cursor>(), any())
            } returns flowOf(emptyList())

            // Act
            repository.getIndexedExpressHistory(USER_WALLET_ID, currency, limit = 50).first()

            // Assert
            assertThat(addresses.captured).containsExactly(ADDRESS, DYNAMIC_ADDRESS).inOrder()
        }

        @Test
        fun `GIVEN no default address WHEN getExpressHistory THEN only the dynamic ones are queried`() = runTest {
            // Arrange
            val addresses = stubQueriesCapturingAddresses()
            coEvery { walletManagersFacade.getDefaultAddress(any(), any()) } returns null
            coEvery { walletManagersFacade.usedDynamicAddresses(any(), any()) } returns listOf(DYNAMIC_ADDRESS)

            // Act
            repository.getExpressHistory(USER_WALLET_ID, currency, NO_LOWER_BOUND).first()

            // Assert
            assertThat(addresses.captured).containsExactly(DYNAMIC_ADDRESS)
        }
    }

    /** Stubs the three express queries and captures the addresses they are filtered by. */
    private fun stubQueriesCapturingAddresses(): CapturingSlot<List<String>> {
        val addresses = slot<List<String>>()
        every {
            expressHistoryDao.observeOutgoingSwaps(capture(addresses), any(), any(), any(), any())
        } returns flowOf(emptyList())
        every {
            expressHistoryDao.observeIncomingSwaps(any(), any(), any(), any(), any())
        } returns flowOf(emptyList())
        every {
            expressHistoryDao.observeIncomingOnramps(any(), any(), any(), any(), any())
        } returns flowOf(emptyList())
        return addresses
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetExpressTxById {

        @Test
        fun `GIVEN a swap row from this address WHEN getExpressTxById THEN it converts to an outgoing swap`() =
            runTest {
                // Arrange
                every { expressHistoryDao.observeExchangeById(TX_ID) } returns
                    flowOf(createExchangeEntity(fromAddress = ADDRESS))
                every { expressHistoryDao.observeOnrampById(TX_ID) } returns flowOf(null)

                // Act
                val result = repository.getExpressTxById(USER_WALLET_ID, currency, TX_ID).first()

                // Assert
                assertThat(result).isInstanceOf(ExpressTx.Swap::class.java)
                assertThat((result as ExpressTx.Swap).isOutgoing).isTrue()
            }

        @Test
        fun `GIVEN a swap row from another address WHEN getExpressTxById THEN it converts to an incoming swap`() =
            runTest {
                // Arrange
                every { expressHistoryDao.observeExchangeById(TX_ID) } returns
                    flowOf(createExchangeEntity(fromAddress = "someone-else"))
                every { expressHistoryDao.observeOnrampById(TX_ID) } returns flowOf(null)

                // Act
                val result = repository.getExpressTxById(USER_WALLET_ID, currency, TX_ID).first()

                // Assert
                assertThat((result as ExpressTx.Swap).isOutgoing).isFalse()
            }

        @Test
        fun `GIVEN an onramp row WHEN getExpressTxById THEN it converts to an onramp`() = runTest {
            // Arrange
            every { expressHistoryDao.observeExchangeById(TX_ID) } returns flowOf(null)
            every { expressHistoryDao.observeOnrampById(TX_ID) } returns flowOf(createOnrampEntity())

            // Act
            val result = repository.getExpressTxById(USER_WALLET_ID, currency, TX_ID).first()

            // Assert
            assertThat(result).isInstanceOf(ExpressTx.Onramp::class.java)
        }

        @Test
        fun `GIVEN the id is in neither table WHEN getExpressTxById THEN nothing is emitted`() = runTest {
            // Arrange
            every { expressHistoryDao.observeExchangeById(TX_ID) } returns flowOf(null)
            every { expressHistoryDao.observeOnrampById(TX_ID) } returns flowOf(null)

            // Act
            val result = repository.getExpressTxById(USER_WALLET_ID, currency, TX_ID).toList()

            // Assert
            assertThat(result).isEmpty()
        }
    }

    private fun createExchangeEntity(fromAddress: String) = ExpressExchangeEntity(
        txId = TX_ID,
        providerId = "provider",
        fromAddress = fromAddress,
        payinAddress = "payin-addr",
        payinExtraId = null,
        payoutAddress = "payout-addr",
        refundAddress = null,
        refundExtraId = null,
        rateType = "float",
        status = "waiting",
        externalTxId = null,
        externalTxUrl = null,
        payinHash = "payin",
        payoutHash = "payout",
        refundNetwork = null,
        refundContractAddress = null,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        payTill = null,
        averageDuration = null,
        from = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = "",
            network = "ethereum",
            decimals = 18,
            amount = "1000000000000000000",
            actualAmount = null,
        ),
        to = ExpressExchangeEntity.AssetEmbedded(
            contractAddress = "0xtoken",
            network = "bitcoin",
            decimals = 8,
            amount = "100000",
            actualAmount = null,
        ),
    )

    private fun createOnrampEntity() = ExpressOnrampEntity(
        txId = TX_ID,
        providerId = "provider",
        payoutAddress = ADDRESS,
        status = "finished",
        failReason = null,
        externalTxId = null,
        externalTxUrl = null,
        payoutHash = "payout",
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        fromCurrencyCode = "USD",
        fromAmount = "10000",
        fromPrecision = 2,
        to = ExpressOnrampEntity.AssetEmbedded(
            contractAddress = "0xtoken",
            network = "ethereum",
            decimals = 18,
            amount = "500000000000000000",
            actualAmount = null,
        ),
        paymentMethod = "card",
        countryCode = "US",
    )

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
        const val DYNAMIC_ADDRESS = "addr-dynamic"
        const val NO_LOWER_BOUND = 0L
        const val EPOCH_ISO = "1970-01-01T00:00:00Z"
        const val TX_ID = "tx-1"
        const val CREATED_AT = "2026-06-01T00:00:00Z"
    }
}