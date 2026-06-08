package com.tangem.data.txhistory.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
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
internal class DefaultWalletTxHistoryFetcherTest {

    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val accountFetcherFactory: DefaultAccountTxHistoryFetcher.Factory = mockk()

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val mainAccount = Account.CryptoPortfolio.createMainAccount(
        userWalletId = WALLET_ID,
        cryptoCurrencies = listOf(currency),
    )
    private val secondAccount = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET_ID)

    @BeforeEach
    fun setup() {
        clearMocks(singleAccountListSupplier, accountFetcherFactory)
    }

    @Test
    fun `creates account fetcher for each account in the wallet`() = runTest {
        val utils = createUtils()
        val accountListFlow = MutableStateFlow(accountListOf(mainAccount, secondAccount))
        every { singleAccountListSupplier.invoke(WALLET_ID) } returns accountListFlow
        val mainFetcher = relaxedAccountFetcher()
        val secondFetcher = relaxedAccountFetcher()
        every { accountFetcherFactory.create(mainAccount.accountId) } returns mainFetcher
        every { accountFetcherFactory.create(secondAccount.accountId) } returns secondFetcher

        // Act
        val fetcher = createFetcher(utils)
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.fetchers.keys).containsExactly(mainAccount.accountId, secondAccount.accountId)
        verify(exactly = 1) { accountFetcherFactory.create(mainAccount.accountId) }
        verify(exactly = 1) { accountFetcherFactory.create(secondAccount.accountId) }
    }

    @Test
    fun `closes and removes fetcher when account is removed`() = runTest {
        val utils = createUtils()
        val accountListFlow = MutableStateFlow(accountListOf(mainAccount, secondAccount))
        every { singleAccountListSupplier.invoke(WALLET_ID) } returns accountListFlow
        val mainFetcher = relaxedAccountFetcher()
        val secondFetcher = relaxedAccountFetcher()
        every { accountFetcherFactory.create(mainAccount.accountId) } returns mainFetcher
        every { accountFetcherFactory.create(secondAccount.accountId) } returns secondFetcher

        val fetcher = createFetcher(utils)
        advanceUntilIdle()
        assertThat(fetcher.fetchers.keys).containsExactly(mainAccount.accountId, secondAccount.accountId)

        // Act
        accountListFlow.value = accountListOf(mainAccount)
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.fetchers.keys).containsExactly(mainAccount.accountId)
        verify(exactly = 1) { secondFetcher.close() }
        verify(inverse = true) { mainFetcher.close() }
    }

    @Test
    fun `routes trigger to the fetcher of the account that holds the currency`() = runTest {
        val utils = createUtils()
        val accountListFlow = MutableStateFlow(accountListOf(mainAccount))
        every { singleAccountListSupplier.invoke(WALLET_ID) } returns accountListFlow
        val mainFetcher = relaxedAccountFetcher()
        every { accountFetcherFactory.create(mainAccount.accountId) } returns mainFetcher

        val fetcher = createFetcher(utils)
        advanceUntilIdle()

        // Act
        val trigger = TxHistoryFetchTrigger.TokenDetailsPTR(walletId = WALLET_ID, currency = currency)
        fetcher.invoke(trigger)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mainFetcher.invoke(trigger) }
    }

    @Test
    fun `does nothing when trigger currency is not present in any account`() = runTest {
        val utils = createUtils()
        // main account without the triggered currency
        val accountListFlow = MutableStateFlow(accountListOf(Account.CryptoPortfolio.createMainAccount(WALLET_ID)))
        every { singleAccountListSupplier.invoke(WALLET_ID) } returns accountListFlow
        val mainFetcher = relaxedAccountFetcher()
        every { accountFetcherFactory.create(any()) } returns mainFetcher

        val fetcher = createFetcher(utils)
        advanceUntilIdle()

        // Act
        val trigger = TxHistoryFetchTrigger.TokenDetailsOpen(walletId = WALLET_ID, currency = currency)
        val result = fetcher.invoke(trigger)
        advanceUntilIdle()

        // Assert
        coVerify(inverse = true) { mainFetcher.invoke(any()) }
    }

    @Test
    fun `close cancels scope and closes all child fetchers`() = runTest {
        val utils = createUtils()
        val accountListFlow = MutableStateFlow(accountListOf(mainAccount, secondAccount))
        every { singleAccountListSupplier.invoke(WALLET_ID) } returns accountListFlow
        val mainFetcher = relaxedAccountFetcher()
        val secondFetcher = relaxedAccountFetcher()
        every { accountFetcherFactory.create(mainAccount.accountId) } returns mainFetcher
        every { accountFetcherFactory.create(secondAccount.accountId) } returns secondFetcher

        val fetcher = createFetcher(utils)
        advanceUntilIdle()
        assertThat(fetcher.fetchers.keys).containsExactly(mainAccount.accountId, secondAccount.accountId)

        // Act
        fetcher.close()

        // Assert
        assertThat(fetcher.fetchers).isEmpty()
        verify(exactly = 1) { mainFetcher.close() }
        verify(exactly = 1) { secondFetcher.close() }
        assertThat(utils.fetcherScope.coroutineContext.job.isActive).isFalse()
    }

    private fun accountListOf(vararg accounts: Account): AccountList = AccountList(
        userWalletId = WALLET_ID,
        accounts = accounts.toList(),
        totalAccounts = accounts.size,
        totalArchivedAccounts = 0,
    ).getOrNull()!!

    private fun TestScope.createUtils(): DefaultTxHistoryFetcherUtils = DefaultTxHistoryFetcherUtils(
        appScope = TestAppCoroutineScope(testScope = this),
        analyticsEventHandler = mockk(relaxed = true),
        analyticsExceptionHandler = mockk(relaxed = true),
    )

    private fun createFetcher(utils: DefaultTxHistoryFetcherUtils) = DefaultWalletTxHistoryFetcher(
        walletId = WALLET_ID,
        utils = utils,
        singleAccountListSupplier = singleAccountListSupplier,
        accountTxHistoryFetcher = accountFetcherFactory,
    )

    private fun relaxedAccountFetcher() = mockk<DefaultAccountTxHistoryFetcher>(relaxed = true)

    private companion object {
        val WALLET_ID: UserWalletId = MockAccounts.userWalletId
    }
}