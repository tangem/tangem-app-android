package com.tangem.data.txhistory.repository

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.tangem.data.txhistory.repository.converter.toHistoryIndexEntities
import com.tangem.data.txhistory.repository.converter.toHistoryIndexEntity
import com.tangem.data.txhistory.repository.factory.TokenInfoRepository
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
import com.tangem.datasource.local.txhistory.db.TxHistoryDatabase
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.dao.HistoryIndexDao
import com.tangem.datasource.local.txhistory.db.entity.HistoryIndexEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
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
    private val expressSyncStateDao: ExpressSyncStateDao = mockk(relaxUnitFun = true)
    private val tokenInfoRepository: TokenInfoRepository = mockk(relaxUnitFun = true)

    private val database: TxHistoryDatabase = mockk()

    private val repository = DefaultExpressHistoryRepository(
        exchangeApi = exchangeApi,
        onrampApi = onrampApi,
        expressHistoryDao = expressHistoryDao,
        historyIndexDao = historyIndexDao,
        expressSyncStateDao = expressSyncStateDao,
        tokenInfoRepository = tokenInfoRepository,
        database = database,
        appScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(exchangeApi, onrampApi, expressHistoryDao, historyIndexDao, expressSyncStateDao, tokenInfoRepository)
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
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, syncState(afterCursor = AFTER_CURSOR))
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
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, state = null)
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
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, syncState(afterCursor = AFTER_CURSOR))
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
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, syncState(deltaCursor = DELTA_CURSOR))
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
        stubSyncState(ExpressSyncStateEntity.Type.ONRAMP, syncState(afterCursor = AFTER_CURSOR))
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
        stubSyncState(ExpressSyncStateEntity.Type.ONRAMP, syncState(deltaCursor = DELTA_CURSOR))
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
        stubSyncState(ExpressSyncStateEntity.Type.ONRAMP, syncState(afterCursor = AFTER_CURSOR))
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

    private fun stubSyncState(type: ExpressSyncStateEntity.Type, state: ExpressSyncStateEntity?) {
        coEvery {
            expressSyncStateDao.observe(type = type.name, userWalletId = USER_WALLET_ID_VALUE)
        } returns flowOf(state)
    }

    private fun syncState(afterCursor: String? = null, deltaCursor: String? = null) = ExpressSyncStateEntity(
        type = ExpressSyncStateEntity.Type.EXCHANGE.name,
        userWalletId = USER_WALLET_ID_VALUE,
        isInitialCompleted = true,
        afterCursor = afterCursor,
        deltaCursor = deltaCursor,
    )

    private fun pagination() = ExpressPagination(endCursor = "end", startDeltaCursor = "delta", hasMore = false)

    private fun paginationDelta() = ExpressPaginationDelta(startCursor = "start", hasMore = false)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> ApiResponse.Error.cast(): ApiResponse<T> = this as ApiResponse<T>

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
    }
}