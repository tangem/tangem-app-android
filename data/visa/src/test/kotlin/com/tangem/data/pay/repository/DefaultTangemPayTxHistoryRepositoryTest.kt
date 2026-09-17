package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchingContext
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListConfig
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.pagination.BatchAction
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.TangemPayTransactionResponse
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.spend.datasource.pay.store.TangemPayTxHistoryItemsStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultTangemPayTxHistoryRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestPerformer: TangemPayRequestPerformer = mockk()
    private val cacheRegistry: CacheRegistry = mockk()
    private val txHistoryItemsStore: TangemPayTxHistoryItemsStore = mockk(relaxed = true)

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestPerformer, cacheRegistry, txHistoryItemsStore)
        coEvery {
            requestPerformer.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
        coEvery {
            cacheRegistry.invokeOnExpire(key = any(), skipCache = any(), expireIn = any(), block = any())
        } coAnswers {
            arg<suspend () -> Unit>(3).invoke()
        }
    }

    @Test
    fun `GIVEN transaction id WHEN getTransaction THEN transactions endpoint is called`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getTransaction(any(), any()) } returns
            ApiResponse.Success(transactionResponse())

        // Act
        val actual = createRepository().getTransaction(userWalletId, transactionId = TX_ID)

        // Assert
        assertThat(actual.isRight()).isTrue()
        coVerify(exactly = 1) { tangemPayApi.getTransaction(AUTH_HEADER, TX_ID) }
    }

    @Test
    fun `GIVEN history WHEN reloaded THEN transactions endpoint is called`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getTangemPayTxHistory(any(), any(), any()) } returns
            ApiResponse.Success(historyResponse())

        // Act
        reloadHistory(createRepository())

        // Assert
        coVerify(exactly = 1) { tangemPayApi.getTangemPayTxHistory(AUTH_HEADER, cursor = null, limit = any()) }
    }

    private fun TestScope.reloadHistory(repository: DefaultTangemPayTxHistoryRepository) {
        val sourceScope = CoroutineScope(coroutineContext + Job())
        val actionsFlow =
            MutableSharedFlow<BatchAction<Int, TangemPayTxHistoryListConfig, Nothing>>(replay = 1)
        repository.getTxHistoryBatchFlow(
            userWalletId = userWalletId,
            batchSize = BATCH_SIZE,
            context = TangemPayTxHistoryListBatchingContext(
                actionsFlow = actionsFlow,
                coroutineScope = sourceScope,
            ),
        )
        actionsFlow.tryEmit(BatchAction.Reload(TangemPayTxHistoryListConfig(shouldRefresh = true)))
        advanceUntilIdle()
        sourceScope.cancel()
    }

    private fun createRepository(): DefaultTangemPayTxHistoryRepository {
        return DefaultTangemPayTxHistoryRepository(
            requestPerformer = requestPerformer,
            visaApi = tangemPayApi,
            cacheRegistry = cacheRegistry,
            txHistoryItemsStore = txHistoryItemsStore,
            dispatchers = TestingCoroutineDispatcherProvider(),
            moshi = Moshi.Builder().build(),
        )
    }

    private fun transactionResponse() = TangemPayTransactionResponse(
        result = TangemPayTxHistoryResponse.Transaction(id = TX_ID, type = "SPEND"),
    )

    private fun historyResponse() = TangemPayTxHistoryResponse(
        result = TangemPayTxHistoryResponse.Result(transactions = emptyList()),
    )

    private companion object {
        const val AUTH_HEADER = "Bearer token"
        const val TX_ID = "tx-id"
        const val BATCH_SIZE = 20
    }
}