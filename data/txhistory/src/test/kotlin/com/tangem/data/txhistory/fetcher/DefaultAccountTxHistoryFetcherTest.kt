package com.tangem.data.txhistory.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.mock.MockAccounts
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultAccountTxHistoryFetcherTest {

    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val paymentAccountCurrency: GetPaymentAccountCryptoCurrencyStatusUseCase = mockk()
    private val expressFetcherFactory: DefaultExpressTxHistoryFetcher.Factory = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()

    private val coin: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val cryptoAccount = MockAccounts.createAccount(
        derivationIndex = 1,
        userWalletId = WALLET_ID,
        cryptoCurrencies = listOf(coin),
    )

    @BeforeEach
    fun setup() {
        clearMocks(singleAccountSupplier, paymentAccountCurrency, expressFetcherFactory, walletManagersFacade)
    }

    @Test
    fun `creates express fetcher per coin address of a crypto portfolio account`() = runTest {
        val utils = createUtils()
        val accountFlow = MutableStateFlow<Account>(cryptoAccount)
        every { singleAccountSupplier.invoke(cryptoAccount.accountId) } returns accountFlow
        coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, coin.network) } returns ADDRESS
        val expressFetcher = relaxedExpressFetcher()
        every { expressFetcherFactory.create(ADDRESS, cryptoAccount.accountId) } returns expressFetcher

        // Act
        val fetcher = createFetcher(cryptoAccount.accountId, utils)
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.expressFetchers.keys).containsExactly(ADDRESS)
        verify(exactly = 1) { expressFetcherFactory.create(ADDRESS, cryptoAccount.accountId) }
    }

    @Test
    fun `closes express fetcher when its coin is removed from the account`() = runTest {
        val utils = createUtils()
        val accountFlow = MutableStateFlow<Account>(cryptoAccount)
        every { singleAccountSupplier.invoke(cryptoAccount.accountId) } returns accountFlow
        coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, coin.network) } returns ADDRESS
        val expressFetcher = relaxedExpressFetcher()
        every { expressFetcherFactory.create(ADDRESS, cryptoAccount.accountId) } returns expressFetcher

        val fetcher = createFetcher(cryptoAccount.accountId, utils)
        advanceUntilIdle()
        assertThat(fetcher.expressFetchers.keys).containsExactly(ADDRESS)

        // Act
        accountFlow.value = cryptoAccount.copy(cryptoCurrencies = emptyList())
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.expressFetchers).isEmpty()
        verify(exactly = 1) { expressFetcher.close() }
    }

    @Test
    fun `routes trigger to the express fetcher of the currency address`() = runTest {
        val utils = createUtils()
        val accountFlow = MutableStateFlow<Account>(cryptoAccount)
        every { singleAccountSupplier.invoke(cryptoAccount.accountId) } returns accountFlow
        coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, coin.network) } returns ADDRESS
        val expressFetcher = relaxedExpressFetcher()
        every { expressFetcherFactory.create(ADDRESS, cryptoAccount.accountId) } returns expressFetcher

        val fetcher = createFetcher(cryptoAccount.accountId, utils)
        advanceUntilIdle()

        // Act
        val trigger = TxHistoryFetchTrigger.TokenDetailsOpen(walletId = WALLET_ID, currency = coin)
        fetcher.invoke(trigger)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { expressFetcher.invoke(trigger) }
    }

    @Test
    fun `close cancels scope and closes all express fetchers`() = runTest {
        val utils = createUtils()
        val accountFlow = MutableStateFlow<Account>(cryptoAccount)
        every { singleAccountSupplier.invoke(cryptoAccount.accountId) } returns accountFlow
        coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, coin.network) } returns ADDRESS
        val expressFetcher = relaxedExpressFetcher()
        every { expressFetcherFactory.create(ADDRESS, cryptoAccount.accountId) } returns expressFetcher

        val fetcher = createFetcher(cryptoAccount.accountId, utils)
        advanceUntilIdle()
        assertThat(fetcher.expressFetchers.keys).containsExactly(ADDRESS)

        // Act
        fetcher.close()

        // Assert
        assertThat(fetcher.expressFetchers).isEmpty()
        verify(exactly = 1) { expressFetcher.close() }
        assertThat(utils.fetcherScope.coroutineContext.job.isActive).isFalse()
    }

    private fun TestScope.createUtils(): DefaultTxHistoryFetcherUtils = DefaultTxHistoryFetcherUtils(
        appScope = TestAppCoroutineScope(testScope = this),
        analyticsEventHandler = mockk(relaxed = true),
        analyticsExceptionHandler = mockk(relaxed = true),
    )

    private fun createFetcher(accountId: AccountId, utils: DefaultTxHistoryFetcherUtils) =
        DefaultAccountTxHistoryFetcher(
            accountId = accountId,
            utils = utils,
            singleAccountSupplier = singleAccountSupplier,
            paymentAccountCurrency = paymentAccountCurrency,
            expressFetcherFactory = expressFetcherFactory,
            walletManagersFacade = walletManagersFacade,
        )

    private fun relaxedExpressFetcher() = mockk<DefaultExpressTxHistoryFetcher>(relaxed = true)

    private companion object {
        val WALLET_ID = MockAccounts.userWalletId
        const val ADDRESS = "0xEthAddress"
    }
}