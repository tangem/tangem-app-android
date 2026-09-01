package com.tangem.data.account.fetcher

import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.account.api.WalletAccountsApi
import com.tangem.data.account.converter.createGetWalletAccountsResponse
import com.tangem.data.account.converter.createWalletAccountDTO
import com.tangem.data.account.fetcher.DefaultWalletAccountsFetcher.FetchResult
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.tokens.DefaultMainAccountTokensMigration
import com.tangem.data.account.utils.DefaultWalletAccountsResponseFactory
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.toUserTokensResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

/**
[REDACTED_AUTHOR]
 */
@Suppress("Unchecked_Cast", "UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultWalletAccountsFetcherTest {

    private val walletAccountsApi: WalletAccountsApi = mockk()

    private val accountsResponseStoreFactory: AccountsResponseStoreFactory = mockk()
    private val accountsResponseStore: AccountsResponseStore = mockk()
    private val accountsResponseStoreFlow = MutableStateFlow<GetWalletAccountsResponse?>(value = null)
    private val tokensMigration: DefaultMainAccountTokensMigration = mockk()

    private val userTokensSaver: UserTokensSaver = mockk(relaxUnitFun = true)
    private val fetchWalletAccountsErrorHandler: FetchWalletAccountsErrorHandler = mockk()
    private val defaultWalletAccountsResponseFactory: DefaultWalletAccountsResponseFactory = mockk()
    private val eTagsStore: ETagsStore = mockk(relaxUnitFun = true)
    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()

    private val fetcher: DefaultWalletAccountsFetcher = DefaultWalletAccountsFetcher(
        walletAccountsApi = walletAccountsApi,
        accountsResponseStoreFactory = accountsResponseStoreFactory,
        userTokensSaver = userTokensSaver,
        fetchWalletAccountsErrorHandler = fetchWalletAccountsErrorHandler,
        defaultWalletAccountsResponseFactory = defaultWalletAccountsResponseFactory,
        eTagsStore = eTagsStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
        mainAccountTokensMigration = tokensMigration,
        jointAccountFeatureToggles = jointAccountFeatureToggles,
    )

    private val userWalletId = UserWalletId("011")
    private val eTag = "etag"
    private val eTagKey = ETagsStore.Key.WalletAccounts
    private val migratedAccountsResponse = createGetWalletAccountsResponse(userWalletId)

    @BeforeAll
    fun setUp() {
        every { accountsResponseStoreFactory.create(userWalletId) } returns accountsResponseStore
        every { accountsResponseStore.data } returns accountsResponseStoreFlow

        coEvery { tokensMigration.migrate(userWalletId) } returns migratedAccountsResponse.right()
        coEvery { eTagsStore.getSyncOrNull(userWalletId, eTagKey) } returns eTag
    }

    @BeforeEach
    fun setUpEach() {
        every { jointAccountFeatureToggles.isJointAccountCreationEnabled } returns false
    }

    @AfterEach
    fun tearDown() {
        clearMocks(
            walletAccountsApi,
            userTokensSaver,
            fetchWalletAccountsErrorHandler,
        )

        accountsResponseStoreFlow.value = null
    }


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class JointAccountToggle {

        private val ownAccount = createWalletAccountDTO(userWalletId = userWalletId)
        private val jointAccount = createWalletAccountDTO(
            userWalletId = userWalletId,
            accountId = "9CC9C1C9A1F1B62B1F0FCBB5D4C7A34F1B8B98A6D3B3E1B94F6C8F0A1E2D3C4B",
            accountName = "Family",
            derivationIndex = 0,
            type = WalletAccountDTO.Type.JOINT.value,
        )

        @Test
        fun `GIVEN toggle is off WHEN fetch THEN joint records do not reach the store`() = runTest {
            // Arrange
            every { jointAccountFeatureToggles.isJointAccountCreationEnabled } returns false

            val stored = arrangeFetch(accounts = listOf(ownAccount, jointAccount))

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            Truth.assertThat(stored.captured.invoke(null)?.accounts).containsExactly(ownAccount)
        }

        @Test
        fun `GIVEN toggle is on WHEN fetch THEN joint records reach the store`() = runTest {
            // Arrange
            every { jointAccountFeatureToggles.isJointAccountCreationEnabled } returns true

            val stored = arrangeFetch(accounts = listOf(ownAccount, jointAccount))

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            Truth.assertThat(stored.captured.invoke(null)?.accounts)
                .containsExactly(ownAccount, jointAccount)
                .inOrder()
        }

        private fun arrangeFetch(
            accounts: List<WalletAccountDTO>,
        ): CapturingSlot<suspend (GetWalletAccountsResponse?) -> GetWalletAccountsResponse?> {
            val response = createGetWalletAccountsResponse(userWalletId).copy(accounts = accounts)

            coEvery {
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns ApiResponse.Success(data = response, headers = mapOf(ETAG_HEADER to listOf(eTag)))

            val transform = slot<suspend (GetWalletAccountsResponse?) -> GetWalletAccountsResponse?>()
            coEvery { accountsResponseStore.updateData(capture(transform)) } returns response

            return transform
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Fetch {

        @Test
        fun `fetch should call assignTokens when unassignedTokens are not empty`() = runTest {
            // Arrange
            val savedAccountsResponse = null
            val unassignedToken = createToken(accountId = null)

            val accounts = listOf(createWalletAccountDTO(userWalletId = userWalletId, tokens = null))
            val accountsResponse = createGetWalletAccountsResponse(
                userWalletId = userWalletId,
                unassignedTokens = listOf(unassignedToken),
            )
                .copy(accounts = accounts)

            val newETag = "newEtag"
            val apiResponse = ApiResponse.Success(
                data = accountsResponse,
                headers = mapOf(ETAG_HEADER to listOf(newETag)),
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
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns apiResponse

            coEvery { accountsResponseStore.updateData(any()) } returns accountsResponse

            coEvery {
                walletAccountsApi.saveAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(updatedAccountsResponse.accounts),
                )
            } returns ApiResponse.Success(data = updatedAccountsResponse)

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.data
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = eTagKey)
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)
                eTagsStore.store(userWalletId = userWalletId, key = eTagKey, value = newETag)
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.updateData(any())
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.updateData(any())
                tokensMigration.migrate(userWalletId)
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
                headers = mapOf(ETAG_HEADER to listOf(newETag)),
            )

            accountsResponseStoreFlow.value = savedAccountsResponse

            coEvery {
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns apiResponse

            coEvery { accountsResponseStore.updateData(any()) } returns accountsResponse

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.data
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = eTagKey)
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)
                eTagsStore.store(userWalletId = userWalletId, key = eTagKey, value = newETag)
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.updateData(any())
                tokensMigration.migrate(userWalletId)
            }

            coVerify(inverse = true) {
                fetchWalletAccountsErrorHandler.handle(
                    error = any(),
                    userWalletId = any(),
                    savedAccountsResponse = any(),
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )

                walletAccountsApi.saveAccounts(walletId = any(), eTag = any(), body = any())
                userTokensSaver.push(any(), any())
            }
        }

        @Test
        fun `fetch should call error handler when getWalletAccounts returns error`() = runTest {
            // Arrange
            val savedAccountsResponse = createGetWalletAccountsResponse(userWalletId)
            val apiError = ApiResponse.Error(ApiResponseError.NetworkException())

            accountsResponseStoreFlow.value = savedAccountsResponse

            coEvery {
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns apiError as ApiResponse<GetWalletAccountsResponse>

            coEvery {
                fetchWalletAccountsErrorHandler.handle(
                    error = apiError.cause,
                    userWalletId = userWalletId,
                    savedAccountsResponse = savedAccountsResponse,
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )
            } returns FetchResult(savedAccountsResponse)

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.data
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = eTagKey)
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)

                fetchWalletAccountsErrorHandler.handle(
                    error = apiError.cause,
                    userWalletId = userWalletId,
                    savedAccountsResponse = savedAccountsResponse,
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )
                tokensMigration.migrate(userWalletId)
            }

            coVerify(inverse = true) {
                eTagsStore.store(userWalletId = any(), key = any(), value = any())
                walletAccountsApi.saveAccounts(walletId = any(), eTag = any(), body = any())
                userTokensSaver.push(userWalletId = any(), response = any())
            }
        }

        @Test
        fun `GIVEN response with empty accounts and push request is failed THEN eTag will be cleared`() = runTest {
            // Arrange
            val savedAccountsResponse = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    group = null,
                    sort = null,
                    totalAccounts = 0,
                    totalArchivedAccounts = 0,
                ),
                accounts = emptyList(),
                unassignedTokens = emptyList(),
            )
            val getResponse = ApiResponse.Error(
                cause = ApiResponseError.HttpException(
                    code = ApiResponseError.HttpException.Code.NOT_MODIFIED,
                    message = null,
                    errorBody = null,
                ),
                headers = mapOf(ETAG_HEADER to listOf(eTag)),
            )

            accountsResponseStoreFlow.value = savedAccountsResponse

            coEvery {
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)
            } returns getResponse as ApiResponse<GetWalletAccountsResponse>

            coEvery {
                fetchWalletAccountsErrorHandler.handle(
                    error = getResponse.cause,
                    userWalletId = userWalletId,
                    savedAccountsResponse = savedAccountsResponse,
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )
            } returns FetchResult(savedAccountsResponse)

            coEvery {
                defaultWalletAccountsResponseFactory.create(userWalletId = userWalletId, userTokensResponse = null)
            } returns savedAccountsResponse

            coEvery { accountsResponseStore.updateData(any()) } returns savedAccountsResponse

            val saveResponse = ApiResponse.Error(ApiResponseError.TimeoutException())
            coEvery {
                walletAccountsApi.saveAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(savedAccountsResponse.accounts),
                )
            } returns saveResponse as ApiResponse<GetWalletAccountsResponse>

            // Act
            fetcher.fetch(userWalletId)

            // Assert
            coVerify {
                accountsResponseStoreFactory.create(userWalletId = userWalletId)
                accountsResponseStore.data
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = eTagKey)
                walletAccountsApi.getAccounts(walletId = userWalletId.stringValue, eTag = eTag)
                eTagsStore.store(userWalletId = userWalletId, key = eTagKey, value = eTag)
                accountsResponseStore.updateData(any())
                fetchWalletAccountsErrorHandler.handle(
                    error = getResponse.cause,
                    userWalletId = userWalletId,
                    savedAccountsResponse = savedAccountsResponse,
                    pushWalletAccounts = any(),
                    storeWalletAccounts = any(),
                )
                defaultWalletAccountsResponseFactory.create(userWalletId = userWalletId, userTokensResponse = null)
                eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = eTagKey)
                walletAccountsApi.saveAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = SaveWalletAccountsResponse(savedAccountsResponse.accounts),
                )
                eTagsStore.clear(userWalletId, eTagKey)
                userTokensSaver.push(
                    userWalletId = userWalletId,
                    response = savedAccountsResponse.toUserTokensResponse(),
                )
                tokensMigration.migrate(userWalletId)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Get {

        @Test
        fun get() = runTest {
            // Arrange
            val response = createGetWalletAccountsResponse(userWalletId = userWalletId)
            accountsResponseStoreFlow.value = response

            // Act
            val actual = getEmittedValues(fetcher.get(userWalletId))

            // Assert
            val expected = response
            Truth.assertThat(actual).containsExactly(expected)

            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
            }
        }

        @Test
        fun `get if store is empty`() = runTest {
            // Arrange
            accountsResponseStoreFlow.value = null

            // Act
            val actual = getEmittedValues(fetcher.get(userWalletId))

            // Assert
            Truth.assertThat(actual).isEmpty()

            coVerifyOrder {
                accountsResponseStoreFactory.create(userWalletId)
                accountsResponseStore.data
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
            val getResponse = createGetWalletAccountsResponse(userWalletId, tokens = null)
            val saveResponse = SaveWalletAccountsResponse(getResponse.accounts)

            coEvery {
                walletAccountsApi.saveAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = saveResponse,
                )
            } returns ApiResponse.Success(data = getResponse)

            // Act
            fetcher.push(userWalletId, saveResponse)

            // Assert
            coVerify {
                walletAccountsApi.saveAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = saveResponse,
                )
            }
        }

        @Test
        fun `push should throw error when saveWalletAccounts returns PRECONDITION_FAILED`() = runTest {
            // Arrange
            val accounts = listOf(
                createWalletAccountDTO(userWalletId = userWalletId, tokens = null),
            )
            val response = SaveWalletAccountsResponse(accounts)
            val apiError = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.PRECONDITION_FAILED,
                message = null,
                errorBody = null,
            )
            val saveApiResponse = ApiResponse.Error(apiError)
            coEvery {
                walletAccountsApi.saveAccounts(
                    walletId = userWalletId.stringValue,
                    eTag = eTag,
                    body = response,
                )
            } returns saveApiResponse as ApiResponse<GetWalletAccountsResponse>

            // Act
            val actual = runCatching { fetcher.push(userWalletId, response) }.exceptionOrNull()!!

            // Assert
            Truth.assertThat(actual).isEqualTo(apiError)
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