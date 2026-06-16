package com.tangem.data.account.utils

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.account.converter.CryptoPortfolioConverter
import com.tangem.data.account.converter.createWalletAccountDTO
import com.tangem.data.account.utils.GetWalletAccountsResponseExtTest.Companion.createUserToken
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val networkFactory = mockk<NetworkFactory>()
    private val featureTogglesManager = mockk<FeatureTogglesManager>()

    private val factory = DefaultWalletAccountsResponseFactory(
        userWalletsListRepository = userWalletsListRepository,
        cryptoPortfolioCF = cryptoPortfolioCF,
        userTokensResponseFactory = userTokensResponseFactory,
        networkFactory = networkFactory,
        featureTogglesManager = featureTogglesManager,
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

        val userWalletsFlow = MutableStateFlow<List<UserWallet>?>(null)

        every { userWalletsListRepository.userWallets } returns userWalletsFlow
        every {
            userTokensResponseFactory.createDefaultResponse(
                userWallet = null,
                networkFactory = networkFactory,
                accountId = null,
                extraBlockchains = emptyList(),
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
            userWalletsListRepository.userWallets
            userTokensResponseFactory.createDefaultResponse(
                userWallet = null,
                networkFactory = networkFactory,
                accountId = null,
                extraBlockchains = emptyList(),
            )
        }
    }

    @Test
    fun `create returns response with default tokens when userTokensResponse is null`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet>(relaxed = true) {
            every { walletId } returns userWalletId
        }

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

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
                extraBlockchains = emptyList(),
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
            userWalletsListRepository.userWallets
            cryptoPortfolioConverter.convertListBack(accounts)
            userTokensResponseFactory.createDefaultResponse(
                userWallet = userWallet,
                networkFactory = networkFactory,
                accountId = accounts.first().accountId,
                extraBlockchains = emptyList(),
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

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

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
                extraBlockchains = emptyList(),
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
    fun `create passes ADI as extra blockchain when batch is BB000053 and toggle is on`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet.Cold>(relaxed = true) {
            every { walletId } returns userWalletId
            every { scanResponse.card.batchId } returns "BB000053"
        }

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15402_ADI_MAIN_SCREEN_DEFAULT_ENABLED)
        } returns true

        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()
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
                extraBlockchains = listOf(Blockchain.Adi),
            )
        } returns defaultResponse
        every { cryptoPortfolioConverter.convertListBack(accounts) } returns emptyList()

        // Act
        factory.create(userWalletId, null)

        // Assert
        coVerifyOrder {
            userTokensResponseFactory.createDefaultResponse(
                userWallet = userWallet,
                networkFactory = networkFactory,
                accountId = accounts.first().accountId,
                extraBlockchains = listOf(Blockchain.Adi),
            )
        }
    }

    @Test
    fun `create passes no extra blockchains when batch is BB000053 but toggle is off`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet.Cold>(relaxed = true) {
            every { walletId } returns userWalletId
            every { scanResponse.card.batchId } returns "BB000053"
        }

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15402_ADI_MAIN_SCREEN_DEFAULT_ENABLED)
        } returns false

        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()
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
                extraBlockchains = emptyList(),
            )
        } returns defaultResponse
        every { cryptoPortfolioConverter.convertListBack(accounts) } returns emptyList()

        // Act
        factory.create(userWalletId, null)

        // Assert
        coVerifyOrder {
            userTokensResponseFactory.createDefaultResponse(
                userWallet = userWallet,
                networkFactory = networkFactory,
                accountId = accounts.first().accountId,
                extraBlockchains = emptyList(),
            )
        }
    }

    @Test
    fun `create passes no extra blockchains when batch is not BB000053 even if toggle is on`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet.Cold>(relaxed = true) {
            every { walletId } returns userWalletId
            every { scanResponse.card.batchId } returns "AC000001"
        }

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every {
            featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15402_ADI_MAIN_SCREEN_DEFAULT_ENABLED)
        } returns true

        val accounts = AccountList.empty(userWallet.walletId).accounts
            .filterIsInstance<Account.CryptoPortfolio>()
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
                extraBlockchains = emptyList(),
            )
        } returns defaultResponse
        every { cryptoPortfolioConverter.convertListBack(accounts) } returns emptyList()

        // Act
        factory.create(userWalletId, null)

        // Assert
        coVerifyOrder {
            userTokensResponseFactory.createDefaultResponse(
                userWallet = userWallet,
                networkFactory = networkFactory,
                accountId = accounts.first().accountId,
                extraBlockchains = emptyList(),
            )
        }
    }

    @Test
    fun `create returns response with assigned tokens`() = runTest {
        // Arrange
        val userWallet = mockk<UserWallet>(relaxed = true) {
            every { walletId } returns userWalletId
        }
        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

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