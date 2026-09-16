package com.tangem.data.earn.repository.batch

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.EarnListResponse
import com.tangem.datasource.api.tangemTech.models.EarnResponse
import com.tangem.datasource.api.tangemTech.models.EarnTokenResponse
import com.tangem.datasource.api.tangemTech.models.MetaEarnListResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.pagination.BatchFetchResult
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * `last` used to be derived from the *mapped* row count against the batch size, so a single row whose
 * network the mapping drops ended the tab's pagination early. It now follows the server's `hasNext`.
 */
internal class EarnTokensBatchFetcherTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val selectedWallet = MutableStateFlow<UserWallet?>(MockUserWalletFactory.create())

    private val fetcher = EarnTokensBatchFetcher(
        tangemTechApi = tangemTechApi,
        batchSize = BATCH_SIZE,
        userWalletsListRepository = userWalletsListRepository,
        cryptoCurrencyFactory = CryptoCurrencyFactory(ExcludedBlockchains()),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemTechApi, userWalletsListRepository)
        selectedWallet.value = MockUserWalletFactory.create()
        every { userWalletsListRepository.selectedUserWallet } returns selectedWallet
    }

    @Test
    fun `GIVEN hasNext is true WHEN a page is fetched THEN it is not the last one`() = runTest {
        // Arrange
        coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returns page(hasNext = true)

        // Act
        val result = fetcher.fetchFirst(config())

        // Assert
        assertThat((result as BatchFetchResult.Success).last).isFalse()
    }

    @Test
    fun `GIVEN a full page of rows the mapping drops WHEN it is fetched THEN it is still not the last one`() =
        runTest {
            // Arrange — the old rule counted the mapped rows and would have stopped the list here
            coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returns page(
                hasNext = true,
                networkId = "not-a-real-network",
            )

            // Act
            val result = fetcher.fetchFirst(config()) as BatchFetchResult.Success

            // Assert
            assertThat(result.data).isEmpty()
            assertThat(result.last).isFalse()
        }

    @Test
    fun `GIVEN hasNext is false WHEN a full page is fetched THEN it is the last one`() = runTest {
        // Arrange
        coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returns page(hasNext = false)

        // Act
        val result = fetcher.fetchFirst(config())

        // Assert
        assertThat((result as BatchFetchResult.Success).last).isTrue()
    }

    @Test
    fun `GIVEN no hasNext WHEN a short page is fetched THEN it is the last one`() = runTest {
        // Arrange
        coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returns page(
            hasNext = null,
            itemCount = 1,
        )

        // Act
        val result = fetcher.fetchFirst(config())

        // Assert
        assertThat((result as BatchFetchResult.Success).last).isTrue()
    }

    @Test
    fun `GIVEN no hasNext WHEN the server reports its own smaller limit THEN a full page is not the last one`() =
        runTest {
            // Arrange — the server clamped the request, so its limit decides what "full" means
            coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returns page(
                hasNext = null,
                itemCount = SERVER_LIMIT,
                serverLimit = SERVER_LIMIT,
            )

            // Act
            val result = fetcher.fetchFirst(config())

            // Assert
            assertThat((result as BatchFetchResult.Success).last).isFalse()
        }

    private fun config() = EarnTokensListConfig(type = null, networks = null, isForEarn = false)

    private fun page(
        hasNext: Boolean?,
        itemCount: Int = BATCH_SIZE,
        serverLimit: Int = BATCH_SIZE,
        networkId: String = "ethereum",
    ): ApiResponse<EarnListResponse> = ApiResponse.Success(
        data = EarnListResponse(
            meta = MetaEarnListResponse(page = 1, limit = serverLimit, total = 51, hasNext = hasNext),
            items = List(itemCount) { index ->
                EarnResponse(
                    apy = "0.038400",
                    networkId = networkId,
                    rewardType = "apy",
                    type = "yield",
                    token = EarnTokenResponse(
                        id = "usd-coin-$index",
                        symbol = "USDC",
                        name = "USDC",
                        address = "0xabc$index",
                        decimalCount = 6,
                    ),
                )
            },
        ),
    )

    private companion object {

        const val BATCH_SIZE = 20
        const val SERVER_LIMIT = 10
    }
}