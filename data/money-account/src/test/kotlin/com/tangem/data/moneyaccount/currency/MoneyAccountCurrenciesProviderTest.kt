package com.tangem.data.moneyaccount.currency

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class MoneyAccountCurrenciesProviderTest {

    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk()
    private val getMoneyAccountCryptoCurrencyStatusUseCase: GetMoneyAccountCryptoCurrencyStatusUseCase = mockk()
    private val currenciesRepository: CurrenciesRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier = mockk()
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher = mockk()

    private val sut = MoneyAccountCurrenciesProvider(
        moneyAccountStatusSupplier = moneyAccountStatusSupplier,
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
        getMoneyAccountCryptoCurrencyStatusUseCase = getMoneyAccountCryptoCurrencyStatusUseCase,
        currenciesRepository = currenciesRepository,
        walletManagersFacade = walletManagersFacade,
        singleQuoteStatusSupplier = singleQuoteStatusSupplier,
        singleQuoteStatusFetcher = singleQuoteStatusFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            moneyAccountStatusSupplier,
            moneyAccountStatusFetcher,
            getMoneyAccountCryptoCurrencyStatusUseCase,
            currenciesRepository,
            walletManagersFacade,
            singleQuoteStatusSupplier,
            singleQuoteStatusFetcher,
        )
        every { currenciesRepository.createCoinCurrency(NETWORK) } returns COIN
        coEvery {
            walletManagersFacade.getNativeTokenBalance(
                userWalletId = USER_WALLET_ID,
                networkId = NETWORK.rawId,
                derivationPath = NETWORK.derivationPath.value,
            )
        } returns COIN_AMOUNT
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(QUOTE_PARAMS) } returns COIN_QUOTE
        coEvery { singleQuoteStatusFetcher(any()) } returns Unit.right()
        coEvery { moneyAccountStatusFetcher(MoneyAccountStatusFetcher.Params(USER_WALLET_ID)) } returns Unit.right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetStatuses {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account with a balance WHEN getStatuses THEN coin at the account address and account token`(
            account: MoneyAccountStatusValue.Account,
        ) = runTest {
            // Arrange
            givenAccountStatus(account)

            // Act
            val actual = sut.getStatuses(USER_WALLET_ID, NETWORK)

            // Assert
            assertThat(actual).containsExactly(
                account.statusOf(currency = COIN, amount = COIN_AMOUNT, fiatRate = COIN_RATE),
                account.cryptoCurrencyStatus,
            ).inOrder()
            coVerify(exactly = 0) { singleQuoteStatusFetcher(any()) }
        }

        @Test
        fun `GIVEN coin quote not loaded yet WHEN getStatuses THEN the quote is fetched for the coin status`() =
            runTest {
                // Arrange
                val account = activeAccount()
                givenAccountStatus(account)
                coEvery { singleQuoteStatusSupplier.getSyncOrNull(QUOTE_PARAMS) } returnsMany listOf(
                    QuoteStatus(rawCurrencyId = COIN_RAW_ID),
                    COIN_QUOTE,
                )

                // Act
                val actual = sut.getStatuses(USER_WALLET_ID, NETWORK)

                // Assert
                assertThat(actual.first()).isEqualTo(
                    account.statusOf(currency = COIN, amount = COIN_AMOUNT, fiatRate = COIN_RATE),
                )
                coVerify(exactly = 1) {
                    singleQuoteStatusFetcher(
                        SingleQuoteStatusFetcher.Params(rawCurrencyId = COIN_RAW_ID, appCurrencyId = null),
                    )
                }
            }

        @Test
        fun `GIVEN coin quote unavailable WHEN getStatuses THEN coin status has no rate`() = runTest {
            // Arrange
            val account = activeAccount()
            givenAccountStatus(account)
            coEvery { singleQuoteStatusSupplier.getSyncOrNull(QUOTE_PARAMS) } returns
                QuoteStatus(rawCurrencyId = COIN_RAW_ID)
            coEvery { singleQuoteStatusFetcher(any()) } returns IllegalStateException("quotes are down").left()

            // Act
            val actual = sut.getStatuses(USER_WALLET_ID, NETWORK)

            // Assert
            assertThat(actual.first()).isEqualTo(
                account.statusOf(currency = COIN, amount = COIN_AMOUNT, fiatRate = null),
            )
        }

        private fun provideTestModels(): List<MoneyAccountStatusValue.Account> = listOf(
            activeAccount(),
            MoneyAccountStatusValue.Account.Deactivated(
                source = StatusSource.ACTUAL,
                cryptoCurrency = TOKEN,
                balance = BALANCE,
                customerId = CUSTOMER_ID,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoAccountBalance {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no account with a balance on the network WHEN getStatuses THEN nothing to pay the fee with`(
            model: NoStatusesModel,
        ) = runTest {
            // Arrange
            givenAccountStatus(model.value)

            // Act
            val actual = sut.getStatuses(USER_WALLET_ID, model.network)

            // Assert
            assertThat(actual).isEmpty()
            coVerify(exactly = 0) { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) }
        }

        private fun provideTestModels(): List<NoStatusesModel> = listOf(
            NoStatusesModel(value = activeAccount(), network = ETHEREUM_NETWORK),
            NoStatusesModel(value = activeAccount(balance = null), network = NETWORK),
            NoStatusesModel(value = MoneyAccountStatusValue.NotCreated, network = NETWORK),
            NoStatusesModel(
                value = MoneyAccountStatusValue.Onboarding.Activating(
                    source = StatusSource.ACTUAL,
                    customerId = CUSTOMER_ID,
                ),
                network = NETWORK,
            ),
            NoStatusesModel(value = MoneyAccountStatusValue.Error.Unavailable, network = NETWORK),
        )
    }

    data class NoStatusesModel(val value: MoneyAccountStatusValue, val network: Network)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FindCurrency {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account status WHEN findCurrency THEN only the account token is found`(model: FindCurrencyModel) =
            runTest {
                // Arrange
                givenAccountStatus(model.value)

                // Act
                val actual = sut.findCurrency(USER_WALLET_ID, model.currencyId)

                // Assert
                assertThat(actual).isEqualTo(model.expected)
            }

        private fun provideTestModels(): List<FindCurrencyModel> = listOf(
            FindCurrencyModel(value = activeAccount(), currencyId = TOKEN.id, expected = TOKEN),
            FindCurrencyModel(value = activeAccount(), currencyId = COIN.id, expected = null),
            FindCurrencyModel(value = MoneyAccountStatusValue.NotCreated, currencyId = TOKEN.id, expected = null),
        )
    }

    data class FindCurrencyModel(
        val value: MoneyAccountStatusValue,
        val currencyId: CryptoCurrency.ID,
        val expected: CryptoCurrency?,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Refresh {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account status WHEN refresh THEN only its own network is refetched`(model: RefreshModel) = runTest {
            // Arrange
            givenAccountStatus(model.value)

            // Act
            sut.refresh(USER_WALLET_ID, model.network)

            // Assert
            coVerify(exactly = model.expectedFetches) {
                moneyAccountStatusFetcher(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))
            }
        }

        private fun provideTestModels(): List<RefreshModel> = listOf(
            RefreshModel(value = activeAccount(), network = NETWORK, expectedFetches = 1),
            RefreshModel(value = activeAccount(), network = ETHEREUM_NETWORK, expectedFetches = 0),
            RefreshModel(value = MoneyAccountStatusValue.NotCreated, network = NETWORK, expectedFetches = 0),
        )
    }

    data class RefreshModel(val value: MoneyAccountStatusValue, val network: Network, val expectedFetches: Int)

    private fun givenAccountStatus(value: MoneyAccountStatusValue) {
        every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns flowOf(
            AccountStatus.Money(account = Account.Money(userWalletId = USER_WALLET_ID), value = value),
        )
    }

    private fun activeAccount(balance: MoneyAccountStatusValue.Balance? = BALANCE) =
        MoneyAccountStatusValue.Account.Active(
            source = StatusSource.ACTUAL,
            cryptoCurrency = TOKEN,
            balance = balance,
            customerId = CUSTOMER_ID,
        )

    private companion object {
        const val CUSTOMER_ID = "customer"

        val USER_WALLET_ID = UserWalletId("011")
        val CURRENCY_FACTORY = MockCryptoCurrencyFactory()
        val TOKEN = CURRENCY_FACTORY.createToken(
            blockchain = Blockchain.Polygon,
            id = "usd-coin",
            contractAddress = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359",
            symbol = "USDC",
            decimals = 6,
        )
        val NETWORK = TOKEN.network
        val COIN = CURRENCY_FACTORY.createCoin(Blockchain.Polygon).copy(network = NETWORK)
        val COIN_RAW_ID = requireNotNull(COIN.id.rawCurrencyId)
        val ETHEREUM_NETWORK = CURRENCY_FACTORY.ethereum.network

        val COIN_AMOUNT = BigDecimal("0.5")
        val COIN_RATE = BigDecimal("0.2")
        val QUOTE_PARAMS = SingleQuoteStatusProducer.Params(rawCurrencyId = COIN_RAW_ID)
        val COIN_QUOTE = QuoteStatus(
            rawCurrencyId = COIN_RAW_ID,
            value = QuoteStatus.Data(
                source = StatusSource.ACTUAL,
                fiatRate = COIN_RATE,
                fiatRateUSD = COIN_RATE,
                priceChange = BigDecimal.ZERO,
            ),
        )
        val BALANCE = MoneyAccountStatusValue.Balance(
            cryptoAmount = BigDecimal("25"),
            depositAddress = "0xaccount",
            currencyCode = "USD",
            fiatRate = BigDecimal.ONE,
        )
    }
}