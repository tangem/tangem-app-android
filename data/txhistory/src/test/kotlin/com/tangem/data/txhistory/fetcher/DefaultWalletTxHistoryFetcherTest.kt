package com.tangem.data.txhistory.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.test.mock.MockAccounts
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val expressFetcherFactory: DefaultExpressTxHistoryFetcher.Factory = mockk()
    private val expressFetcher: DefaultExpressTxHistoryFetcher = mockk(relaxed = true)

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    @BeforeEach
    fun setup() {
        clearMocks(singleAccountListSupplier, accountFetcherFactory, expressFetcherFactory, expressFetcher)
        every { expressFetcherFactory.create(WALLET_ID) } returns expressFetcher
    }

    @Test
    fun `creates a single express fetcher for the whole wallet`() = runTest {
        // Act
        val fetcher = createFetcher(createUtils())
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.expressFetcher).isEqualTo(expressFetcher)
        verify(exactly = 1) { expressFetcherFactory.create(WALLET_ID) }
    }

    @Test
    fun `routes token details trigger to the express fetcher`() = runTest {
        val fetcher = createFetcher(createUtils())
        advanceUntilIdle()

        // Act
        val trigger = TxHistoryFetchTrigger.TokenDetailsPTR(walletId = WALLET_ID, currency = currency)
        fetcher.invoke(trigger)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { expressFetcher.invoke(trigger) }
    }

    @Test
    fun `routes wallet selected trigger to the express fetcher`() = runTest {
        val fetcher = createFetcher(createUtils())
        advanceUntilIdle()

        // Act
        val trigger = TxHistoryFetchTrigger.WalletSelected(walletId = WALLET_ID)
        fetcher.invoke(trigger)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { expressFetcher.invoke(trigger) }
    }

    @Test
    fun `close cancels scope and closes the express fetcher`() = runTest {
        val utils = createUtils()
        val fetcher = createFetcher(utils)
        advanceUntilIdle()

        // Act
        fetcher.close()

        // Assert
        verify(exactly = 1) { expressFetcher.close() }
        assertThat(utils.fetcherScope.coroutineContext.job.isActive).isFalse()
    }

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
        expressTxHistoryFetcher = expressFetcherFactory,
    )

    private companion object {
        val WALLET_ID: UserWalletId = MockAccounts.userWalletId
    }
}