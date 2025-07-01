package com.tangem.data.tokens

import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.assertEither
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.tokens.utils.CustomTokensMerger
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.exchangeservice.swap.ExpressServiceLoader
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.isMultiCurrency
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
internal class DefaultMultiWalletCryptoCurrenciesFetcherTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val userTokensResponseFactory = UserTokensResponseFactory()

    private val userWalletsStore: UserWalletsStore = mockk(relaxUnitFun = true)
    private val tangemTechApi: TangemTechApi = mockk()
    private val customTokensMerger: CustomTokensMerger = mockk()
    private val userTokensResponseStore: UserTokensResponseStore = mockk(relaxUnitFun = true)
    private val userTokensSaver: UserTokensSaver = mockk(relaxUnitFun = true)
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()
    private val expressServiceLoader: ExpressServiceLoader = mockk(relaxUnitFun = true)

    private val fetcher = DefaultMultiWalletCryptoCurrenciesFetcher(
        demoConfig = DemoConfig(),
        userWalletsStore = userWalletsStore,
        tangemTechApi = tangemTechApi,
        customTokensMerger = customTokensMerger,
        userTokensResponseStore = userTokensResponseStore,
        userTokensSaver = userTokensSaver,
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
        expressServiceLoader = expressServiceLoader,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            userWalletsStore,
            tangemTechApi,
            userTokensResponseStore,
            userTokensSaver,
            cardCryptoCurrencyFactory,
            expressServiceLoader,
        )
    }

    @Test
    fun `fetch failure if UserWallet ISN'T MULTI-CURRENCY wallet`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)

        val mockUserWallet = mockk<UserWallet> {
            every { isMultiCurrency } returns false
        }

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = IllegalStateException("${this::class.simpleName} supports only multi-currency wallet").left()
        assertEither(actual, expected)

        verifyOrder { userWalletsStore.getSyncStrict(key = params.userWalletId) }
        coVerify(inverse = true) {
            userTokensResponseStore.getSyncOrNull(any())
        }
    }

    @Test
    fun `fetch successfully if CARD IS DEMO and STORED TOKENS ARE EMPTY`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)

        val mockUserWallet = mockk<UserWallet.Cold> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
            every { cardId } returns "AC01000000041225"
        }

        val defaultCoins = listOf(
            cryptoCurrencyFactory.createCoin(Blockchain.Bitcoin),
            cryptoCurrencyFactory.createCoin(Blockchain.Ethereum),
        )

        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NONE,
            sort = UserTokensResponse.SortType.MANUAL,
            tokens = listOf(
                userTokensResponseFactory.createResponseToken(defaultCoins.first()),
                userTokensResponseFactory.createResponseToken(defaultCoins.last()),
            ),
        )

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet
        coEvery { userTokensResponseStore.getSyncOrNull(userWalletId = params.userWalletId) } returns null
        every {
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(mockUserWallet.scanResponse)
        } returns defaultCoins
        coEvery {
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = userTokensResponse)
        } returns userTokensResponse

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            userTokensResponseStore.getSyncOrNull(userWalletId = params.userWalletId)
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(scanResponse = mockUserWallet.scanResponse)
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = userTokensResponse)
            userTokensSaver.store(userWalletId = params.userWalletId, response = userTokensResponse)
            expressServiceLoader.update(userWallet = mockUserWallet, userTokens = userTokensResponse.toLeastTokens())
        }
    }

    @Test
    fun `fetch successfully if CARD IS DEMO and STORED TOKENS AREN'T EMPTY`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)

        val mockUserWallet = mockk<UserWallet.Cold> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
            every { cardId } returns "AC01000000041225"
        }

        val apiResponse = ApiResponse.Success(
            data = defaultResponse.copy(group = UserTokensResponse.GroupType.TOKEN),
        )

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet
        coEvery { userTokensResponseStore.getSyncOrNull(userWalletId = params.userWalletId) } returns defaultResponse
        coEvery { tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue) } returns apiResponse
        coEvery {
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = apiResponse.data)
        } returns apiResponse.data

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            userTokensResponseStore.getSyncOrNull(userWalletId = params.userWalletId)
            tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue)
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = apiResponse.data)
            userTokensSaver.store(userWalletId = params.userWalletId, response = apiResponse.data)
            expressServiceLoader.update(userWallet = mockUserWallet, userTokens = defaultResponse.toLeastTokens())
        }

        coVerify(inverse = true) {
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(scanResponse = any())
        }
    }

    @Test
    fun `fetch successfully if CARD ISN'T DEMO`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)

        val mockUserWallet = mockk<UserWallet.Cold> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
            every { cardId } returns "cardID"
        }

        val apiResponse = ApiResponse.Success(
            data = defaultResponse.copy(group = UserTokensResponse.GroupType.TOKEN),
        )

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet
        coEvery { tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue) } returns apiResponse
        coEvery {
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = apiResponse.data)
        } returns apiResponse.data

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue)
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = apiResponse.data)
            userTokensSaver.store(userWalletId = params.userWalletId, response = apiResponse.data)
            expressServiceLoader.update(userWallet = mockUserWallet, userTokens = defaultResponse.toLeastTokens())
        }

        coVerify(inverse = true) {
            userTokensResponseStore.getSyncOrNull(userWalletId = any())
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(scanResponse = any())
        }
    }

    @Test
    fun `fetch successfully if API request RETURNS TIMEOUT EXCEPTION and STORED TOKENS ARE EMPTY`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)

        val mockUserWallet = mockk<UserWallet.Cold> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
            every { cardId } returns "cardID"
        }

        @Suppress("UNCHECKED_CAST")
        val apiResponse = ApiResponse.Error(
            cause = ApiResponseError.TimeoutException,
        ) as ApiResponse<UserTokensResponse>

        val defaultCoins = listOf(
            cryptoCurrencyFactory.createCoin(Blockchain.Bitcoin),
            cryptoCurrencyFactory.createCoin(Blockchain.Ethereum),
        )

        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NONE,
            sort = UserTokensResponse.SortType.MANUAL,
            tokens = listOf(
                userTokensResponseFactory.createResponseToken(defaultCoins.first()),
                userTokensResponseFactory.createResponseToken(defaultCoins.last()),
            ),
        )

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet
        coEvery { tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue) } returns apiResponse
        coEvery { userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId) } returns null
        coEvery {
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(mockUserWallet.scanResponse)
        } returns defaultCoins
        coEvery {
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = userTokensResponse)
        } returns userTokensResponse

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue)
            userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId)
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = userTokensResponse)
            userTokensSaver.store(userWalletId = params.userWalletId, response = userTokensResponse)
            expressServiceLoader.update(userWallet = mockUserWallet, userTokens = userTokensResponse.toLeastTokens())
        }

        coVerify(inverse = true) {
            userTokensSaver.push(userWalletId = any(), response = any())
        }
    }

    @Test
    fun `fetch successfully if API request RETURNS TIMEOUT EXCEPTION and STORED TOKENS AREN'T EMPTY`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)

        val mockUserWallet = mockk<UserWallet.Cold> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
            every { cardId } returns "cardID"
        }

        @Suppress("UNCHECKED_CAST")
        val apiResponse = ApiResponse.Error(
            cause = ApiResponseError.TimeoutException,
        ) as ApiResponse<UserTokensResponse>

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet
        coEvery { tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue) } returns apiResponse
        coEvery { userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId) } returns defaultResponse
        coEvery {
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = defaultResponse)
        } returns defaultResponse

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue)
            userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId)
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = defaultResponse)
            userTokensSaver.store(userWalletId = params.userWalletId, response = defaultResponse)
            expressServiceLoader.update(userWallet = mockUserWallet, userTokens = defaultResponse.toLeastTokens())
        }

        coVerify(inverse = true) {
            userTokensSaver.push(userWalletId = any(), response = any())
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(scanResponse = any())
        }
    }

    @Test
    fun `fetch successfully if API request RETURNS NOT FOUND EXCEPTION and STORED TOKENS ARE EMPTY`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)

        val mockUserWallet = mockk<UserWallet.Cold> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
            every { cardId } returns "cardID"
        }

        @Suppress("UNCHECKED_CAST")
        val apiResponse = ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.NOT_FOUND,
                message = null,
                errorBody = null,
            ),
        ) as ApiResponse<UserTokensResponse>

        val defaultCoins = listOf(
            cryptoCurrencyFactory.createCoin(Blockchain.Bitcoin),
            cryptoCurrencyFactory.createCoin(Blockchain.Ethereum),
        )

        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NONE,
            sort = UserTokensResponse.SortType.MANUAL,
            tokens = listOf(
                userTokensResponseFactory.createResponseToken(defaultCoins.first()),
                userTokensResponseFactory.createResponseToken(defaultCoins.last()),
            ),
        )

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet
        coEvery { tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue) } returns apiResponse
        coEvery { userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId) } returns null
        coEvery {
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(mockUserWallet.scanResponse)
        } returns defaultCoins
        coEvery {
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = userTokensResponse)
        } returns userTokensResponse

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue)
            userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId)
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(mockUserWallet.scanResponse)
            userTokensSaver.push(userWalletId = params.userWalletId, response = userTokensResponse)
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = userTokensResponse)
            userTokensSaver.store(userWalletId = params.userWalletId, response = userTokensResponse)
            expressServiceLoader.update(userWallet = mockUserWallet, userTokens = userTokensResponse.toLeastTokens())
        }
    }

    @Test
    fun `fetch successfully if API request RETURNS NOT FOUND EXCEPTION and STORED TOKENS AREN'T EMPTY`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)

        val mockUserWallet = mockk<UserWallet.Cold> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
            every { cardId } returns "cardID"
        }

        @Suppress("UNCHECKED_CAST")
        val apiResponse = ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.NOT_FOUND,
                message = null,
                errorBody = null,
            ),
        ) as ApiResponse<UserTokensResponse>

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet
        coEvery { tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue) } returns apiResponse
        coEvery { userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId) } returns defaultResponse
        coEvery {
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = defaultResponse)
        } returns defaultResponse

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            tangemTechApi.getUserTokens(userId = params.userWalletId.stringValue)
            userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId)
            userTokensSaver.push(userWalletId = params.userWalletId, response = defaultResponse)
            customTokensMerger.mergeIfPresented(userWalletId = params.userWalletId, response = defaultResponse)
            userTokensSaver.store(userWalletId = params.userWalletId, response = defaultResponse)
            expressServiceLoader.update(userWallet = mockUserWallet, userTokens = defaultResponse.toLeastTokens())
        }

        coVerify(inverse = true) {
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyCard(scanResponse = any())
        }
    }

    private companion object {
        val userWalletId = UserWalletId("011")

        val defaultResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NONE,
            sort = UserTokensResponse.SortType.MANUAL,
            tokens = listOf(
                UserTokensResponse.Token(
                    id = null,
                    networkId = "bitcoin",
                    derivationPath = null,
                    name = "Bitcoin",
                    symbol = "BTC",
                    decimals = 8,
                    contractAddress = null,
                    addresses = listOf(),
                ),
            ),
        )

        fun UserTokensResponse.toLeastTokens(): List<LeastTokenInfo> {
            return tokens.map { token ->
                LeastTokenInfo(
                    contractAddress = token.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE,
                    network = token.networkId,
                )
            }
        }
    }
}