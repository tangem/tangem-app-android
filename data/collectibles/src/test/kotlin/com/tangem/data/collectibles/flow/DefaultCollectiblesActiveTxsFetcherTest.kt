package com.tangem.data.collectibles.flow

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.collectibles.store.CollectiblesActiveTxsStore
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsFetcher
import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.collectibles.CollectiblesApi
import com.tangem.grow.datasource.collectibles.models.response.ActiveTxsResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultCollectiblesActiveTxsFetcherTest {

    private val collectiblesApi: CollectiblesApi = mockk()
    private lateinit var store: CollectiblesActiveTxsStore
    private lateinit var fetcher: DefaultCollectiblesActiveTxsFetcher

    @BeforeEach
    fun setUp() {
        clearMocks(collectiblesApi)
        store = CollectiblesActiveTxsStore(store = RuntimeSharedStore())
        fetcher = DefaultCollectiblesActiveTxsFetcher(
            collectiblesApi = collectiblesApi,
            activeTxsStore = store,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    @Test
    fun `GIVEN active purchases WHEN fetched THEN they are stored in order with their machine and turbo flag`() =
        runTest {
            // Arrange
            val response = NetworkJsonFactory.create().decodeFromString<ActiveTxsResponse>(
                """
                {"activeTxs":[
                  {"txId":"tx-1","status":"pack-opening-error","packOpeningErrorCode":"3999",
                   "machine":{"externalId":"pokemon_50","name":"Elite Pack"},"turboModeEnabled":false},
                  {"txId":"tx-2","status":"pack-purchased",
                   "machine":{"externalId":"pokemon_25","name":"Pack","shortName":"PKMN"},"turboModeEnabled":true},
                  {"txId":"tx-3","status":"waiting-for-payment","turboModeEnabled":false}
                ]}
                """.trimIndent(),
            )
            coEvery { collectiblesApi.getActiveTxs(any(), any()) } returns ApiResponse.Success(response)

            // Act
            val actual = fetcher(PARAMS)

            // Assert
            assertThat(actual.isRight()).isTrue()
            assertThat(store.get(WALLET_ID).first()).containsExactly(
                activeTx(txId = "tx-1", status = CollectibleTxStatus.REFUNDING, machineId = "pokemon_50"),
                activeTx(txId = "tx-2", status = CollectibleTxStatus.PACK_PURCHASED, machineId = "pokemon_25")
                    .copy(isTurboModeEnabled = true),
                activeTx(txId = "tx-3", status = CollectibleTxStatus.WAITING_FOR_PAYMENT, machineId = null),
            ).inOrder()
            coVerify(exactly = 1) {
                collectiblesApi.getActiveTxs(userWalletId = WALLET_ID.stringValue, includeMachine = true)
            }
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `GIVEN an earlier read WHEN the next fetch fails THEN it is a left and the earlier read stays`() = runTest {
        // Arrange
        val stored = listOf(activeTx(txId = "tx-1", status = CollectibleTxStatus.PACK_PURCHASED, machineId = null))
        store.store(userWalletId = WALLET_ID, activeTxs = stored)
        coEvery { collectiblesApi.getActiveTxs(any(), any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<ActiveTxsResponse>

        // Act
        val actual = fetcher(PARAMS)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(ApiResponseError.NetworkException::class.java)
        assertThat(store.get(WALLET_ID).first()).isEqualTo(stored)
    }

    private fun activeTx(txId: String, status: CollectibleTxStatus, machineId: String?) = CollectibleActiveTx(
        txId = txId,
        status = status,
        machineId = machineId,
        isTurboModeEnabled = false,
    )

    private companion object {
        val WALLET_ID = UserWalletId("aa")
        val PARAMS = CollectiblesActiveTxsFetcher.Params(WALLET_ID)
    }
}