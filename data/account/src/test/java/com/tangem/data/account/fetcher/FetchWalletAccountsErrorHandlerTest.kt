package com.tangem.data.account.fetcher

import com.tangem.data.account.converter.createGetWalletAccountsResponse
import com.tangem.data.account.converter.createWalletAccountDTO
import com.tangem.data.account.utils.DefaultWalletAccountsResponseFactory
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.WalletIdBody
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.toUserTokensResponse
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.wallet.UserWallet
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
class FetchWalletAccountsErrorHandlerTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsStore: UserWalletsStore = mockk()
    private val userTokensSaver: UserTokensSaver = mockk(relaxUnitFun = true)
    private val userTokensResponseStore: UserTokensResponseStore = mockk(relaxUnitFun = true)
    private val defaultWalletAccountsResponseFactory: DefaultWalletAccountsResponseFactory = mockk()
    private val eTagsStore: ETagsStore = mockk(relaxUnitFun = true)

    private val handler = FetchWalletAccountsErrorHandler(
        tangemTechApi = tangemTechApi,
        userWalletsStore = userWalletsStore,
        userTokensSaver = userTokensSaver,
        userTokensResponseStore = userTokensResponseStore,
        defaultWalletAccountsResponseFactory = defaultWalletAccountsResponseFactory,
        eTagsStore = eTagsStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val pushWalletAccounts: suspend (UserWalletId, List<WalletAccountDTO>) -> GetWalletAccountsResponse =
        mockk(relaxed = true)
    private val storeWalletAccounts: suspend (UserWalletId, GetWalletAccountsResponse) -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun setupEach() {
        clearMocks(
            userTokensSaver,
            userTokensResponseStore,
            defaultWalletAccountsResponseFactory,
        )
    }

    @Test
    fun `does not update accounts when response is up to date`() = runTest {
        // Arrange
        val response = createGetWalletAccountsResponse(userWalletId)

        val error = ApiResponseError.HttpException(
            code = Code.NOT_MODIFIED,
            message = "Not Modified",
            errorBody = null,
        )

        // Act
        handler.handle(
            error = error,
            userWalletId = userWalletId,
            savedAccountsResponse = response,
            pushWalletAccounts = pushWalletAccounts,
            storeWalletAccounts = storeWalletAccounts,
        )

        // Assert
        coVerify(inverse = true) {
            userTokensResponseStore.getSyncOrNull(userWalletId = any())
            defaultWalletAccountsResponseFactory.create(userWalletId = any(), userTokensResponse = any())
            pushWalletAccounts(any(), any())
            userTokensSaver.pushWithRetryer(userWalletId = any(), response = any())
            storeWalletAccounts(any(), any())
        }
    }

    @Test
    fun `pushes and stores accounts when NOT_FOUND error occurs`() = runTest {
        // Arrange
        val error = ApiResponseError.HttpException(
            code = Code.NOT_FOUND,
            message = "Not Found",
            errorBody = null,
        )

        val accountDTO = createWalletAccountDTO(userWalletId)

        val savedAccountsResponse = createGetWalletAccountsResponse(userWalletId)

        val eTagValue = "etag-value"
        val apiResponse = ApiResponse.Success(
            data = Unit,
            headers = mapOf(ETAG_HEADER to listOf(eTagValue)),
            code = Code.CREATED,
        )

        val walletName = "Wallet"
        val userWallet = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.name } returns walletName
        }
        coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns userWallet

        coEvery {
            tangemTechApi.createWallet(WalletIdBody(walletId = userWalletId.stringValue, name = walletName))
        } returns apiResponse
        coEvery { pushWalletAccounts(userWalletId, listOf(accountDTO)) } returns savedAccountsResponse

        // Act
        handler.handle(
            error = error,
            userWalletId = userWalletId,
            savedAccountsResponse = savedAccountsResponse,
            pushWalletAccounts = pushWalletAccounts,
            storeWalletAccounts = storeWalletAccounts,
        )

        // Assert
        coVerify {
            userTokensSaver.pushWithRetryer(userWalletId, response = savedAccountsResponse.toUserTokensResponse())
            tangemTechApi.createWallet(WalletIdBody(walletId = userWalletId.stringValue, name = "Wallet"))
            eTagsStore.store(userWalletId, ETagsStore.Key.WalletAccounts, eTagValue)
            pushWalletAccounts(userWalletId, listOf(accountDTO))
            storeWalletAccounts(userWalletId, savedAccountsResponse)
        }

        coVerify(inverse = true) {
            userTokensResponseStore.getSyncOrNull(userWalletId = any())
            defaultWalletAccountsResponseFactory.create(userWalletId = any(), userTokensResponse = any())
        }
    }

    @Test
    fun `uses default accounts when savedAccountsResponse is null`() = runTest {
        // Arrange
        val error = ApiResponseError.TimeoutException()

        val accountDTO = WalletAccountDTO(
            id = "nibh",
            name = "Michael Dotson",
            derivationIndex = 7135,
            icon = "consectetuer",
            iconColor = "ferri",
            tokens = listOf(),
            totalTokens = 7738,
            totalNetworks = 3348,
        )

        val savedAccountsResponse = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = UserTokensResponse.GroupType.NONE,
                sort = UserTokensResponse.SortType.MANUAL,
                totalAccounts = 1,
            ),
            accounts = listOf(accountDTO),
            unassignedTokens = emptyList(),
        )

        val userTokensResponse = savedAccountsResponse.toUserTokensResponse()

        coEvery { userTokensResponseStore.getSyncOrNull(userWalletId) } returns userTokensResponse
        coEvery {
            defaultWalletAccountsResponseFactory.create(userWalletId, userTokensResponse)
        } returns savedAccountsResponse

        // Act
        handler.handle(
            error = error,
            userWalletId = userWalletId,
            savedAccountsResponse = null,
            pushWalletAccounts = pushWalletAccounts,
            storeWalletAccounts = storeWalletAccounts,
        )

        // Assert
        coVerify {
            userTokensResponseStore.getSyncOrNull(userWalletId)
            defaultWalletAccountsResponseFactory.create(userWalletId, userTokensResponse)
            storeWalletAccounts(userWalletId, any())
        }

        coVerify(inverse = true) {
            pushWalletAccounts(any(), any())
            userTokensSaver.pushWithRetryer(userWalletId = any(), response = any())
        }
    }

    private companion object {

        val userWalletId = UserWalletId("011")
    }
}