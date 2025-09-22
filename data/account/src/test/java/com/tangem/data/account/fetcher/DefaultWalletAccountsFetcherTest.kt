package com.tangem.data.account.fetcher

import com.google.common.truth.Truth
import com.tangem.data.account.converter.createGetWalletAccountsResponse
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.IF_NONE_MATCH_HEADER
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultWalletAccountsFetcherTest {

    private val tangemTechApi: TangemTechApi = mockk()

    private val accountsResponseStoreFactory: AccountsResponseStoreFactory = mockk()
    private val accountsResponseStore: AccountsResponseStore = mockk()
    private val accountsResponseStoreFlow = MutableStateFlow<GetWalletAccountsResponse?>(value = null)

    private val userTokensSaver: UserTokensSaver = mockk(relaxUnitFun = true)
    private val fetchWalletAccountsErrorHandler: FetchWalletAccountsErrorHandler = mockk(relaxUnitFun = true)
    private val eTagsStore: ETagsStore = mockk(relaxUnitFun = true)

    private val fetcher: DefaultWalletAccountsFetcher = DefaultWalletAccountsFetcher(
        tangemTechApi = tangemTechApi,
        accountsResponseStoreFactory = accountsResponseStoreFactory,
        userTokensSaver = userTokensSaver,
        fetchWalletAccountsErrorHandler = fetchWalletAccountsErrorHandler,
        eTagsStore = eTagsStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")
    private val eTag = "etag"

    @BeforeAll
    fun setUp() {
        every { accountsResponseStoreFactory.create(userWalletId) } returns accountsResponseStore
        every { accountsResponseStore.data } returns accountsResponseStoreFlow

        coEvery { eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.WalletAccounts) } returns eTag
    }

    @AfterEach
    fun tearDown() {
        clearMocks(
            tangemTechApi,
            userTokensSaver,
            fetchWalletAccountsErrorHandler,
        )

        accountsResponseStoreFlow.value = null
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Fetch {

        @Test
        fun `fetch should call assignTokens when unassignedTokens are not empty`() = runTest {
            // Arrange
            val savedAccountsResponse = null
            val unassignedToken = createToken(accountId = null)

            val accountsResponse = createGetWalletAccountsResponse(
                userWalletId = userWalletId,
                unassignedTokens = listOf(unassignedToken),
            )
            val newETag = "newEtag"
            val apiResponse = ApiResponse.Success(
                data = accountsResponse,
                headers = mapOf(IF_NONE_MATCH_HEADER to listOf(newETag)),
            )

            accountsResponseStoreFlow.value = savedAccountsResponse

            val accountId = "957B88B12730E646E0F33D3618B77DFA579E8231E3C59C7104BE7165611C8027"
            val updatedAccountsResponse = accountsResponse.copy(
                accounts = accountsResponse.accounts.map {
                    it.copy(tokens = listOf(unassignedToken.copy(accountId = accountId)))
                },
                unassignedTokens = emptyList(),
            )

            coEvery {
                tangemTechApi.getWalletAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns apiResponse

            coEvery { accountsResponseStore.updateData(any()) } returns accountsResponse

            coEvery {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(updatedAccountsResponse.accounts),
                )
            } returns ApiResponse.Success(data = Unit)

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.data
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
                tangemTechApi.getWalletAccounts(walletId = userWalletId.stringValue, eTag = eTag)
                eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = newETag)
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.updateData(any())
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(updatedAccountsResponse.accounts),
                )
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.updateData(any())
            }

            coVerify(inverse = true) {
                fetchWalletAccountsErrorHandler.handle(
                    error = any(),
                    userWalletId = any(),
                    savedAccountsResponse = any(),
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )
            }
        }

        @Test
        fun `fetch should not call assignTokens when unassignedTokens are empty`() = runTest {
            // Arrange
            val savedAccountsResponse = null
            val accountsResponse = createGetWalletAccountsResponse(
                userWalletId = userWalletId,
                unassignedTokens = emptyList(),
            )
            val newETag = "newEtag"
            val apiResponse = ApiResponse.Success(
                data = accountsResponse,
                headers = mapOf(IF_NONE_MATCH_HEADER to listOf(newETag)),
            )

            accountsResponseStoreFlow.value = savedAccountsResponse

            coEvery {
                tangemTechApi.getWalletAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns apiResponse

            coEvery { accountsResponseStore.updateData(any()) } returns accountsResponse

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.data
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
                tangemTechApi.getWalletAccounts(walletId = userWalletId.stringValue, eTag = eTag)
                eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = newETag)
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.updateData(any())
            }

            coVerify(inverse = true) {
                fetchWalletAccountsErrorHandler.handle(
                    error = any(),
                    userWalletId = any(),
                    savedAccountsResponse = any(),
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )

                tangemTechApi.saveWalletAccounts(walletId = any(), eTag = any(), body = any())
                userTokensSaver.push(any(), any())
            }
        }

        @Test
        fun `fetch should call error handler when getWalletAccounts returns error`() = runTest {
            // Arrange
            val savedAccountsResponse = null
            val apiError = ApiResponse.Error(ApiResponseError.NetworkException)

            accountsResponseStoreFlow.value = savedAccountsResponse

            coEvery {
                tangemTechApi.getWalletAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns apiError as ApiResponse<GetWalletAccountsResponse>

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.data
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
                tangemTechApi.getWalletAccounts(walletId = userWalletId.stringValue, eTag = eTag)

                fetchWalletAccountsErrorHandler.handle(
                    error = apiError.cause,
                    userWalletId = userWalletId,
                    savedAccountsResponse = null,
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )
            }

            coVerify(inverse = true) {
                eTagsStore.store(userWalletId = any(), key = any(), value = any())
                tangemTechApi.saveWalletAccounts(walletId = any(), eTag = any(), body = any())
                userTokensSaver.push(userWalletId = any(), response = any())
            }
        }

        @Test
        fun `push should throw error when saveWalletAccounts returns PRECONDITION_FAILED`() = runTest {
            // Arrange
            val savedAccountsResponse = null
            val unassignedToken = createToken(accountId = null)

            val accountsResponse = createGetWalletAccountsResponse(
                userWalletId = userWalletId,
                unassignedTokens = listOf(unassignedToken),
            )

            val newETag = "newEtag"
            val apiResponse = ApiResponse.Success(
                data = accountsResponse,
                headers = mapOf(IF_NONE_MATCH_HEADER to listOf(newETag)),
            )

            val apiError = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.PRECONDITION_FAILED,
                message = null,
                errorBody = null,
            )
            val saveApiResponse = ApiResponse.Error(apiError)

            accountsResponseStoreFlow.value = savedAccountsResponse

            val accountId = "957B88B12730E646E0F33D3618B77DFA579E8231E3C59C7104BE7165611C8027"
            val updatedAccountsResponse = accountsResponse.copy(
                accounts = accountsResponse.accounts.map {
                    it.copy(tokens = listOf(unassignedToken.copy(accountId = accountId)))
                },
                unassignedTokens = emptyList(),
            )

            coEvery {
                tangemTechApi.getWalletAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns apiResponse

            coEvery { accountsResponseStore.updateData(any()) } returns accountsResponse

            coEvery {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(updatedAccountsResponse.accounts),
                )
            } returns saveApiResponse as ApiResponse<Unit>

            // Act
            val actual = runCatching { fetcher.fetch(userWalletId) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isEqualTo(apiError)

            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.data
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
                tangemTechApi.getWalletAccounts(walletId = userWalletId.stringValue, eTag = eTag)
                eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = newETag)
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.updateData(any())
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(updatedAccountsResponse.accounts),
                )
            }

            coVerify(inverse = true) {
                fetchWalletAccountsErrorHandler.handle(
                    error = any(),
                    userWalletId = any(),
                    savedAccountsResponse = any(),
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Store {

        @Test
        fun `store should update data in AccountsResponseStore`() = runTest {
            // Arrange
            val response = createGetWalletAccountsResponse(userWalletId = userWalletId)
            coEvery { accountsResponseStore.updateData(any()) } returns response

            // Act
            fetcher.store(userWalletId = userWalletId, response = response)

            // Assert
            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.updateData(any())
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Push {

        @Test
        fun `push should call saveWalletAccounts with correct params`() = runTest {
            // Arrange
            val accounts = listOf(
                mockk<WalletAccountDTO>(),
            )
            val response = SaveWalletAccountsResponse(accounts)

            coEvery {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = response,
                )
            } returns ApiResponse.Success(data = Unit)

            // Act
            fetcher.push(userWalletId, response)

            // Assert
            coVerify {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = response,
                )
            }
        }

        @Test
        fun `push should throw error when saveWalletAccounts returns PRECONDITION_FAILED`() = runTest {
            // Arrange
            val accounts = listOf(
                mockk<WalletAccountDTO>(),
            )
            val response = SaveWalletAccountsResponse(accounts)
            val apiError = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.PRECONDITION_FAILED,
                message = null,
                errorBody = null,
            )
            val saveApiResponse = ApiResponse.Error(apiError)
            coEvery {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = response,
                )
            } returns saveApiResponse as ApiResponse<Unit>

            // Act
            val actual = runCatching { fetcher.push(userWalletId, response) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isEqualTo(apiError)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PushAndStore {

        @Test
        fun `pushAndStore should call push and store with correct params`() = runTest {
            // Arrange
            listOf(
                mockk<WalletAccountDTO>(),
            )
            val response = createGetWalletAccountsResponse(userWalletId)

            coEvery {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(accounts = response.accounts),
                )
            } returns ApiResponse.Success(data = Unit)

            coEvery { accountsResponseStore.updateData(any()) } returns mockk()

            // Act
            fetcher.pushAndStore(userWalletId, response)

            // Assert
            coVerifyOrder {
                tangemTechApi.saveWalletAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(accounts = response.accounts),
                )
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.updateData(any())
            }
        }
    }

    private fun createToken(
        networkId: String = "ethereum",
        derivationPath: String = "m/44'/60'/0'/0/0",
        list: List<String> = emptyList(),
        name: String = "Ethereum",
        symbol: String = "ETH",
        decimals: Int = 18,
        contractAddress: String? = null,
        id: String? = null,
        accountId: String? = null,
    ) = UserTokensResponse.Token(
        id = id,
        accountId = accountId,
        networkId = networkId,
        derivationPath = derivationPath,
        name = name,
        symbol = symbol,
        decimals = decimals,
        contractAddress = contractAddress,
        addresses = list,
    )
}