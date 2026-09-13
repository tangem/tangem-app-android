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
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.pagination.BatchAction
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.TangemPayTransactionResponse
import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.spend.datasource.pay.store.TangemPayTxHistoryItemsStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private val featureToggles: TangemPayFeatureToggles = mockk()

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestPerformer, cacheRegistry, txHistoryItemsStore, featureToggles)
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
    fun `GIVEN cashback toggle disabled WHEN getTransaction THEN legacy endpoint is called`() = runTest {
        // Arrange
        every { featureToggles.isCashbackEnabled } returns false
        coEvery { tangemPayApi.getTransactionLegacy(any(), any()) } returns
            ApiResponse.Success(transactionResponse())

        // Act
        val actual = createRepository().getTransaction(userWalletId, transactionId = TX_ID)

        // Assert
        assertThat(actual.isRight()).isTrue()
        coVerify(exactly = 1) { tangemPayApi.getTransactionLegacy(AUTH_HEADER, TX_ID) }
        coVerify(exactly = 0) { tangemPayApi.getTransaction(any(), any()) }
    }

    @Test
    fun `GIVEN cashback toggle enabled WHEN getTransaction THEN new endpoint is called`() = runTest {
        // Arrange
        every { featureToggles.isCashbackEnabled } returns true
        coEvery { tangemPayApi.getTransaction(any(), any()) } returns
            ApiResponse.Success(transactionResponse())

        // Act
        val actual = createRepository().getTransaction(userWalletId, transactionId = TX_ID)

        // Assert
        assertThat(actual.isRight()).isTrue()
        coVerify(exactly = 1) { tangemPayApi.getTransaction(AUTH_HEADER, TX_ID) }
        coVerify(exactly = 0) { tangemPayApi.getTransactionLegacy(any(), any()) }
    }

    @Test
    fun `GIVEN cashback toggle disabled WHEN history reloaded THEN legacy endpoint is called`() = runTest {
        // Arrange
        every { featureToggles.isCashbackEnabled } returns false
        coEvery { tangemPayApi.getTangemPayTxHistoryLegacy(any(), any(), any()) } returns
            ApiResponse.Success(historyResponse())

        // Act
        reloadHistory(createRepository())

        // Assert
        coVerify(exactly = 1) { tangemPayApi.getTangemPayTxHistoryLegacy(AUTH_HEADER, cursor = null, limit = any()) }
        coVerify(exactly = 0) { tangemPayApi.getTangemPayTxHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN cashback toggle enabled WHEN history reloaded THEN new endpoint is called`() = runTest {
        // Arrange
        every { featureToggles.isCashbackEnabled } returns true
        coEvery { tangemPayApi.getTangemPayTxHistory(any(), any(), any()) } returns
            ApiResponse.Success(historyResponse())

        // Act
        reloadHistory(createRepository())

        // Assert
        coVerify(exactly = 1) { tangemPayApi.getTangemPayTxHistory(AUTH_HEADER, cursor = null, limit = any()) }
        coVerify(exactly = 0) { tangemPayApi.getTangemPayTxHistoryLegacy(any(), any(), any()) }
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
            tangemPayFeatureToggles = featureToggles,
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