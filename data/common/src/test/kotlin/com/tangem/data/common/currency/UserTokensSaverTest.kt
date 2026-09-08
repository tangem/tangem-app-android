package com.tangem.data.common.currency

import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.cache.etag.WalletAccountsETags
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsV2Response
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTokensSaverTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxUnitFun = true)
    private val enricher: UserTokensResponseAddressesEnricher = mockk()
    private val walletServerBinder: WalletServerBinder = mockk()
    private val eTagsStore: ETagsStore = mockk(relaxUnitFun = true)
    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()

    private val userTokensSaver: UserTokensSaver = UserTokensSaver(
        tangemTechApi = tangemTechApi,
        userWalletsListRepository = userWalletsListRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
        addressesEnricher = enricher,
        walletServerBinder = walletServerBinder,
        pushTokensRetryerPool = mockk(),
        walletAccountsETags = WalletAccountsETags(eTagsStore = eTagsStore),
        jointAccountFeatureToggles = jointAccountFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            tangemTechApi,
            userWalletsListRepository,
            enricher,
            walletServerBinder,
            eTagsStore,
            jointAccountFeatureToggles,
        )
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
        coEvery { eTagsStore.getSyncOrNull(any(), any()) } returns null
    }

    @Test
    fun `GIVEN user wallet id and response WHEN push AND api call fails THEN should log error and call onFailSend`() =
        runTest {
            // Arrange
            val userWalletId = UserWalletId("1234567890abcdef")
            stubUserWallet(userWalletId)

            val response = createResponse(tokens = emptyList(), sort = UserTokensResponse.SortType.BALANCE)
            val enrichedResponse = createResponse(tokens = emptyList(), sort = UserTokensResponse.SortType.MANUAL)
            val error = ApiResponseError.UnknownException(Exception("API Error"))
            var onFailSendCalled = false

            coEvery { enricher(userWalletId, response) } returns enrichedResponse
            coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returns
                ApiResponse.Error(error) as ApiResponse<Unit>

            // Act
            userTokensSaver.push(
                userWalletId = userWalletId,
                response = response,
                onFailSend = { onFailSendCalled = true },
            )

            // Assert
            coVerifyOrder {
                userWalletsListRepository.userWallets
                enricher(userWalletId, response)
                tangemTechApi.saveTokens(userWalletId.stringValue, enrichedResponse, null)
            }
            assert(onFailSendCalled) { "onFailSend callback should be called when API call fails" }
        }

    @Test
    fun `GIVEN response with duplicated tokens WHEN push THEN duplicates are dropped before the api call`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("1234567890abcdef")
        stubUserWallet(userWalletId)

        val token = createToken()
        val response = createResponse(tokens = listOf(token, token))
        val uniqueResponse = createResponse(tokens = listOf(token))

        coEvery { enricher(userWalletId, uniqueResponse) } returns uniqueResponse
        coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returns ApiResponse.Success(Unit)

        // Act
        userTokensSaver.push(userWalletId = userWalletId, response = response)

        // Assert
        coVerify(exactly = 1) { tangemTechApi.saveTokens(userWalletId.stringValue, uniqueResponse, null) }
    }

    @Test
    fun `GIVEN tokens differing in account id only WHEN push THEN both of them are pushed`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("1234567890abcdef")
        stubUserWallet(userWalletId)

        val token = createToken()
        val response = createResponse(tokens = listOf(token, token.copy(accountId = "other-account")))

        coEvery { enricher(userWalletId, response) } returns response
        coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returns ApiResponse.Success(Unit)

        // Act
        userTokensSaver.push(userWalletId = userWalletId, response = response)

        // Assert
        coVerify(exactly = 1) { tangemTechApi.saveTokens(userWalletId.stringValue, response, null) }
    }

    @Test
    fun `GIVEN joint toggle on and stored etag WHEN push THEN if-match sent and refreshed from the response`() =
        runTest {
            // Arrange
            val userWalletId = UserWalletId("1234567890abcdef")
            stubUserWallet(userWalletId)
            every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
            coEvery {
                eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.WalletAccounts)
            } returns STALE_ETAG

            val response = createResponse(tokens = listOf(createToken()))
            coEvery { enricher(userWalletId, response) } returns response
            coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returns ApiResponse.Success(
                data = Unit,
                headers = mapOf(ETAG_HEADER to listOf(FRESH_ETAG)),
            )

            // Act
            userTokensSaver.push(userWalletId = userWalletId, response = response)

            // Assert
            coVerify(exactly = 1) { tangemTechApi.saveTokens(userWalletId.stringValue, response, STALE_ETAG) }
            coVerify(exactly = 1) {
                eTagsStore.store(userWalletId, ETagsStore.Key.WalletAccounts, FRESH_ETAG)
            }
        }

    @Test
    fun `GIVEN successful write without etag header WHEN push THEN stored etag is dropped as stale`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("1234567890abcdef")
        stubUserWallet(userWalletId)
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
        coEvery { eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.WalletAccounts) } returns STALE_ETAG

        val response = createResponse(tokens = listOf(createToken()))
        coEvery { enricher(userWalletId, response) } returns response
        coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returns ApiResponse.Success(Unit)

        // Act
        userTokensSaver.push(userWalletId = userWalletId, response = response)

        // Assert
        coVerify(exactly = 1) { eTagsStore.clear(userWalletId, ETagsStore.Key.WalletAccounts) }
        coVerify(exactly = 0) { eTagsStore.store(any(), any(), any()) }
    }

    @Test
    fun `GIVEN write rejected with 412 WHEN push THEN fresh state united with pushed list and retried`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("1234567890abcdef")
        stubUserWallet(userWalletId)
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
        coEvery {
            eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.WalletAccounts)
        } returnsMany listOf(STALE_ETAG, FRESH_ETAG)

        val pushedToken = createToken()
        val peerToken = createToken().copy(id = "polygon", networkId = "polygon")
        val response = createResponse(tokens = listOf(pushedToken))
        coEvery { enricher(userWalletId, response) } returns response
        coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returnsMany listOf(
            preconditionFailed(),
            ApiResponse.Success(Unit),
        )
        coEvery { tangemTechApi.getWalletAccountsV2(walletId = userWalletId.stringValue) } returns ApiResponse.Success(
            data = createAccountsV2Response(tokens = listOf(pushedToken, peerToken)),
            headers = mapOf(ETAG_HEADER to listOf(FRESH_ETAG)),
        )

        // Act
        userTokensSaver.push(userWalletId = userWalletId, response = response)

        // Assert
        val merged = response.copy(tokens = listOf(pushedToken, peerToken))
        coVerifyOrder {
            tangemTechApi.saveTokens(userWalletId.stringValue, response, STALE_ETAG)
            tangemTechApi.getWalletAccountsV2(walletId = userWalletId.stringValue)
            eTagsStore.store(userWalletId, ETagsStore.Key.WalletAccounts, FRESH_ETAG)
            tangemTechApi.saveTokens(userWalletId.stringValue, merged, FRESH_ETAG)
        }
    }

    @Test
    fun `GIVEN joint toggle off WHEN write returns 412 THEN no recovery and the push fails`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("1234567890abcdef")
        stubUserWallet(userWalletId)

        val response = createResponse(tokens = listOf(createToken()))
        var onFailSendCalled = false
        coEvery { enricher(userWalletId, response) } returns response
        coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returns preconditionFailed()

        // Act
        userTokensSaver.push(userWalletId = userWalletId, response = response, onFailSend = { onFailSendCalled = true })

        // Assert
        coVerify(exactly = 0) { tangemTechApi.getWalletAccountsV2(walletId = any()) }
        assert(onFailSendCalled) { "onFailSend callback should be called when 412 recovery is disabled" }
    }

    @Test
    fun `GIVEN wallet rebound after 404 WHEN retry hits 412 THEN merge recovery still applies`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("1234567890abcdef")
        stubUserWallet(userWalletId)
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
        coEvery { eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.WalletAccounts) } returns STALE_ETAG

        val token = createToken()
        val response = createResponse(tokens = listOf(token))
        coEvery { enricher(userWalletId, response) } returns response
        coEvery { walletServerBinder.bind(any<UserWallet>()) } returns ApiResponse.Success(Unit)
        coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returnsMany listOf(
            ApiResponse.Error(
                ApiResponseError.HttpException(
                    code = ApiResponseError.HttpException.Code.NOT_FOUND,
                    message = null,
                    errorBody = null,
                ),
            ) as ApiResponse<Unit>,
            preconditionFailed(),
            ApiResponse.Success(Unit),
        )
        coEvery { tangemTechApi.getWalletAccountsV2(walletId = userWalletId.stringValue) } returns ApiResponse.Success(
            data = createAccountsV2Response(tokens = listOf(token)),
            headers = mapOf(ETAG_HEADER to listOf(FRESH_ETAG)),
        )

        // Act
        userTokensSaver.push(userWalletId = userWalletId, response = response)

        // Assert
        coVerify(exactly = 1) { tangemTechApi.getWalletAccountsV2(walletId = userWalletId.stringValue) }
        coVerify(exactly = 3) { tangemTechApi.saveTokens(any(), any(), any()) }
    }

    @Test
    fun `GIVEN backend tokens lacking account ids WHEN merging after 412 THEN they take the owning account id`() =
        runTest {
            // Arrange
            val userWalletId = UserWalletId("1234567890abcdef")
            stubUserWallet(userWalletId)
            every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
            coEvery {
                eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.WalletAccounts)
            } returnsMany listOf(STALE_ETAG, FRESH_ETAG)

            val pushedToken = createToken()
            val response = createResponse(tokens = listOf(pushedToken))
            coEvery { enricher(userWalletId, response) } returns response
            coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returnsMany listOf(
                preconditionFailed(),
                ApiResponse.Success(Unit),
            )
            // The same token, but as GET returns it: without the accountId the owning account implies
            coEvery {
                tangemTechApi.getWalletAccountsV2(walletId = userWalletId.stringValue)
            } returns ApiResponse.Success(
                data = createAccountsV2Response(tokens = listOf(pushedToken.copy(accountId = null))),
                headers = mapOf(ETAG_HEADER to listOf(FRESH_ETAG)),
            )

            // Act
            userTokensSaver.push(userWalletId = userWalletId, response = response)

            // Assert
            coVerify(exactly = 1) { tangemTechApi.saveTokens(userWalletId.stringValue, response, FRESH_ETAG) }
        }

    @Test
    fun `GIVEN refetched state without etag header WHEN merging after 412 THEN stored etag is kept`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("1234567890abcdef")
        stubUserWallet(userWalletId)
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
        coEvery { eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.WalletAccounts) } returns STALE_ETAG

        val response = createResponse(tokens = listOf(createToken()))
        coEvery { enricher(userWalletId, response) } returns response
        coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returnsMany listOf(
            preconditionFailed(),
            preconditionFailed(),
        )
        coEvery { tangemTechApi.getWalletAccountsV2(walletId = userWalletId.stringValue) } returns ApiResponse.Success(
            data = createAccountsV2Response(tokens = emptyList()),
        )

        // Act
        userTokensSaver.push(userWalletId = userWalletId, response = response)

        // Assert
        coVerify(exactly = 0) { eTagsStore.clear(any(), any()) }
        coVerify(exactly = 0) { eTagsStore.store(any(), any(), any()) }
    }

    @Test
    fun `GIVEN 412 and failed refetch WHEN push THEN onFailSend is called`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("1234567890abcdef")
        stubUserWallet(userWalletId)
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
        coEvery { eTagsStore.getSyncOrNull(userWalletId, ETagsStore.Key.WalletAccounts) } returns STALE_ETAG

        val response = createResponse(tokens = listOf(createToken()))
        var onFailSendCalled = false
        coEvery { enricher(userWalletId, response) } returns response
        coEvery { tangemTechApi.saveTokens(any(), any(), any()) } returns preconditionFailed()
        coEvery { tangemTechApi.getWalletAccountsV2(walletId = userWalletId.stringValue) } returns
            ApiResponse.Error(ApiResponseError.UnknownException(Exception("boom"))) as
            ApiResponse<GetWalletAccountsV2Response>

        // Act
        userTokensSaver.push(userWalletId = userWalletId, response = response, onFailSend = { onFailSendCalled = true })

        // Assert
        coVerify(exactly = 1) { tangemTechApi.saveTokens(any(), any(), any()) }
        assert(onFailSendCalled) { "onFailSend callback should be called when the 412 recovery fails" }
    }

    private fun stubUserWallet(userWalletId: UserWalletId) {
        val userWallet = mockk<UserWallet.Cold> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.name } returns ""
        }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
    }

    private fun preconditionFailed(): ApiResponse<Unit> {
        val cause = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.PRECONDITION_FAILED,
            message = null,
            errorBody = null,
        )
        return ApiResponse.Error(cause) as ApiResponse<Unit>
    }

    private fun createAccountsV2Response(tokens: List<UserTokensResponse.Token>): GetWalletAccountsV2Response {
        return GetWalletAccountsV2Response(
            wallet = GetWalletAccountsV2Response.Wallet(
                version = 0,
                group = UserTokensResponse.GroupType.NETWORK,
                sort = UserTokensResponse.SortType.BALANCE,
                totalCryptoAccounts = 1,
                totalArchivedAccounts = 0,
                totalJointAccounts = 0,
            ),
            accounts = listOf(
                WalletAccountDTO(
                    id = "account",
                    name = "Main",
                    derivationIndex = 0,
                    icon = "Family",
                    iconColor = "Azure",
                    tokens = tokens,
                ),
            ),
            unassignedTokens = emptyList(),
        )
    }

    private fun createResponse(
        tokens: List<UserTokensResponse.Token>,
        sort: UserTokensResponse.SortType = UserTokensResponse.SortType.BALANCE,
    ): UserTokensResponse {
        return UserTokensResponse(
            version = 0,
            group = UserTokensResponse.GroupType.NETWORK,
            sort = sort,
            tokens = tokens,
            walletName = null,
            walletType = WalletType.COLD,
        )
    }

    private fun createToken(): UserTokensResponse.Token {
        return UserTokensResponse.Token(
            id = "ethereum",
            accountId = "account",
            networkId = "ethereum",
            derivationPath = "m/44'/60'/0'/0/1",
            name = "Ethereum",
            symbol = "ETH",
            decimals = 18,
            contractAddress = null,
        )
    }

    private companion object {
        const val STALE_ETAG = """{"a":1,"t":1}"""
        const val FRESH_ETAG = """{"a":1,"t":2}"""
    }
}