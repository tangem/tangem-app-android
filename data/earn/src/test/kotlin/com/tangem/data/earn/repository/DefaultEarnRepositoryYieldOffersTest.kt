package com.tangem.data.earn.repository

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.earn.DefaultEarnErrorResolver
import com.tangem.data.earn.datastore.EarnFilterStore
import com.tangem.data.earn.datastore.EarnNetworksStore
import com.tangem.data.earn.datastore.EarnTokensCatalogStore
import com.tangem.data.earn.datastore.EarnTopTokensStore
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.EarnListResponse
import com.tangem.datasource.api.tangemTech.models.EarnResponse
import com.tangem.datasource.api.tangemTech.models.EarnTokenResponse
import com.tangem.datasource.api.tangemTech.models.MetaEarnListResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.earn.EarnYieldOffers
import com.tangem.test.core.assertEither
import com.tangem.test.core.assertEitherLeft
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Only the first yield page across all networks is stubbed, so a request with other parameters fails the test. */
internal class DefaultEarnRepositoryYieldOffersTest {

    private val tangemTechApi: TangemTechApi = mockk()

    private val repository = DefaultEarnRepository(
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        userWalletsListRepository = mockk<UserWalletsListRepository>(),
        earnNetworksStore = EarnNetworksStore(),
        earnTopTokensStore = EarnTopTokensStore(),
        earnTokensCatalogStore = EarnTokensCatalogStore(),
        earnFilterStore = EarnFilterStore(),
        earnErrorResolver = DefaultEarnErrorResolver(),
        excludedBlockchains = ExcludedBlockchains(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemTechApi)
    }

    @Test
    fun `GIVEN yield offers WHEN getYieldOffers THEN returns the total and the first row`() = runTest {
        // Arrange
        stubYieldPage(
            response(
                total = 32,
                items = listOf(
                    earnResponse(symbol = "USDT", networkId = "ethereum", apy = "0.092"),
                    earnResponse(symbol = "USDC", networkId = "base", apy = "0.051"),
                ),
            ),
        )

        // Act
        val actual = repository.getYieldOffers()

        // Assert
        val expected = EarnYieldOffers(
            total = 32,
            best = EarnToken(
                apy = "0.092",
                networkId = "ethereum",
                rewardType = EarnRewardType.APY,
                type = EarnType.YIELD,
                tokenId = "usdt",
                tokenSymbol = "USDT",
                tokenName = "USDT",
                tokenAddress = "0xabc",
                decimalCount = 6,
            ),
        )
        assertEither(actual, Either.Right(expected))
    }

    @Test
    fun `GIVEN no offers WHEN getYieldOffers THEN returns no best offer`() = runTest {
        // Arrange
        stubYieldPage(response(total = 0, items = emptyList()))

        // Act
        val actual = repository.getYieldOffers()

        // Assert
        assertEither(actual, Either.Right(EarnYieldOffers(total = 0, best = null)))
    }

    @Test
    fun `GIVEN a response without total WHEN getYieldOffers THEN returns an error`() = runTest {
        // Arrange
        stubYieldPage(response(total = null))

        // Act
        val actual = repository.getYieldOffers()

        // Assert
        assertEitherLeft(actual, IllegalStateException("Earn list response has no total"))
    }

    @Test
    fun `GIVEN the api fails WHEN getYieldOffers THEN returns its error`() = runTest {
        // Arrange
        val error = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
            message = "boom",
            errorBody = null,
        )
        stubYieldPage(httpError(error))

        // Act
        val actual = repository.getYieldOffers()

        // Assert
        assertThat(actual).isEqualTo(Either.Left(error))
    }

    private fun stubYieldPage(response: ApiResponse<EarnListResponse>) {
        coEvery {
            tangemTechApi.getEarnTokens(isForEarn = false, page = "1", limit = 20, type = "yield", networks = null)
        } returns response
    }

    /** `ApiResponse.Error` is an `ApiResponse<Nothing>`, and the type is invariant. */
    @Suppress("UNCHECKED_CAST")
    private fun httpError(error: ApiResponseError): ApiResponse<EarnListResponse> =
        ApiResponse.Error(cause = error) as ApiResponse<EarnListResponse>

    private fun response(
        total: Int? = 1,
        items: List<EarnResponse> = listOf(earnResponse()),
    ): ApiResponse<EarnListResponse> =
        ApiResponse.Success(
            data = EarnListResponse(
                meta = MetaEarnListResponse(page = 1, limit = 20, total = total, hasNext = false),
                items = items,
            ),
        )

    private fun earnResponse(
        symbol: String = "USDT",
        networkId: String = "ethereum",
        apy: String = "0.092",
    ) = EarnResponse(
        apy = apy,
        networkId = networkId,
        rewardType = "apy",
        type = "yield",
        token = EarnTokenResponse(
            id = symbol.lowercase(),
            symbol = symbol,
            name = symbol,
            address = "0xabc",
            decimalCount = 6,
        ),
    )
}