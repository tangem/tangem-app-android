package com.tangem.data.account.utils

import com.google.common.truth.Truth
import com.tangem.data.account.converter.CryptoPortfolioConverter
import com.tangem.data.account.converter.createWalletAccountDTO
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
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

    private val userWalletsListRepository = mockk<UserWalletsListRepository>()
    private val cryptoPortfolioCF = mockk<CryptoPortfolioConverter.Factory>()
    private val cryptoPortfolioConverter = mockk<CryptoPortfolioConverter>()
    private val userTokensResponseFactory = mockk<UserTokensResponseFactory>()
    private val cardCryptoCurrencyFactory = mockk<CardCryptoCurrencyFactory>()

    private val factory = DefaultWalletAccountsResponseFactory(
        userWalletsListRepository = userWalletsListRepository,
        cryptoPortfolioCF = cryptoPortfolioCF,
        userTokensResponseFactory = userTokensResponseFactory,
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun setUpEach() {
        every { cryptoPortfolioCF.create(any()) } returns cryptoPortfolioConverter
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(
            userWalletsListRepository,
            cryptoPortfolioCF,
            cryptoPortfolioConverter,
            userTokensResponseFactory,
            cardCryptoCurrencyFactory,
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

        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        every {
            userTokensResponseFactory.createUserTokensResponse(
                currencies = emptyList(),
                isGroupedByNetwork = false,
                isSortedByBalance = false,
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
            ),
            accounts = emptyList(),
            unassignedTokens = emptyList(),
        )
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            userWalletsListRepository.userWalletsSync()
            userTokensResponseFactory.createUserTokensResponse(
                currencies = emptyList(),
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )
        }
    }

    @Test
    fun `create returns response with default tokens when userTokensResponse is null`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet>(relaxed = true) {
            every { walletId } returns userWalletId
        }

        val defaultCoins = listOf(mockk<CryptoCurrency.Coin>())
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
        every { cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(userWallet) } returns defaultCoins

        val defaultResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = listOf(mockk(relaxed = true)),
        )

        every {
            userTokensResponseFactory.createUserTokensResponse(
                currencies = defaultCoins,
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )
        } returns defaultResponse

        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()

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
            ),
            accounts = listOf(accountsDTO),
            unassignedTokens = emptyList(),
        )

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            userWalletsListRepository.userWalletsSync()
            cryptoPortfolioConverter.convertListBack(accounts)
            cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(userWallet)
            userTokensResponseFactory.createUserTokensResponse(
                currencies = defaultCoins,
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )
        }
    }

    @Test
    fun `create returns response with default tokens when userTokensResponse is null and no default coins`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet>(relaxed = true) {
            every { walletId } returns userWalletId
        }
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
        every { cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(userWallet) } returns emptyList()
        val defaultResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
        )
        every {
            userTokensResponseFactory.createUserTokensResponse(
                currencies = emptyList(),
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )
        } returns defaultResponse
        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()
        every { cryptoPortfolioConverter.convertListBack(accounts) } returns emptyList()

        // Act
        val actual = factory.create(userWalletId, null)

        // Assert
        val expected = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = defaultResponse.group,
                sort = defaultResponse.sort,
                totalAccounts = 0,
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
        val assignedTokens = listOf(mockk<CryptoCurrency.Token>(), mockk<CryptoCurrency.Token>())
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(userWallet)
        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = listOf(mockk(relaxed = true)),
        )
        every {
            userTokensResponseFactory.createUserTokensResponse(
                currencies = assignedTokens,
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )
        } returns userTokensResponse

        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()
        val accountsDTO = createWalletAccountDTO(userWalletId)
        every { cryptoPortfolioConverter.convertListBack(accounts) } returns listOf(accountsDTO)

        // Act
        val actual = factory.create(userWalletId, userTokensResponse)

        // Assert
        val expected = GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = userTokensResponse.group,
                sort = userTokensResponse.sort,
                totalAccounts = 1,
            ),
            accounts = listOf(accountsDTO),
            unassignedTokens = emptyList(),
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }
}