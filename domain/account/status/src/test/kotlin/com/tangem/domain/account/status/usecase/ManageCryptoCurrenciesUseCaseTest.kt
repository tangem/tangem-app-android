package com.tangem.domain.account.status.usecase

import com.google.common.truth.Truth
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.status.utils.CryptoCurrencyMetadataCleaner
import com.tangem.domain.account.status.utils.createStatus
import com.tangem.domain.account.status.utils.createUngrouped
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

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
        )
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

    private fun createAccount(currencies: List<CryptoCurrency>): Account.CryptoPortfolio {
        return Account.CryptoPortfolio(
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
        account: Account.CryptoPortfolio,
        currencies: List<CryptoCurrency>,
    ): com.tangem.domain.account.models.AccountStatusList {
        val statuses = currencies.map { createStatus(currency = it, fiatAmount = BigDecimal.ONE) }

        return com.tangem.domain.account.models.AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.CryptoPortfolio(
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