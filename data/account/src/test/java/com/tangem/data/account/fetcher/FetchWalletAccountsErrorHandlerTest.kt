package com.tangem.data.account.fetcher

import com.tangem.data.account.converter.CryptoPortfolioConverter
import com.tangem.data.account.utils.toUserTokensResponse
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
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

    private val userTokensSaver: UserTokensSaver = mockk(relaxUnitFun = true)
    private val userWalletsStore: UserWalletsStore = mockk()
    private val userTokensResponseStore: UserTokensResponseStore = mockk()
    private val cryptoPortfolioCF: CryptoPortfolioConverter.Factory = mockk()
    private val cryptoPortfolioConverter = mockk<CryptoPortfolioConverter>()
    private val userTokensResponseFactory: UserTokensResponseFactory = mockk()
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()

    private val handler = FetchWalletAccountsErrorHandler(
        userTokensSaver = userTokensSaver,
        userWalletsStore = userWalletsStore,
        userTokensResponseStore = userTokensResponseStore,
        cryptoPortfolioCF = cryptoPortfolioCF,
        userTokensResponseFactory = userTokensResponseFactory,
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
    )

    private val userWallet = mockk<UserWallet> {
        every { this@mockk.walletId } returns userWalletId
    }

    @BeforeEach
    fun setupEach() {
        clearMocks(
            userTokensSaver,
            userWalletsStore,
            userTokensResponseStore,
            cryptoPortfolioCF,
            cryptoPortfolioConverter,
            cardCryptoCurrencyFactory,
        )
    }

    @Test
    fun `does not update accounts when response is up to date`() = runTest {
        // Arrange
        val error = ApiResponseError.HttpException(
            code = Code.NOT_MODIFIED,
            message = "Not Modified",
            errorBody = null,
        )

        val pushWalletAccounts: suspend (UserWalletId, List<WalletAccountDTO>) -> Unit = mockk()
        val storeWalletAccounts: suspend (UserWalletId, GetWalletAccountsResponse) -> Unit = mockk()

        // Act
        handler.handle(
            error = error,
            userWalletId = userWalletId,
            savedAccountsResponse = null,
            pushWalletAccounts = pushWalletAccounts,
            storeWalletAccounts = storeWalletAccounts,
        )

        // Assert
        coVerify(inverse = true) {
            userWalletsStore.getSyncStrict(key = any())
            userTokensResponseStore.getSyncOrNull(userWalletId = any())
            userTokensResponseFactory.createUserTokensResponse(any(), any(), any())
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(any())
            cryptoPortfolioCF.create(any())
            cryptoPortfolioConverter.convertListBack(any())
            pushWalletAccounts(any(), any())
            userTokensSaver.push(userWalletId = any(), response = any())
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

        val pushWalletAccounts: suspend (UserWalletId, List<WalletAccountDTO>) -> Unit = mockk(relaxed = true)
        val storeWalletAccounts: suspend (UserWalletId, GetWalletAccountsResponse) -> Unit = mockk(relaxed = true)

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
            pushWalletAccounts(userWalletId, listOf(accountDTO))
            userTokensSaver.push(userWalletId, response = savedAccountsResponse.toUserTokensResponse())
            storeWalletAccounts(userWalletId, savedAccountsResponse)
        }

        coVerify(inverse = true) {
            userWalletsStore.getSyncStrict(key = any())
            userTokensResponseStore.getSyncOrNull(userWalletId = any())
            userTokensResponseFactory.createUserTokensResponse(any(), any(), any())
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(any())
            cryptoPortfolioCF.create(any())
            cryptoPortfolioConverter.convertListBack(any())
        }
    }

    @Test
    fun `uses default accounts when savedAccountsResponse is null`() = runTest {
        // Arrange
        val error = ApiResponseError.TimeoutException

        val accounts = AccountList.empty(userWallet).accounts
            .filterIsInstance<Account.CryptoPortfolio>()

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

        every { userWalletsStore.getSyncStrict(userWalletId) } returns userWallet
        every { cryptoPortfolioCF.create(userWallet) } returns cryptoPortfolioConverter
        every { cryptoPortfolioConverter.convertListBack(accounts) } returns listOf(accountDTO)
        coEvery { userTokensResponseStore.getSyncOrNull(userWalletId) } returns null
        every {
            userTokensResponseFactory.createUserTokensResponse(
                currencies = emptyList(),
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )
        } returns userTokensResponse
        every { cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(userWallet) } returns emptyList()

        val pushWalletAccounts: suspend (UserWalletId, List<WalletAccountDTO>) -> Unit = mockk(relaxed = true)
        val storeWalletAccounts: suspend (UserWalletId, GetWalletAccountsResponse) -> Unit = mockk(relaxed = true)

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
            userWalletsStore.getSyncStrict(userWalletId)
            cryptoPortfolioCF.create(userWallet)
            cryptoPortfolioConverter.convertListBack(accounts)
            userTokensResponseStore.getSyncOrNull(userWalletId)
            userTokensResponseFactory.createUserTokensResponse(
                currencies = emptyList(),
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(userWallet)
            storeWalletAccounts(userWalletId, any())
        }

        coVerify(inverse = true) {
            pushWalletAccounts(any(), any())
            userTokensSaver.push(userWalletId = any(), response = any())
        }
    }

    private companion object {

        val userWalletId = UserWalletId("011")
    }
}