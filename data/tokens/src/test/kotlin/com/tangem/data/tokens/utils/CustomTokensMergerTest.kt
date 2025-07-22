package com.tangem.data.tokens.utils

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CustomTokensMergerTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userTokensSaver: UserTokensSaver = mockk(relaxUnitFun = true)
    private val merger = CustomTokensMerger(
        tangemTechApi = tangemTechApi,
        userTokensSaver = userTokensSaver,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemTechApi, userTokensSaver)
    }

    @Test
    fun `mergeIfPresented successfully if UserTokensResponse DOESN'T CONTAIN custom tokens`() = runTest {
        // Act
        val actual = merger.mergeIfPresented(userWalletId = userWalletId, response = defaultUserTokensResponse)

        // Assert
        val expected = defaultUserTokensResponse
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify(inverse = true) {
            tangemTechApi.getCoins(contractAddress = any(), networkIds = any())
            userTokensSaver.push(any(), any())
        }
    }

    @Test
    fun `mergeIfPresented successfully if UserTokensResponse CONTAINS custom tokens`() = runTest {
        // Arrange
        val customToken = UserTokensResponse.Token(
            id = null,
            networkId = "cum",
            name = "Matilda Rich",
            symbol = "consectetuer",
            decimals = 9582,
            contractAddress = "contractAddress",
        )

        val userTokensResponse = defaultUserTokensResponse.copy(
            tokens = defaultUserTokensResponse.tokens + customToken,
        )

        coEvery {
            tangemTechApi.getCoins(contractAddress = customToken.contractAddress, networkIds = customToken.networkId)
        } returns ApiResponse.Success(data = createCoinResponse(token = customToken))

        // Act
        val actual = merger.mergeIfPresented(userWalletId = userWalletId, response = userTokensResponse)

        // Assert
        val updatedCustomToken = customToken.copy(
            id = "NEW_${customToken.id}",
            name = "NEW_${customToken.name}",
            symbol = "NEW_${customToken.symbol}",
        )

        val expected = defaultUserTokensResponse.copy(
            tokens = defaultUserTokensResponse.tokens + updatedCustomToken,
        )

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            tangemTechApi.getCoins(
                contractAddress = customToken.contractAddress,
                networkIds = customToken.networkId,
            )

            userTokensSaver.push(userWalletId = userWalletId, response = expected)
        }
    }

    @Test
    fun `mergeIfPresented successfully if API returns error`() = runTest {
        // Arrange
        val customToken = UserTokensResponse.Token(
            id = null,
            networkId = "cum",
            name = "Matilda Rich",
            symbol = "consectetuer",
            decimals = 9582,
            contractAddress = "contractAddress",
        )

        val userTokensResponse = defaultUserTokensResponse.copy(
            tokens = defaultUserTokensResponse.tokens + customToken,
        )

        @Suppress("UNCHECKED_CAST")
        val apiResponse = ApiResponse.Error(ApiResponseError.TimeoutException) as ApiResponse<CoinsResponse>

        coEvery {
            tangemTechApi.getCoins(contractAddress = customToken.contractAddress, networkIds = customToken.networkId)
        } returns apiResponse

        // Act
        val actual = merger.mergeIfPresented(userWalletId = userWalletId, response = userTokensResponse)

        // Assert
        val expected = userTokensResponse
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify {
            tangemTechApi.getCoins(
                contractAddress = customToken.contractAddress,
                networkIds = customToken.networkId,
            )
        }

        coVerify(inverse = true) { userTokensSaver.push(userWalletId = any(), response = any()) }
    }

    private companion object {

        val userWalletId = UserWalletId("011")

        val defaultUserTokensResponse = UserTokensResponseFactory().createUserTokensResponse(
            currencies = MockCryptoCurrencyFactory().ethereumAndStellar,
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        fun createCoinResponse(token: UserTokensResponse.Token): CoinsResponse {
            return CoinsResponse(
                imageHost = null,
                coins = listOf(
                    CoinsResponse.Coin(
                        id = "NEW_${token.id}",
                        name = "NEW_${token.name}",
                        symbol = "NEW_${token.symbol}",
                        active = true,
                    ),
                ),
                total = 1,
            )
        }
    }
}