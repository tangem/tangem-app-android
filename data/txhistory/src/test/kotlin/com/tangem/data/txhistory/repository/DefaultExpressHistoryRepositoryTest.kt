package com.tangem.data.txhistory.repository

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.tangem.data.txhistory.repository.converter.toHistoryIndexEntities
import com.tangem.data.txhistory.repository.converter.toHistoryIndexEntity
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.grow.datasource.express.models.response.ExchangeHistoryDeltaResponse
import com.tangem.grow.datasource.express.models.response.ExchangeHistoryResponse
import com.tangem.grow.datasource.express.models.response.ExchangeItemResponse
import com.tangem.grow.datasource.express.models.response.ExpressPagination
import com.tangem.grow.datasource.express.models.response.ExpressPaginationDelta
import com.tangem.grow.datasource.onramp.OnrampApi
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampItemResponse
import com.tangem.datasource.local.converter.toEntity
import com.tangem.store.datasource.txhistory.db.TxHistoryDatabase
import com.tangem.store.datasource.txhistory.db.dao.ExpressHistoryDao
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.dao.HistoryIndexDao
import com.tangem.store.datasource.txhistory.db.entity.HistoryIndexEntity
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.TokenInfoRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.blockchain.common.address.Address
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.test.mock.MockAccounts
import com.tangem.domain.models.wallet.UserWallet
import arrow.core.Either
import com.tangem.grow.datasource.express.ExpressUtils
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultExpressHistoryRepositoryTest {

    private val exchangeApi: TangemExpressApi = mockk()
    private val onrampApi: OnrampApi = mockk()
    private val expressHistoryDao: ExpressHistoryDao = mockk(relaxUnitFun = true)
    private val historyIndexDao: HistoryIndexDao = mockk(relaxUnitFun = true)
    private val historySyncStateDao: HistorySyncStateDao = mockk(relaxUnitFun = true)
    private val tokenInfoRepository: TokenInfoRepository = mockk(relaxUnitFun = true)
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val expressUtils: ExpressUtils = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase = mockk()

    private val database: TxHistoryDatabase = mockk()

    private val repository = DefaultExpressHistoryRepository(
        exchangeApi = exchangeApi,
        onrampApi = onrampApi,
        expressHistoryDao = expressHistoryDao,
        historyIndexDao = historyIndexDao,
        historySyncStateDao = historySyncStateDao,
        tokenInfoRepository = tokenInfoRepository,
        walletManagersFacade = walletManagersFacade,
        expressUtils = expressUtils,
        database = database,
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            exchangeApi,
            onrampApi,
            expressHistoryDao,
            historyIndexDao,
            historySyncStateDao,
            tokenInfoRepository,
            walletManagersFacade,
            expressUtils,
            getUserWalletUseCase,
            singleAccountStatusListSupplier,
            manageCryptoCurrenciesUseCase,
        )
        // Run the withTransaction block inline so the DAO writes inside it actually happen and can be verified.
        mockkStatic("androidx.room.RoomDatabaseKt")
        val block = slot<suspend () -> Any?>()
        coEvery { database.withTransaction(capture(block)) } coAnswers { block.captured.invoke() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    // region exchange history

    @Test
    fun `GIVEN sync state WHEN fetchExchangeHistory THEN passes after cursor and persists items`() = runTest {
        // GIVEN
        val item = createExchangeItem()
        val response = ExchangeHistoryResponse(items = listOf(item), pagination = pagination())
        stubSyncState(HistorySyncStateEntity.Type.EXCHANGE, syncState(afterCursor = AFTER_CURSOR))
        coEvery {
            exchangeApi.getHistory(userWalletId = USER_WALLET_ID_VALUE, cursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        val result = repository.fetchExchangeHistory(userWalletId = USER_WALLET_ID)

        // THEN
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) {
            exchangeApi.getHistory(userWalletId = USER_WALLET_ID_VALUE, cursor = AFTER_CURSOR, limit = DEFAULT_LIMIT)
        }
        coVerify(exactly = 1) { expressHistoryDao.upsertExchanges(listOfNotNull(item.toEntity())) }
    }

    @Test
    fun `GIVEN no sync state WHEN fetchExchangeHistory THEN passes null cursor`() = runTest {
        // GIVEN
        val response = ExchangeHistoryResponse(items = emptyList(), pagination = pagination())
        stubSyncState(HistorySyncStateEntity.Type.EXCHANGE, state = null)
        coEvery {
            exchangeApi.getHistory(userWalletId = USER_WALLET_ID_VALUE, cursor = null, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        repository.fetchExchangeHistory(userWalletId = USER_WALLET_ID)

        // THEN
        coVerify(exactly = 1) {
            exchangeApi.getHistory(userWalletId = USER_WALLET_ID_VALUE, cursor = null, limit = DEFAULT_LIMIT)
        }
    }

    @Test
    fun `GIVEN api error WHEN fetchExchangeHistory THEN throws and does not persist`() = runTest {
        // GIVEN
        stubSyncState(HistorySyncStateEntity.Type.EXCHANGE, syncState(afterCursor = AFTER_CURSOR))
        val error = httpError()
        coEvery {
            exchangeApi.getHistory(userWalletId = USER_WALLET_ID_VALUE, cursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Error(error).cast()

        // WHEN
        val thrown = runCatching { repository.fetchExchangeHistory(userWalletId = USER_WALLET_ID) }.exceptionOrNull()

        // THEN
        assertThat(thrown).isEqualTo(error)
        coVerify(exactly = 0) { expressHistoryDao.upsertExchanges(any()) }
    }

    @Test
    fun `GIVEN sync state WHEN fetchExchangeHistoryDelta THEN passes delta cursor and persists items`() = runTest {
        // GIVEN
        val item = createExchangeItem()
        val response = ExchangeHistoryDeltaResponse(items = listOf(item), pagination = paginationDelta())
        stubSyncState(HistorySyncStateEntity.Type.EXCHANGE, syncState(deltaCursor = DELTA_CURSOR))
        coEvery {
            exchangeApi.getHistoryDelta(userWalletId = USER_WALLET_ID_VALUE, cursor = DELTA_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        val result = repository.fetchExchangeHistoryDelta(userWalletId = USER_WALLET_ID)

        // THEN
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) {
            exchangeApi.getHistoryDelta(userWalletId = USER_WALLET_ID_VALUE, cursor = DELTA_CURSOR, limit = DEFAULT_LIMIT)
        }
        coVerify(exactly = 1) { expressHistoryDao.upsertExchanges(listOfNotNull(item.toEntity())) }
    }

    // endregion

    // region onramp history

    @Test
    fun `GIVEN sync state WHEN fetchOnrampHistory THEN passes after cursor and persists items`() = runTest {
        // GIVEN
        val item = createOnrampItem()
        val response = OnrampHistoryResponse(items = listOf(item), pagination = pagination())
        stubSyncState(HistorySyncStateEntity.Type.ONRAMP, syncState(afterCursor = AFTER_CURSOR))
        coEvery {
            onrampApi.getHistory(userWalletId = USER_WALLET_ID_VALUE, afterCursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        val result = repository.fetchOnrampHistory(userWalletId = USER_WALLET_ID)

        // THEN
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) {
            onrampApi.getHistory(userWalletId = USER_WALLET_ID_VALUE, afterCursor = AFTER_CURSOR, limit = DEFAULT_LIMIT)
        }
        coVerify(exactly = 1) { expressHistoryDao.upsertOnramps(listOf(item.toEntity())) }
    }

    @Test
    fun `GIVEN sync state WHEN fetchOnrampHistoryDelta THEN passes delta cursor and persists items`() = runTest {
        // GIVEN
        val item = createOnrampItem()
        val response = OnrampHistoryDeltaResponse(items = listOf(item), pagination = paginationDelta())
        stubSyncState(HistorySyncStateEntity.Type.ONRAMP, syncState(deltaCursor = DELTA_CURSOR))
        coEvery {
            onrampApi.getHistoryDelta(userWalletId = USER_WALLET_ID_VALUE, cursor = DELTA_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        val result = repository.fetchOnrampHistoryDelta(userWalletId = USER_WALLET_ID)

        // THEN
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) {
            onrampApi.getHistoryDelta(userWalletId = USER_WALLET_ID_VALUE, cursor = DELTA_CURSOR, limit = DEFAULT_LIMIT)
        }
        coVerify(exactly = 1) { expressHistoryDao.upsertOnramps(listOf(item.toEntity())) }
    }

    @Test
    fun `GIVEN api error WHEN fetchOnrampHistory THEN throws and does not persist`() = runTest {
        // GIVEN
        stubSyncState(HistorySyncStateEntity.Type.ONRAMP, syncState(afterCursor = AFTER_CURSOR))
        val error = httpError()
        coEvery {
            onrampApi.getHistory(userWalletId = USER_WALLET_ID_VALUE, afterCursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Error(error).cast()

        // WHEN
        val thrown = runCatching { repository.fetchOnrampHistory(userWalletId = USER_WALLET_ID) }.exceptionOrNull()

        // THEN
        assertThat(thrown).isEqualTo(error)
        coVerify(exactly = 0) { expressHistoryDao.upsertOnramps(any()) }
    }

    // endregion

    // region store

    @Test
    fun `GIVEN exchanges WHEN storeExchanges THEN persists entities and fetches missing info for both legs`() = runTest {
        // GIVEN
        val item = createExchangeItem()

        // WHEN
        repository.storeExchanges(items = listOf(item))

        // THEN
        coVerify(exactly = 1) { expressHistoryDao.upsertExchanges(listOfNotNull(item.toEntity())) }
        coVerify(exactly = 1) { historyIndexDao.upsert(item.toEntity()!!.toHistoryIndexEntities()) }
        coVerify(exactly = 1) {
            tokenInfoRepository.fetchMissing(
                setOf(
                    ExpressAsset.ID(networkId = "ethereum", contractAddress = "0xfromContract"),
                    ExpressAsset.ID(networkId = "bitcoin", contractAddress = "0xtoContract"),
                ),
            )
        }
    }

    @Test
    fun `GIVEN onramps WHEN storeOnramps THEN persists entities and fetches missing info for to-leg`() = runTest {
        // GIVEN
        val item = createOnrampItem()

        // WHEN
        repository.storeOnramps(items = listOf(item))

        // THEN
        coVerify(exactly = 1) { expressHistoryDao.upsertOnramps(listOf(item.toEntity())) }
        coVerify(exactly = 1) { historyIndexDao.upsert(listOf(item.toEntity().toHistoryIndexEntity())) }
        coVerify(exactly = 1) {
            tokenInfoRepository.fetchMissing(setOf(ExpressAsset.ID(networkId = "bitcoin", contractAddress = "0xtoContract")))
        }
    }

    @Test
    fun `GIVEN empty items WHEN store THEN does nothing`() = runTest {
        // WHEN
        repository.storeExchanges(items = emptyList())
        repository.storeOnramps(items = emptyList())

        // THEN
        coVerify(exactly = 0) { expressHistoryDao.upsertExchanges(any()) }
        coVerify(exactly = 0) { expressHistoryDao.upsertOnramps(any()) }
        coVerify(exactly = 0) { historyIndexDao.upsert(any<List<HistoryIndexEntity>>()) }
        coVerify(exactly = 0) { tokenInfoRepository.fetchMissing(any()) }
    }

    @Test
    fun `GIVEN exchange with null fromAddress WHEN storeExchanges THEN it is skipped`() = runTest {
        // GIVEN
        val item = createExchangeItem().copy(fromAddress = null)

        // WHEN
        repository.storeExchanges(items = listOf(item))

        // THEN
        coVerify(exactly = 0) { expressHistoryDao.upsertExchanges(any()) }
        coVerify(exactly = 0) { historyIndexDao.upsert(any<List<HistoryIndexEntity>>()) }
        coVerify(exactly = 0) { tokenInfoRepository.fetchMissing(any()) }
    }

    // endregion


    // region observe active operation ids

    @Test
    fun `GIVEN active exchange and onramp WHEN observeActiveOperationIds THEN both are emitted with their type`() =
        runTest {
            // Arrange
            stubActiveStatuses(exchangeIds = listOf(ACTIVE_EXCHANGE_TX_ID), onrampIds = listOf(ACTIVE_ONRAMP_TX_ID))

            // Act
            val actual = repository.observeActiveOperationIds(USER_WALLET_ID, CURRENCY).first()

            // Assert
            assertThat(actual).containsExactly(
                ExpressOperationId(ACTIVE_EXCHANGE_TX_ID, ExpressOperationType.Swap),
                ExpressOperationId(ACTIVE_ONRAMP_TX_ID, ExpressOperationType.Onramp),
            )
        }

    @Test
    fun `GIVEN only the asset is queried WHEN observeActiveOperationIds THEN lookup uses its network and contract`() =
        runTest {
            // Arrange
            stubActiveStatuses(exchangeIds = emptyList(), onrampIds = emptyList())

            // Act
            repository.observeActiveOperationIds(USER_WALLET_ID, CURRENCY).first()

            // Assert
            val assetId = ExpressAsset.ID(CURRENCY)
            coVerify(exactly = 1) {
                expressHistoryDao.observeActiveExchangeIds(
                    addresses = listOf(ADDRESS),
                    network = assetId.networkId,
                    contract = assetId.contractAddress,
                    activeStatuses = any(),
                )
            }
            coVerify(exactly = 1) {
                expressHistoryDao.observeActiveOnrampIds(
                    addresses = listOf(ADDRESS),
                    network = assetId.networkId,
                    contract = assetId.contractAddress,
                    activeStatuses = any(),
                )
            }
        }

    @Test
    fun `GIVEN no address WHEN observeActiveOperationIds THEN emits empty and nothing is queried`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getAddresses(USER_WALLET_ID, CURRENCY.network) } returns emptySet()
        coEvery { walletManagersFacade.usedDynamicAddresses(USER_WALLET_ID, CURRENCY.network) } returns null

        // Act
        val actual = repository.observeActiveOperationIds(USER_WALLET_ID, CURRENCY).first()

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 0) { expressHistoryDao.observeActiveExchangeIds(any(), any(), any(), any()) }
        coVerify(exactly = 0) { expressHistoryDao.observeActiveOnrampIds(any(), any(), any(), any()) }
    }

    // endregion

    // region refresh active statuses

    @Test
    fun `GIVEN a swap id WHEN refreshActiveStatuses THEN the exchange status is re-read and persisted`() = runTest {
        // Arrange
        val exchange = createExchangeItem(txId = ACTIVE_EXCHANGE_TX_ID).copy(status = "confirming")
        stubActiveStatuses(exchangeIds = listOf(ACTIVE_EXCHANGE_TX_ID), onrampIds = emptyList())
        stubExchangeStatus(ACTIVE_EXCHANGE_TX_ID, ApiResponse.Success(exchange))

        // Act
        repository.refreshActiveStatuses(
            userWalletId = USER_WALLET_ID,
            activeId = ExpressOperationId(ACTIVE_EXCHANGE_TX_ID, ExpressOperationType.Swap),
        )

        // Assert
        coVerify(exactly = 1) {
            exchangeApi.getExchangeStatus(
                userWalletId = USER_WALLET_ID_VALUE,
                refCode = REF_CODE,
                txId = ACTIVE_EXCHANGE_TX_ID,
            )
        }
        coVerify(exactly = 0) { onrampApi.getStatus(any(), any(), any()) }
        coVerify(exactly = 1) { expressHistoryDao.upsertExchanges(listOfNotNull(exchange.toEntity())) }
    }

    @Test
    fun `GIVEN an onramp id WHEN refreshActiveStatuses THEN the onramp status is re-read and persisted`() = runTest {
        // Arrange
        val onramp = createOnrampItem(txId = ACTIVE_ONRAMP_TX_ID).copy(status = "sending")
        stubActiveStatuses(exchangeIds = emptyList(), onrampIds = listOf(ACTIVE_ONRAMP_TX_ID))
        stubOnrampStatus(ACTIVE_ONRAMP_TX_ID, ApiResponse.Success(onramp))

        // Act
        repository.refreshActiveStatuses(
            userWalletId = USER_WALLET_ID,
            activeId = ExpressOperationId(ACTIVE_ONRAMP_TX_ID, ExpressOperationType.Onramp),
        )

        // Assert
        coVerify(exactly = 1) {
            onrampApi.getStatus(
                userWalletId = USER_WALLET_ID_VALUE,
                refCode = REF_CODE,
                txId = ACTIVE_ONRAMP_TX_ID,
            )
        }
        coVerify(exactly = 0) { exchangeApi.getExchangeStatus(any(), any(), any()) }
        coVerify(exactly = 1) { expressHistoryDao.upsertOnramps(listOf(onramp.toEntity())) }
    }

    @Test
    fun `GIVEN the wallet is gone WHEN refreshActiveStatuses THEN no request is sent`() = runTest {
        // Arrange
        every { getUserWalletUseCase(USER_WALLET_ID) } returns Either.Left(mockk())

        // Act
        repository.refreshActiveStatuses(
            userWalletId = USER_WALLET_ID,
            activeId = ExpressOperationId(ACTIVE_EXCHANGE_TX_ID, ExpressOperationType.Swap),
        )

        // Assert
        coVerify(exactly = 0) { exchangeApi.getExchangeStatus(any(), any(), any()) }
        coVerify(exactly = 0) { onrampApi.getStatus(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a refunded dex-bridge swap WHEN refreshActiveStatuses THEN the refund token is added`() = runTest {
        // Arrange
        val exchange = createExchangeItem(txId = ACTIVE_EXCHANGE_TX_ID)
            .copy(status = "refunded", refundNetwork = REFUND_NETWORK, refundContractAddress = REFUND_CONTRACT)
        stubActiveStatuses(exchangeIds = listOf(ACTIVE_EXCHANGE_TX_ID), onrampIds = emptyList())
        stubExchangeStatus(ACTIVE_EXCHANGE_TX_ID, ApiResponse.Success(exchange))
        stubProvider(ExpressProviderType.DEX_BRIDGE)
        stubPayInAccount()
        coEvery { manageCryptoCurrenciesUseCase.add(any(), any(), any(), any()) } returns Either.Right(mockk())

        // Act
        repository.refreshActiveStatuses(
            userWalletId = USER_WALLET_ID,
            activeId = ExpressOperationId(ACTIVE_EXCHANGE_TX_ID, ExpressOperationType.Swap),
        )

        // Assert
        coVerify(exactly = 1) {
            manageCryptoCurrenciesUseCase.add(
                accountId = ACCOUNT.accountId,
                networkId = REFUND_NETWORK,
                contractAddress = REFUND_CONTRACT,
            )
        }
    }

    @Test
    fun `GIVEN a refunded cex swap WHEN refreshActiveStatuses THEN no token is added`() = runTest {
        // Arrange
        val exchange = createExchangeItem(txId = ACTIVE_EXCHANGE_TX_ID)
            .copy(status = "refunded", refundNetwork = REFUND_NETWORK, refundContractAddress = REFUND_CONTRACT)
        stubActiveStatuses(exchangeIds = listOf(ACTIVE_EXCHANGE_TX_ID), onrampIds = emptyList())
        stubExchangeStatus(ACTIVE_EXCHANGE_TX_ID, ApiResponse.Success(exchange))
        stubProvider(ExpressProviderType.CEX)

        // Act
        repository.refreshActiveStatuses(
            userWalletId = USER_WALLET_ID,
            activeId = ExpressOperationId(ACTIVE_EXCHANGE_TX_ID, ExpressOperationType.Swap),
        )

        // Assert
        coVerify(exactly = 0) { manageCryptoCurrenciesUseCase.add(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a dex-bridge swap without refund WHEN refreshActiveStatuses THEN no token is added`() = runTest {
        // Arrange
        val exchange = createExchangeItem(txId = ACTIVE_EXCHANGE_TX_ID).copy(status = "confirming")
        stubActiveStatuses(exchangeIds = listOf(ACTIVE_EXCHANGE_TX_ID), onrampIds = emptyList())
        stubExchangeStatus(ACTIVE_EXCHANGE_TX_ID, ApiResponse.Success(exchange))

        // Act
        repository.refreshActiveStatuses(
            userWalletId = USER_WALLET_ID,
            activeId = ExpressOperationId(ACTIVE_EXCHANGE_TX_ID, ExpressOperationType.Swap),
        )

        // Assert
        coVerify(exactly = 0) { manageCryptoCurrenciesUseCase.add(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a stored pending swap WHEN the delta turns it refunded THEN the refund token is added`() = runTest {
        // Arrange
        val refunded = refundedExchangeItem()
        stubDelta(refunded)
        stubStoredExchange(refunded.copy(status = "confirming"))
        stubProvider(ExpressProviderType.DEX_BRIDGE)
        stubPayInAccount()
        coEvery { manageCryptoCurrenciesUseCase.add(any(), any(), any(), any()) } returns Either.Right(mockk())

        // Act
        repository.fetchExchangeHistoryDelta(userWalletId = USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) {
            manageCryptoCurrenciesUseCase.add(
                accountId = ACCOUNT.accountId,
                networkId = REFUND_NETWORK,
                contractAddress = REFUND_CONTRACT,
            )
        }
    }

    @Test
    fun `GIVEN no stored swap WHEN the delta brings a refunded one THEN no token is added`() = runTest {
        // Arrange — the initial walk and a reinstall look like this; past refunds must not flood the portfolio.
        val refunded = refundedExchangeItem()
        stubDelta(refunded)
        stubStoredExchange(stored = null)

        // Act
        repository.fetchExchangeHistoryDelta(userWalletId = USER_WALLET_ID)

        // Assert
        coVerify(exactly = 0) { manageCryptoCurrenciesUseCase.add(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN an already refunded stored swap WHEN the delta repeats it THEN no token is added`() = runTest {
        // Arrange
        val refunded = refundedExchangeItem()
        stubDelta(refunded)
        stubStoredExchange(refunded)

        // Act
        repository.fetchExchangeHistoryDelta(userWalletId = USER_WALLET_ID)

        // Assert
        coVerify(exactly = 0) { manageCryptoCurrenciesUseCase.add(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the status request fails WHEN refreshActiveStatuses THEN nothing is persisted`() = runTest {
        // Arrange
        stubActiveStatuses(exchangeIds = listOf(ACTIVE_EXCHANGE_TX_ID), onrampIds = emptyList())
        stubExchangeStatus(ACTIVE_EXCHANGE_TX_ID, ApiResponse.Error(httpError()).cast())

        // Act
        repository.refreshActiveStatuses(
            userWalletId = USER_WALLET_ID,
            activeId = ExpressOperationId(ACTIVE_EXCHANGE_TX_ID, ExpressOperationType.Swap),
        )

        // Assert
        coVerify(exactly = 0) { expressHistoryDao.upsertExchanges(any()) }
    }

    // endregion

    private fun stubSyncState(type: HistorySyncStateEntity.Type, state: HistorySyncStateEntity?) {
        coEvery {
            historySyncStateDao.observe(type = type.name, userWalletId = USER_WALLET_ID_VALUE)
        } returns flowOf(state)
    }

    private fun syncState(afterCursor: String? = null, deltaCursor: String? = null) = HistorySyncStateEntity(
        type = HistorySyncStateEntity.Type.EXCHANGE.name,
        userWalletId = USER_WALLET_ID_VALUE,
        isInitialCompleted = true,
        afterCursor = afterCursor,
        deltaCursor = deltaCursor,
    )

    private fun pagination() = ExpressPagination(endCursor = "end", startDeltaCursor = "delta", hasMore = false)

    private fun paginationDelta() = ExpressPaginationDelta(startCursor = "start", hasMore = false)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> ApiResponse.Error.cast(): ApiResponse<T> = this as ApiResponse<T>


    private fun stubActiveStatuses(exchangeIds: List<String>, onrampIds: List<String>) {
        val address: Address = mockk()
        every { address.value } returns ADDRESS
        coEvery { walletManagersFacade.getAddresses(USER_WALLET_ID, CURRENCY.network) } returns setOf(address)
        coEvery { walletManagersFacade.usedDynamicAddresses(USER_WALLET_ID, CURRENCY.network) } returns null
        every { expressHistoryDao.observeActiveExchangeIds(any(), any(), any(), any()) } returns flowOf(exchangeIds)
        every { expressHistoryDao.observeActiveOnrampIds(any(), any(), any(), any()) } returns flowOf(onrampIds)

        val userWallet: UserWallet = mockk()
        every { userWallet.walletId } returns USER_WALLET_ID
        every { getUserWalletUseCase(USER_WALLET_ID) } returns Either.Right(userWallet)
        coEvery { expressUtils.getRefCode(userWallet) } returns REF_CODE
    }

    private fun refundedExchangeItem() = createExchangeItem(txId = ACTIVE_EXCHANGE_TX_ID)
        .copy(status = "refunded", refundNetwork = REFUND_NETWORK, refundContractAddress = REFUND_CONTRACT)

    private fun stubDelta(vararg items: ExchangeItemResponse) {
        stubSyncState(HistorySyncStateEntity.Type.EXCHANGE, syncState(deltaCursor = DELTA_CURSOR))
        coEvery {
            exchangeApi.getHistoryDelta(userWalletId = USER_WALLET_ID_VALUE, cursor = DELTA_CURSOR, limit = any())
        } returns ApiResponse.Success(
            ExchangeHistoryDeltaResponse(items = items.toList(), pagination = paginationDelta()),
        )
    }

    private fun stubStoredExchange(stored: ExchangeItemResponse?) {
        every { expressHistoryDao.observeExchangeById(ACTIVE_EXCHANGE_TX_ID) } returns flowOf(stored?.toEntity())
    }

    private fun stubProvider(type: ExpressProviderType) {
        val provider = ExpressProviderEntity(
            id = PROVIDER_ID,
            name = "provider",
            type = type.name,
            imageLarge = "large",
            imageSmall = "small",
            termsOfUse = null,
            privacyPolicy = null,
            isRecommended = false,
            slippage = null,
            isExchangeOnlyWithinSingleAddress = false,
            isExtraIdSupported = false,
            isCustomSlippageSupported = false,
        )
        every { expressHistoryDao.getProvidersById() } returns flowOf(mapOf(PROVIDER_ID to provider))
    }

    /** One portfolio account holding [CURRENCY] at [ADDRESS] — the pay-in leg the refund token follows. */
    private fun stubPayInAccount() {
        val networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = SWAP_FROM_ADDRESS,
                type = NetworkAddress.Address.Type.Primary,
            ),
        )
        val currencyStatus: CryptoCurrencyStatus = mockk()
        every { currencyStatus.currency } returns CURRENCY
        val value: CryptoCurrencyStatus.Value = mockk()
        every { currencyStatus.value } returns value
        every { value.networkAddress } returns networkAddress

        val accountStatus: AccountStatus.Personal = mockk()
        every { accountStatus.accountId } returns ACCOUNT.accountId
        every { accountStatus.flattenCurrencies() } returns listOf(currencyStatus)

        val list: AccountStatusList = mockk()
        every { list.accountStatuses } returns listOf(accountStatus)
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(USER_WALLET_ID) } returns list
    }

    private fun stubExchangeStatus(txId: String, response: ApiResponse<ExchangeItemResponse>) {
        coEvery {
            exchangeApi.getExchangeStatus(userWalletId = USER_WALLET_ID_VALUE, refCode = REF_CODE, txId = txId)
        } returns response
    }

    private fun stubOnrampStatus(txId: String, response: ApiResponse<OnrampItemResponse>) {
        coEvery {
            onrampApi.getStatus(userWalletId = USER_WALLET_ID_VALUE, refCode = REF_CODE, txId = txId)
        } returns response
    }

    private fun httpError() = ApiResponseError.HttpException(
        code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
        message = "boom",
        errorBody = null,
    )

    private fun createExchangeItem(txId: String = "exchange-tx-1") = ExchangeItemResponse(
        txId = txId,
        providerId = "changelly",
        fromAddress = "0xfrom",
        payinAddress = "0xpayin",
        payinExtraId = null,
        payoutAddress = "0xpayout",
        refundAddress = null,
        refundExtraId = null,
        rateType = "float",
        status = "finished",
        externalTxId = null,
        externalTxUrl = null,
        payinHash = "payin-hash",
        payoutHash = "payout-hash",
        refundNetwork = null,
        refundContractAddress = null,
        createdAt = "2026-06-01T00:00:00Z",
        updatedAt = "2026-06-01T00:00:00Z",
        payTill = null,
        averageDuration = null,
        fromContractAddress = "0xfromContract",
        fromNetwork = "ethereum",
        fromDecimals = 18,
        fromAmount = "1.0",
        toContractAddress = "0xtoContract",
        toNetwork = "bitcoin",
        toDecimals = 8,
        toAmount = "1.0",
        toActualAmount = "0.99",
    )

    private fun createOnrampItem(txId: String = "onramp-tx-1") = OnrampItemResponse(
        txId = txId,
        providerId = "mercuryo",
        payoutAddress = "0xpayout",
        status = "finished",
        failReason = null,
        externalTxId = null,
        externalTxUrl = null,
        payoutHash = "payout-hash",
        createdAt = "2026-06-01T00:00:00Z",
        updatedAt = "2026-06-01T00:00:00Z",
        fromCurrencyCode = "USD",
        fromAmount = "100.0",
        fromPrecision = 2,
        toContractAddress = "0xtoContract",
        toNetwork = "bitcoin",
        toDecimals = 8,
        toAmount = "0.001",
        toActualAmount = "0.99",
        paymentMethod = "card",
        countryCode = "US",
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("0123456789abcdef")
        val USER_WALLET_ID_VALUE = USER_WALLET_ID.stringValue
        const val AFTER_CURSOR = "after-cursor"
        const val DELTA_CURSOR = "delta-cursor"
        const val DEFAULT_LIMIT = 100
        const val ADDRESS = "0xpayout"
        const val REF_CODE = "ref-code"
        const val ACTIVE_EXCHANGE_TX_ID = "active-exchange-tx"
        const val ACTIVE_ONRAMP_TX_ID = "active-onramp-tx"
        const val PROVIDER_ID = "changelly"
        const val SWAP_FROM_ADDRESS = "0xfrom"
        const val REFUND_NETWORK = "polygon"
        const val REFUND_CONTRACT = "0xrefund"
        val ACCOUNT = MockAccounts.createAccounts(count = 1).first()
        val CURRENCY = MockCryptoCurrencyFactory().ethereum
    }
}