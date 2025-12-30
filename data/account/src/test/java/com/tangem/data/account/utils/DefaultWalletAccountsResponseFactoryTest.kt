package com.tangem.data.account.utils

import com.google.common.truth.Truth
import com.tangem.data.account.converter.CryptoPortfolioConverter
import com.tangem.data.account.converter.createWalletAccountDTO
import com.tangem.data.account.utils.GetWalletAccountsResponseExtTest.Companion.createUserToken
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultWalletAccountsResponseFactoryTest {

    private val userWalletsStore = mockk<UserWalletsStore>()
    private val cryptoPortfolioCF = mockk<CryptoPortfolioConverter.Factory>()
    private val cryptoPortfolioConverter = mockk<CryptoPortfolioConverter>()
    private val userTokensResponseFactory = mockk<UserTokensResponseFactory>()
    private val networkFactory = mockk<NetworkFactory>()

    private val factory = DefaultWalletAccountsResponseFactory(
        userWalletsStore = userWalletsStore,
        cryptoPortfolioCF = cryptoPortfolioCF,
        userTokensResponseFactory = userTokensResponseFactory,
        networkFactory = networkFactory,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUpEach() {
        every { cryptoPortfolioCF.create(any()) } returns cryptoPortfolioConverter
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(
            userWalletsStore,
            cryptoPortfolioCF,
            cryptoPortfolioConverter,
            userTokensResponseFactory,
            networkFactory,
        )
    }

    @Test
    fun `create returns empty accounts when user wallet not found`() = runTest {
        // Arrange
        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
        )

        coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns null
        every {
            userTokensResponseFactory.createDefaultResponse(
                userWallet = null,
                networkFactory = networkFactory,
                accountId = null,
            )
        } returns userTokensResponse

        // Act
        val actual = factory.create(userWalletId = userWalletId, userTokensResponse = null)

        // Assert
        val expected = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = UserTokensResponse.GroupType.NETWORK,
                sort = UserTokensResponse.SortType.BALANCE,
                totalAccounts = 0,
                totalArchivedAccounts = 0,
            ),
            accounts = emptyList(),
            unassignedTokens = emptyList(),
        )
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            userWalletsStore.getSyncOrNull(userWalletId)
            userTokensResponseFactory.createDefaultResponse(
                userWallet = null,
                networkFactory = networkFactory,
                accountId = null,
            )
        }
    }

    @Test
    fun `create returns response with default tokens when userTokensResponse is null`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet>(relaxed = true) {
            every { walletId } returns userWalletId
        }

        coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns userWallet

        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()

        val token = createUserToken(accountIndex = 0)
        val defaultResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = listOf(token),
        )
        every {
            userTokensResponseFactory.createDefaultResponse(
                userWallet = userWallet,
                networkFactory = networkFactory,
                accountId = accounts.first().accountId,
            )
        } returns defaultResponse

        val accountsDTO = createWalletAccountDTO(userWalletId)
        every { cryptoPortfolioConverter.convertListBack(accounts) } returns listOf(accountsDTO)

        // Act
        val actual = factory.create(userWalletId, null)

        // Assert
        val expected = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = defaultResponse.group,
                sort = defaultResponse.sort,
                totalAccounts = 1,
                totalArchivedAccounts = 0,
            ),
            accounts = listOf(accountsDTO.copy(tokens = listOf(token))),
            unassignedTokens = emptyList(),
        )

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            userWalletsStore.getSyncOrNull(userWalletId)
            cryptoPortfolioConverter.convertListBack(accounts)
            userTokensResponseFactory.createDefaultResponse(
                userWallet = userWallet,
                networkFactory = networkFactory,
                accountId = accounts.first().accountId,
            )
        }
    }

    @Test
    fun `create returns response with default tokens when userTokensResponse is null and no default coins`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet>(relaxed = true) {
            every { walletId } returns userWalletId
        }

        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()

        coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns userWallet

        val defaultResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
        )

        every {
            userTokensResponseFactory.createDefaultResponse(
                userWallet = userWallet,
                networkFactory = networkFactory,
                accountId = accounts.first().accountId,
            )
        } returns defaultResponse

        every { cryptoPortfolioConverter.convertListBack(accounts) } returns emptyList()

        // Act
        val actual = factory.create(userWalletId, null)

        // Assert
        val expected = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = defaultResponse.group,
                sort = defaultResponse.sort,
                totalAccounts = 0,
                totalArchivedAccounts = 0,
            ),
            accounts = emptyList(),
            unassignedTokens = emptyList(),
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `create returns response with assigned tokens`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet>(relaxed = true) {
            every { walletId } returns userWalletId
        }
        coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns userWallet

        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = listOf(
                createUserToken(accountIndex = 0),
                createUserToken(accountIndex = 1),
            ),
        )

        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()
        val accountsDTO = createWalletAccountDTO(userWalletId = userWalletId)
        every { cryptoPortfolioConverter.convertListBack(accounts) } returns listOf(accountsDTO)

        // Act
        val actual = factory.create(userWalletId, userTokensResponse)

        // Assert
        val expected = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = userTokensResponse.group,
                sort = userTokensResponse.sort,
                totalAccounts = 1,
                totalArchivedAccounts = 0,
            ),
            accounts = listOf(accountsDTO.copy(tokens = userTokensResponse.tokens)),
            unassignedTokens = emptyList(),
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }
}