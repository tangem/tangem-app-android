package com.tangem.data.earn.repository.batch

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.earn.createEarnResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.EarnListResponse
import com.tangem.datasource.api.tangemTech.models.MetaEarnListResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.pagination.BatchFetchResult
import io.mockk.MockKMatcherScope
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
        apyType = APY_TYPE,
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
        coEvery { getEarnTokensWithApyType() } returns page(hasNext = true)

        // Act
        val result = fetcher.fetchFirst(config())

        // Assert
        assertThat((result as BatchFetchResult.Success).isLast).isFalse()
    }

    @Test
    fun `GIVEN a full page of rows the mapping drops WHEN it is fetched THEN it is still not the last one`() =
        runTest {
            // Arrange — the old rule counted the mapped rows and would have stopped the list here
            coEvery { getEarnTokensWithApyType() } returns page(
                hasNext = true,
                networkId = "not-a-real-network",
            )

            // Act
            val result = fetcher.fetchFirst(config()) as BatchFetchResult.Success

            // Assert
            assertThat(result.data).isEmpty()
            assertThat(result.isLast).isFalse()
        }

    @Test
    fun `GIVEN hasNext is false WHEN a full page is fetched THEN it is the last one`() = runTest {
        // Arrange
        coEvery { getEarnTokensWithApyType() } returns page(hasNext = false)

        // Act
        val result = fetcher.fetchFirst(config())

        // Assert
        assertThat((result as BatchFetchResult.Success).isLast).isTrue()
    }

    @Test
    fun `GIVEN no hasNext WHEN a short page is fetched THEN it is the last one`() = runTest {
        // Arrange
        coEvery { getEarnTokensWithApyType() } returns page(
            hasNext = null,
            itemCount = 1,
        )

        // Act
        val result = fetcher.fetchFirst(config())

        // Assert
        assertThat((result as BatchFetchResult.Success).isLast).isTrue()
    }

    @Test
    fun `GIVEN no hasNext WHEN the server reports its own smaller limit THEN a full page is not the last one`() =
        runTest {
            // Arrange — the server clamped the request, so its limit decides what "full" means
            coEvery { getEarnTokensWithApyType() } returns page(
                hasNext = null,
                itemCount = SERVER_LIMIT,
                serverLimit = SERVER_LIMIT,
            )

            // Act
            val result = fetcher.fetchFirst(config())

            // Assert
            assertThat((result as BatchFetchResult.Success).isLast).isFalse()
        }

    @Test
    fun `GIVEN vaults included WHEN a page is fetched THEN only rows of an unknown type are dropped`() = runTest {
        // Arrange
        coEvery { getEarnTokensWithApyType() } returns pageOfMixedTypes()

        // Act
        val result = fetcher.fetchFirst(config(isVaultsIncluded = true)) as BatchFetchResult.Success

        // Assert
        assertThat(result.data.map { it.earnToken.tokenId to it.earnToken.type })
            .containsExactly("tether" to EarnType.YIELD, "ethereum" to EarnType.VAULT)
            .inOrder()
    }

    @Test
    fun `GIVEN vaults left out WHEN a page is fetched THEN vault rows are dropped too`() = runTest {
        // Arrange
        coEvery { getEarnTokensWithApyType() } returns pageOfMixedTypes()

        // Act
        val result = fetcher.fetchFirst(config(isVaultsIncluded = false)) as BatchFetchResult.Success

        // Assert
        assertThat(result.data.map { it.earnToken.tokenId to it.earnToken.type })
            .containsExactly("tether" to EarnType.YIELD)
        assertThat(result.isLast).isFalse()
    }

    @Test
    fun `GIVEN a token returned on two pages WHEN the next page is fetched THEN it drops the token`() = runTest {
        // Arrange
        coEvery { getEarnTokensWithApyType() } returnsMany listOf(
            page(hasNext = true, tokenIds = listOf("tether", "usd-coin"), pageNumber = 1),
            page(hasNext = true, tokenIds = listOf("usd-coin", "dai"), pageNumber = 2),
        )
        val first = fetcher.fetchFirst(config())

        // Act
        val result = fetcher.fetchNext(overrideRequestParams = null, lastResult = first) as BatchFetchResult.Success

        // Assert
        assertThat(result.data.map { it.earnToken.tokenId }).containsExactly("dai")
        assertThat(result.isLast).isFalse()
    }

    @Test
    fun `GIVEN loaded pages WHEN the first page is fetched again THEN all its tokens are returned`() = runTest {
        // Arrange
        val firstPage = page(hasNext = true, tokenIds = listOf("tether", "usd-coin"), pageNumber = 1)
        coEvery { getEarnTokensWithApyType() } returnsMany listOf(
            firstPage,
            page(hasNext = true, tokenIds = listOf("dai"), pageNumber = 2),
            firstPage,
        )
        val first = fetcher.fetchFirst(config())
        fetcher.fetchNext(overrideRequestParams = null, lastResult = first)

        // Act
        val result = fetcher.fetchFirst(config()) as BatchFetchResult.Success

        // Assert
        assertThat(result.data.map { it.earnToken.tokenId }).containsExactly("tether", "usd-coin").inOrder()
    }

    private fun config(isVaultsIncluded: Boolean = false) = EarnTokensListConfig(
        type = null,
        networks = null,
        isForEarn = false,
        isVaultsIncluded = isVaultsIncluded,
    )

    private fun pageOfMixedTypes() = page(
        hasNext = true,
        tokenIds = listOf("tether", "ethereum", "usd-coin"),
        types = listOf("yield", "vault", "future-type"),
    )

    private fun page(
        hasNext: Boolean?,
        itemCount: Int = BATCH_SIZE,
        serverLimit: Int = BATCH_SIZE,
        networkId: String = "ethereum",
        tokenIds: List<String> = List(itemCount) { index -> "usd-coin-$index" },
        pageNumber: Int = 1,
        types: List<String> = List(tokenIds.size) { "yield" },
    ): ApiResponse<EarnListResponse> = ApiResponse.Success(
        data = EarnListResponse(
            meta = MetaEarnListResponse(page = pageNumber, limit = serverLimit, total = 51, hasNext = hasNext),
            items = tokenIds.mapIndexed { index, tokenId ->
                createEarnResponse(
                    apy = "0.038400",
                    networkId = networkId,
                    type = types[index],
                    tokenId = tokenId,
                    tokenSymbol = "USDC",
                    tokenName = "USDC",
                    tokenAddress = "0xabc$index",
                    decimalCount = 6,
                )
            },
        ),
    )

    private suspend fun MockKMatcherScope.getEarnTokensWithApyType() = tangemTechApi.getEarnTokens(
        isForEarn = any(),
        page = any(),
        limit = any(),
        type = any(),
        networks = any(),
        apyType = APY_TYPE,
    )

    private companion object {

        const val APY_TYPE = "gross"
        const val BATCH_SIZE = 20
        const val SERVER_LIMIT = 10
    }
}