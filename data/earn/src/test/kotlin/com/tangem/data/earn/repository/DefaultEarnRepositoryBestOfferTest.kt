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
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.assertEither
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/** Only the first one-row page of the requested type is stubbed, so a request with other parameters fails the test. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultEarnRepositoryBestOfferTest {

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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN offers of a type WHEN getBestOffer THEN returns the first row`(model: TypeModel) = runTest {
        // Arrange
        stubFirstRow(type = model.apiType, response = response(items = listOf(earnResponse(type = model.apiType))))

        // Act
        val actual = repository.getBestOffer(model.type)

        // Assert
        val expected = EarnToken(
            apy = "0.24",
            networkId = "ethereum",
            rewardType = EarnRewardType.APY,
            type = model.type,
            tokenId = "eth",
            tokenSymbol = "ETH",
            tokenName = "ETH",
            tokenAddress = null,
            decimalCount = 18,
        )
        assertEither(actual, Either.Right(expected))
    }

    private fun provideTestModels() = listOf(
        TypeModel(type = EarnType.STAKING, apiType = "staking"),
        TypeModel(type = EarnType.YIELD, apiType = "yield"),
    )

    @Test
    fun `GIVEN no offers WHEN getBestOffer THEN returns null`() = runTest {
        // Arrange
        stubFirstRow(type = "staking", response = response(items = emptyList()))

        // Act
        val actual = repository.getBestOffer(EarnType.STAKING)

        // Assert
        assertEither(actual, Either.Right(null))
    }

    @Test
    fun `GIVEN the api fails WHEN getBestOffer THEN returns its error`() = runTest {
        // Arrange
        val error = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
            message = "boom",
            errorBody = null,
        )
        @Suppress("UNCHECKED_CAST")
        stubFirstRow(type = "yield", response = ApiResponse.Error(cause = error) as ApiResponse<EarnListResponse>)

        // Act
        val actual = repository.getBestOffer(EarnType.YIELD)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(error))
    }

    internal data class TypeModel(val type: EarnType, val apiType: String)

    private fun stubFirstRow(type: String, response: ApiResponse<EarnListResponse>) {
        coEvery {
            tangemTechApi.getEarnTokens(
                isForEarn = false,
                page = "1",
                limit = 1,
                type = type,
                networks = null,
                apyType = "gross",
            )
        } returns response
    }

    private fun response(items: List<EarnResponse>): ApiResponse<EarnListResponse> = ApiResponse.Success(
        data = EarnListResponse(
            meta = MetaEarnListResponse(page = 1, limit = 1, total = items.size, hasNext = false),
            items = items,
        ),
    )

    private fun earnResponse(type: String) = EarnResponse(
        apy = "0.24",
        networkId = "ethereum",
        rewardType = "apy",
        type = type,
        token = EarnTokenResponse(id = "eth", symbol = "ETH", name = "ETH", address = null, decimalCount = 18),
    )
}