package com.tangem.domain.account.status.usecase

import arrow.core.some
import com.google.common.truth.Truth
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.status.utils.CryptoCurrencyMetadataCleaner
import com.tangem.domain.account.status.utils.createStatus
import com.tangem.domain.account.status.utils.createUngrouped
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.jointaccount.derivation.toNetworkDerivationPath
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManageCryptoCurrenciesUseCaseTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val accountsCRUDRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val currenciesRepository: CurrenciesRepository = mockk(relaxUnitFun = true)
    private val derivationsRepository: DerivationsRepository = mockk(relaxUnitFun = true)
    private val walletManagersFacade: WalletManagersFacade = mockk(relaxed = true)
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher = mockk(relaxed = true)
    private val cryptoCurrencyMetadataCleaner: CryptoCurrencyMetadataCleaner = mockk(relaxed = true)
    private val expressServiceFetcher: ExpressServiceFetcher = mockk(relaxed = true)

    private val useCase = ManageCryptoCurrenciesUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        accountsCRUDRepository = accountsCRUDRepository,
        currenciesRepository = currenciesRepository,
        derivationsRepository = derivationsRepository,
        walletManagersFacade = walletManagersFacade,
        cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
        cryptoCurrencyMetadataCleaner = cryptoCurrencyMetadataCleaner,
        expressServiceFetcher = expressServiceFetcher,
        parallelUpdatingScope = TestAppCoroutineScope(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            singleAccountStatusListSupplier,
            accountsCRUDRepository,
            currenciesRepository,
            derivationsRepository,
            walletManagersFacade,
            cryptoCurrencyBalanceFetcher,
        )
        coEvery { accountsCRUDRepository.syncTokensOrRefresh(any()) } returns false
    }

    /**
     * Another device changed the list first: the sync is refused, the account is replaced by the backend's one and
     * the user's edit is made on it once more.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StaleList {

        private val coin = createCoin(derivationPath = Network.DerivationPath.Card(ACCOUNT_DERIVATION_PATH))
        private val own = createToken(derivationPath = Network.DerivationPath.Card(ACCOUNT_DERIVATION_PATH))
        private val peer = own.copy(
            id = own.id.copy(suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = "0xpeer")),
            contractAddress = "0xpeer",
        )

        @ParameterizedTest
        @ProvideTestModels
        fun invokeAndAwait(model: StaleModel) = runTest {
            // Arrange
            val account = createAccount(currencies = model.local)
            coEvery {
                singleAccountStatusListSupplier.getSyncOrNull(SingleAccountStatusListProducer.Params(userWalletId))
            } returns createAccountStatusList(account = account, currencies = model.local)
            coEvery { accountsCRUDRepository.syncTokensOrRefresh(userWalletId) } returnsMany listOf(true, false)
            coEvery { accountsCRUDRepository.getAccountSync(account.accountId) } returns
                account.copy(cryptoCurrencies = model.refreshed).some()

            // Act
            useCase.invokeAndAwait(accountId = account.accountId, add = model.add, remove = model.remove)

            // Assert
            coVerifyOrder {
                accountsCRUDRepository.syncTokensOrRefresh(userWalletId)
                accountsCRUDRepository.saveAccount(account.copy(cryptoCurrencies = model.expected))
                accountsCRUDRepository.syncTokensOrRefresh(userWalletId)
            }
        }

        private fun provideTestModels() = listOf(
            StaleModel(
                name = "own addition lands next to a token the peer added",
                local = listOf(coin),
                add = listOf(own),
                refreshed = listOf(coin, peer),
                expected = listOf(coin, peer, own),
            ),
            StaleModel(
                name = "own removal survives a token the peer added",
                local = listOf(coin, own),
                remove = listOf(own),
                refreshed = listOf(coin, own, peer),
                expected = listOf(coin, peer),
            ),
            StaleModel(
                name = "a token the peer removed is not brought back",
                local = listOf(coin, peer),
                add = listOf(own),
                refreshed = listOf(coin),
                expected = listOf(coin, own),
            ),
        )

        @Test
        fun `GIVEN the peer made the same edit WHEN the list is refreshed THEN nothing is saved or synced again`() =
            runTest {
                // Arrange
                val account = createAccount(currencies = listOf(coin))
                coEvery {
                    singleAccountStatusListSupplier.getSyncOrNull(SingleAccountStatusListProducer.Params(userWalletId))
                } returns createAccountStatusList(account = account, currencies = listOf(coin))
                coEvery { accountsCRUDRepository.syncTokensOrRefresh(userWalletId) } returns true
                coEvery { accountsCRUDRepository.getAccountSync(account.accountId) } returns
                    account.copy(cryptoCurrencies = listOf(coin, own)).some()

                // Act
                useCase.invokeAndAwait(accountId = account.accountId, add = listOf(own))

                // Assert
                coVerify(exactly = 1) { accountsCRUDRepository.saveAccount(any()) }
                coVerify(exactly = 1) { accountsCRUDRepository.syncTokensOrRefresh(userWalletId) }
            }

        @Test
        fun `GIVEN the list is current WHEN edit THEN it is synced once and not repeated`() = runTest {
            // Arrange
            val account = createAccount(currencies = listOf(coin))
            coEvery {
                singleAccountStatusListSupplier.getSyncOrNull(SingleAccountStatusListProducer.Params(userWalletId))
            } returns createAccountStatusList(account = account, currencies = listOf(coin))

            // Act
            useCase.invokeAndAwait(accountId = account.accountId, add = listOf(own))

            // Assert
            coVerify(exactly = 1) { accountsCRUDRepository.syncTokensOrRefresh(userWalletId) }
            coVerify(exactly = 0) { accountsCRUDRepository.getAccountSync(any()) }
        }
    }

    data class StaleModel(
        val name: String,
        val local: List<CryptoCurrency>,
        val add: List<CryptoCurrency> = emptyList(),
        val remove: List<CryptoCurrency> = emptyList(),
        val refreshed: List<CryptoCurrency>,
        val expected: List<CryptoCurrency>,
    ) {
        override fun toString(): String = name
    }

    @Test
    fun `GIVEN account coin has card derivation WHEN add token with the same custom derivation THEN coin is not duplicated`() =
        runTest {
            // Arrange
            val coin = createCoin(derivationPath = Network.DerivationPath.Card(ACCOUNT_DERIVATION_PATH))
            val account = createAccount(currencies = listOf(coin))

            coEvery {
                singleAccountStatusListSupplier.getSyncOrNull(SingleAccountStatusListProducer.Params(userWalletId))
            } returns createAccountStatusList(account = account, currencies = listOf(coin))

            // the derivation path typed in by the user always arrives as a custom one
            val addedToken = createToken(derivationPath = Network.DerivationPath.Custom(ACCOUNT_DERIVATION_PATH))

            // Act
            val actual = useCase(accountId = account.accountId, add = addedToken, skipDerivationErrors = false)

            // Assert
            Truth.assertThat(actual.isRight()).isTrue()

            coVerify(exactly = 1) {
                accountsCRUDRepository.saveAccount(account.copy(cryptoCurrencies = listOf(coin, addedToken)))
            }
            coVerify(inverse = true) { currenciesRepository.createCoinCurrency(any()) }
        }

    @Test
    fun `GIVEN account without coin WHEN add token THEN coin is created`() = runTest {
        // Arrange
        val account = createAccount(currencies = emptyList())

        coEvery {
            singleAccountStatusListSupplier.getSyncOrNull(SingleAccountStatusListProducer.Params(userWalletId))
        } returns createAccountStatusList(account = account, currencies = emptyList())

        val addedToken = createToken(derivationPath = Network.DerivationPath.Custom(ACCOUNT_DERIVATION_PATH))
        val createdCoin = createCoin(derivationPath = Network.DerivationPath.Custom(ACCOUNT_DERIVATION_PATH))

        coEvery { currenciesRepository.createCoinCurrency(addedToken.network) } returns createdCoin

        // Act
        val actual = useCase(accountId = account.accountId, add = addedToken, skipDerivationErrors = false)

        // Assert
        Truth.assertThat(actual.isRight()).isTrue()

        coVerify(exactly = 1) {
            accountsCRUDRepository.saveAccount(account.copy(cryptoCurrencies = listOf(createdCoin, addedToken)))
        }
    }

    @Test
    fun `GIVEN joint account WHEN add token on the owner path THEN saved without deriving keys or wallet managers`() =
        runTest {
            // Arrange
            val ownerPath = OwnerKeyIndex(value = 0).getOrNull()!!.toNetworkDerivationPath()
            val account = MockAccounts.createJointAccount(ownerKeyIndex = 0, userWalletId = userWalletId)
            coEvery {
                singleAccountStatusListSupplier.getSyncOrNull(SingleAccountStatusListProducer.Params(userWalletId))
            } returns createJointAccountStatusList(account = account)
            val addedToken = createToken(derivationPath = ownerPath)
            val createdCoin = createCoin(derivationPath = ownerPath)
            coEvery { currenciesRepository.createCoinCurrency(addedToken.network) } returns createdCoin

            // Act
            val actual = useCase(accountId = account.accountId, add = addedToken, skipDerivationErrors = false)

            // Assert
            Truth.assertThat(actual.isRight()).isTrue()
            coVerify(exactly = 1) {
                accountsCRUDRepository.saveAccount(account.copySealed(cryptoCurrencies = listOf(createdCoin, addedToken)))
            }
            coVerify(inverse = true) { derivationsRepository.derivePublicKeys(any(), match<List<CryptoCurrency>> { it.isNotEmpty() }) }
            coVerify(inverse = true) { walletManagersFacade.getOrCreateWalletManager(any(), any<Network>()) }
            coVerify(exactly = 1) { cryptoCurrencyBalanceFetcher(userWalletId, listOf(createdCoin, addedToken)) }
        }

    @Test
    fun `GIVEN joint account waiting for members WHEN add token THEN refused and nothing is saved`() = runTest {
        // Arrange
        val ownerPath = OwnerKeyIndex(value = 0).getOrNull()!!.toNetworkDerivationPath()
        val account = MockAccounts.createJointAccount(ownerKeyIndex = 0, userWalletId = userWalletId)
        coEvery {
            singleAccountStatusListSupplier.getSyncOrNull(SingleAccountStatusListProducer.Params(userWalletId))
        } returns createJointAccountStatusList(
            account = account,
            value = JointAccountStatusValue.Pending(
                source = StatusSource.ACTUAL,
                joinedMembers = 1,
                totalMembers = 2,
                threshold = 2,
            ),
        )

        // Act
        val actual = useCase(accountId = account.accountId, add = createToken(derivationPath = ownerPath))

        // Assert
        Truth.assertThat(actual.isLeft()).isTrue()
        coVerify(inverse = true) { accountsCRUDRepository.saveAccount(any()) }
    }

    private fun createJointAccountStatusList(
        account: Account.Joint,
        value: JointAccountStatusValue = JointAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            totalMembers = 2,
            safeAddress = "0x5afe000000000000000000000000000000000001",
            threshold = 2,
        ),
    ): com.tangem.domain.account.models.AccountStatusList {
        return com.tangem.domain.account.models.AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Joint(
                    account = account,
                    value = value,
                    tokenList = createUngrouped(statuses = emptyList()),
                    priceChangeLce = com.tangem.domain.core.utils.lceLoading(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = com.tangem.domain.models.TokensSortType.NONE,
            groupType = com.tangem.domain.models.TokensGroupType.NONE,
        )
    }

    private fun createAccount(currencies: List<CryptoCurrency>): Account.Personal {
        return Account.Personal(
            accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = derivationIndex),
            accountName = AccountName("Account 1").getOrNull()!!,
            icon = CryptoPortfolioIcon.ofCustomAccount(
                value = CryptoPortfolioIcon.Icon.Star,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            derivationIndex = derivationIndex,
            cryptoCurrencies = currencies,
        )
    }

    private fun createAccountStatusList(
        account: Account.Personal,
        currencies: List<CryptoCurrency>,
    ): com.tangem.domain.account.models.AccountStatusList {
        val statuses = currencies.map { createStatus(currency = it, fiatAmount = BigDecimal.ONE) }

        return com.tangem.domain.account.models.AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Personal(
                    account = account,
                    tokenList = createUngrouped(statuses = statuses),
                    priceChangeLce = com.tangem.domain.core.utils.lceLoading(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = com.tangem.domain.models.TokensSortType.NONE,
            groupType = com.tangem.domain.models.TokensGroupType.NONE,
        )
    }

    private fun createNetwork(derivationPath: Network.DerivationPath): Network {
        return Network(
            id = Network.ID(value = NETWORK_ID, derivationPath = derivationPath),
            name = "Ethereum",
            isTestnet = false,
            derivationPath = derivationPath,
            currencySymbol = "ETH",
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private fun createCoin(derivationPath: Network.DerivationPath): CryptoCurrency.Coin {
        val network = createNetwork(derivationPath)

        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = NETWORK_ID,
                    derivationPath = ACCOUNT_DERIVATION_PATH,
                ),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawId = NETWORK_ID),
            ),
            network = network,
            name = "Ethereum",
            symbol = "ETH",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
        )
    }

    private fun createToken(derivationPath: Network.DerivationPath): CryptoCurrency.Token {
        val network = createNetwork(derivationPath)

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = NETWORK_ID,
                    derivationPath = ACCOUNT_DERIVATION_PATH,
                ),
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = CONTRACT_ADDRESS),
            ),
            network = network,
            name = "Custom token",
            symbol = "CST",
            decimals = 6,
            iconUrl = null,
            contractAddress = CONTRACT_ADDRESS,
            isCustom = true,
        )
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val derivationIndex = DerivationIndex(value = 1).getOrNull()!!

        const val NETWORK_ID = "ethereum"
        const val ACCOUNT_DERIVATION_PATH = "m/44'/60'/0'/0/1"
        const val CONTRACT_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
    }
}