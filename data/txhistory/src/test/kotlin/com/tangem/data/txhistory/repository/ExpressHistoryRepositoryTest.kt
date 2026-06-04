package com.tangem.data.txhistory.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.data.txhistory.repository.converter.toEntity
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExchangeHistoryDeltaResponse
import com.tangem.datasource.api.express.models.response.ExchangeHistoryResponse
import com.tangem.datasource.api.express.models.response.ExchangeItemResponse
import com.tangem.datasource.api.express.models.response.ExpressPagination
import com.tangem.datasource.api.express.models.response.ExpressPaginationDelta
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryResponse
import com.tangem.datasource.api.onramp.models.response.OnrampItemResponse
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressHistoryRepositoryTest {

    private val exchangeApi: TangemExpressApi = mockk()
    private val onrampApi: OnrampApi = mockk()
    private val expressHistoryDao: ExpressHistoryDao = mockk(relaxUnitFun = true)
    private val expressSyncStateDao: ExpressSyncStateDao = mockk()

    private val repository = ExpressHistoryRepository(
        exchangeApi = exchangeApi,
        onrampApi = onrampApi,
        expressHistoryDao = expressHistoryDao,
        expressSyncStateDao = expressSyncStateDao,
    )

    @BeforeEach
    fun setup() {
        clearMocks(exchangeApi, onrampApi, expressHistoryDao, expressSyncStateDao)
    }

    // region exchange history

    @Test
    fun `GIVEN sync state WHEN fetchExchangeHistory THEN passes after cursor and persists items`() = runTest {
        // GIVEN
        val item = createExchangeItem()
        val response = ExchangeHistoryResponse(items = listOf(item), pagination = pagination())
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, ADDRESS, syncState(afterCursor = AFTER_CURSOR))
        coEvery {
            exchangeApi.getHistory(fromAddress = ADDRESS, cursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        val result = repository.fetchExchangeHistory(fromAddress = ADDRESS)

        // THEN
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) {
            exchangeApi.getHistory(fromAddress = ADDRESS, cursor = AFTER_CURSOR, limit = DEFAULT_LIMIT)
        }
        coVerify(exactly = 1) { expressHistoryDao.upsertExchanges(listOf(item.toEntity(ADDRESS))) }
    }

    @Test
    fun `GIVEN no sync state WHEN fetchExchangeHistory THEN passes null cursor`() = runTest {
        // GIVEN
        val response = ExchangeHistoryResponse(items = emptyList(), pagination = pagination())
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, ADDRESS, state = null)
        coEvery {
            exchangeApi.getHistory(fromAddress = ADDRESS, cursor = null, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        repository.fetchExchangeHistory(fromAddress = ADDRESS)

        // THEN
        coVerify(exactly = 1) {
            exchangeApi.getHistory(fromAddress = ADDRESS, cursor = null, limit = DEFAULT_LIMIT)
        }
    }

    @Test
    fun `GIVEN custom limit WHEN fetchExchangeHistory THEN forwards limit to api`() = runTest {
        // GIVEN
        val response = ExchangeHistoryResponse(items = emptyList(), pagination = pagination())
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, ADDRESS, syncState(afterCursor = AFTER_CURSOR))
        coEvery {
            exchangeApi.getHistory(fromAddress = ADDRESS, cursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        repository.fetchExchangeHistory(fromAddress = ADDRESS, limit = 25)

        // THEN
        coVerify(exactly = 1) {
            exchangeApi.getHistory(fromAddress = ADDRESS, cursor = AFTER_CURSOR, limit = 25)
        }
    }

    @Test
    fun `GIVEN api error WHEN fetchExchangeHistory THEN throws and does not persist`() = runTest {
        // GIVEN
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, ADDRESS, syncState(afterCursor = AFTER_CURSOR))
        val error = httpError()
        coEvery {
            exchangeApi.getHistory(fromAddress = ADDRESS, cursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Error(error).cast()

        // WHEN
        val thrown = runCatching { repository.fetchExchangeHistory(fromAddress = ADDRESS) }.exceptionOrNull()

        // THEN
        assertThat(thrown).isEqualTo(error)
        coVerify(exactly = 0) { expressHistoryDao.upsertExchanges(any()) }
    }

    @Test
    fun `GIVEN sync state WHEN fetchExchangeHistoryDelta THEN passes delta cursor and persists items`() = runTest {
        // GIVEN
        val item = createExchangeItem()
        val response = ExchangeHistoryDeltaResponse(items = listOf(item), pagination = paginationDelta())
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, ADDRESS, syncState(deltaCursor = DELTA_CURSOR))
        coEvery {
            exchangeApi.getHistoryDelta(fromAddress = ADDRESS, cursor = DELTA_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        val result = repository.fetchExchangeHistoryDelta(fromAddress = ADDRESS)

        // THEN
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) {
            exchangeApi.getHistoryDelta(fromAddress = ADDRESS, cursor = DELTA_CURSOR, limit = DEFAULT_LIMIT)
        }
        coVerify(exactly = 1) { expressHistoryDao.upsertExchanges(listOf(item.toEntity(ADDRESS))) }
    }

    // endregion

    // region onramp history

    @Test
    fun `GIVEN sync state WHEN fetchOnrampHistory THEN passes after cursor and persists items`() = runTest {
        // GIVEN
        val item = createOnrampItem()
        val response = OnrampHistoryResponse(items = listOf(item), pagination = pagination())
        stubSyncState(ExpressSyncStateEntity.Type.ONRAMP, ADDRESS, syncState(afterCursor = AFTER_CURSOR))
        coEvery {
            onrampApi.getHistory(payoutAddress = ADDRESS, afterCursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        val result = repository.fetchOnrampHistory(payoutAddress = ADDRESS)

        // THEN
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) {
            onrampApi.getHistory(payoutAddress = ADDRESS, afterCursor = AFTER_CURSOR, limit = DEFAULT_LIMIT)
        }
        coVerify(exactly = 1) { expressHistoryDao.upsertOnramps(listOf(item.toEntity(ADDRESS))) }
    }

    @Test
    fun `GIVEN no sync state WHEN fetchOnrampHistory THEN passes null cursor`() = runTest {
        // GIVEN
        val response = OnrampHistoryResponse(items = emptyList(), pagination = pagination())
        stubSyncState(ExpressSyncStateEntity.Type.ONRAMP, ADDRESS, state = null)
        coEvery {
            onrampApi.getHistory(payoutAddress = ADDRESS, afterCursor = null, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        repository.fetchOnrampHistory(payoutAddress = ADDRESS)

        // THEN
        coVerify(exactly = 1) {
            onrampApi.getHistory(payoutAddress = ADDRESS, afterCursor = null, limit = DEFAULT_LIMIT)
        }
    }

    @Test
    fun `GIVEN sync state WHEN fetchOnrampHistoryDelta THEN passes delta cursor and persists items`() = runTest {
        // GIVEN
        val item = createOnrampItem()
        val response = OnrampHistoryDeltaResponse(items = listOf(item), pagination = paginationDelta())
        stubSyncState(ExpressSyncStateEntity.Type.ONRAMP, ADDRESS, syncState(deltaCursor = DELTA_CURSOR))
        coEvery {
            onrampApi.getHistoryDelta(payoutAddress = ADDRESS, cursor = DELTA_CURSOR, limit = any())
        } returns ApiResponse.Success(response)

        // WHEN
        val result = repository.fetchOnrampHistoryDelta(payoutAddress = ADDRESS)

        // THEN
        assertThat(result).isEqualTo(response)
        coVerify(exactly = 1) {
            onrampApi.getHistoryDelta(payoutAddress = ADDRESS, cursor = DELTA_CURSOR, limit = DEFAULT_LIMIT)
        }
        coVerify(exactly = 1) { expressHistoryDao.upsertOnramps(listOf(item.toEntity(ADDRESS))) }
    }

    @Test
    fun `GIVEN api error WHEN fetchOnrampHistory THEN throws and does not persist`() = runTest {
        // GIVEN
        stubSyncState(ExpressSyncStateEntity.Type.ONRAMP, ADDRESS, syncState(afterCursor = AFTER_CURSOR))
        val error = httpError()
        coEvery {
            onrampApi.getHistory(payoutAddress = ADDRESS, afterCursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Error(error).cast()

        // WHEN
        val thrown = runCatching { repository.fetchOnrampHistory(payoutAddress = ADDRESS) }.exceptionOrNull()

        // THEN
        assertThat(thrown).isEqualTo(error)
        coVerify(exactly = 0) { expressHistoryDao.upsertOnramps(any()) }
    }

    // endregion

    // region syncState

    @Test
    fun `GIVEN stored sync state WHEN syncState THEN returns first emitted value`() = runTest {
        // GIVEN
        val state = syncState(afterCursor = AFTER_CURSOR, deltaCursor = DELTA_CURSOR)
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, ADDRESS, state)

        // WHEN
        val result = repository.syncState(ExpressSyncStateEntity.Type.EXCHANGE, ADDRESS)

        // THEN
        assertThat(result).isEqualTo(state)
    }

    @Test
    fun `GIVEN multiple items WHEN fetchExchangeHistory THEN maps every item with owner address`() = runTest {
        // GIVEN
        val items = listOf(
            createExchangeItem(txId = "tx-1"),
            createExchangeItem(txId = "tx-2"),
        )
        val response = ExchangeHistoryResponse(items = items, pagination = pagination())
        stubSyncState(ExpressSyncStateEntity.Type.EXCHANGE, ADDRESS, syncState(afterCursor = AFTER_CURSOR))
        coEvery {
            exchangeApi.getHistory(fromAddress = ADDRESS, cursor = AFTER_CURSOR, limit = any())
        } returns ApiResponse.Success(response)
        val saved = slot<List<ExpressExchangeEntity>>()
        coEvery { expressHistoryDao.upsertExchanges(capture(saved)) } returns Unit

        // WHEN
        repository.fetchExchangeHistory(fromAddress = ADDRESS)

        // THEN
        assertThat(saved.captured).isEqualTo(items.map { it.toEntity(ADDRESS) })
        assertThat(saved.captured.map { it.ownerAddress }.toSet()).containsExactly(ADDRESS)
    }

    // endregion

    private fun stubSyncState(type: ExpressSyncStateEntity.Type, address: String, state: ExpressSyncStateEntity?) {
        coEvery { expressSyncStateDao.observe(type = type.name, address = address) } returns flowOf(state)
    }

    private fun syncState(afterCursor: String? = null, deltaCursor: String? = null) = ExpressSyncStateEntity(
        type = ExpressSyncStateEntity.Type.EXCHANGE.name,
        address = ADDRESS,
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
        status = ExchangeItemResponse.Status.FINISHED,
        externalTxId = null,
        externalTxStatus = null,
        externalTxUrl = null,
        payinHash = "payin-hash",
        payoutHash = "payout-hash",
        refundNetwork = null,
        refundContractAddress = null,
        createdAt = "2026-06-01T00:00:00Z",
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
        fromAddress = "0xfrom",
        payinAddress = "0xpayin",
        payinExtraId = null,
        payoutAddress = "0xpayout",
        refundAddress = null,
        refundExtraId = null,
        rateType = "fixed",
        status = OnrampItemResponse.Status.FINISHED,
        externalTxId = null,
        externalTxStatus = null,
        externalTxUrl = null,
        payinHash = "payin-hash",
        payoutHash = "payout-hash",
        refundNetwork = null,
        refundContractAddress = null,
        createdAt = "2026-06-01T00:00:00Z",
        payTill = null,
        averageDuration = null,
        fromContractAddress = "0xfromContract",
        fromNetwork = "usd",
        fromDecimals = 2,
        fromAmount = "100.0",
        toContractAddress = "0xtoContract",
        toNetwork = "bitcoin",
        toDecimals = 8,
        toAmount = "0.001",
        toActualAmount = "0.99",
    )

    private companion object {
        const val ADDRESS = "0xowner"
        const val AFTER_CURSOR = "after-cursor"
        const val DELTA_CURSOR = "delta-cursor"
        const val DEFAULT_LIMIT = 100
    }
}