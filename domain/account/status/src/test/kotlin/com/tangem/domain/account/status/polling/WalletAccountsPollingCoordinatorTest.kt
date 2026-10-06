package com.tangem.domain.account.status.polling

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class WalletAccountsPollingCoordinatorTest {

    private val fetcher: SingleAccountListFetcher = mockk()
    private val currenciesSupplier: MultiWalletCryptoCurrenciesSupplier = mockk()
    private val balanceFetcher: CryptoCurrencyBalanceFetcher = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(fetcher, currenciesSupplier, balanceFetcher)
        coEvery { fetcher(any()) } returns Unit.right()
        every { currenciesSupplier(any()) } returns flowOf(setOf(ethereum))
    }

    @Test
    fun `GIVEN a wallet WHEN subscribed THEN the first fetch waits a full interval`() = runTest {
        // Arrange
        val coordinator = createCoordinator(testScope = this)

        // Act
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect {} }
        runCurrent()

        // Assert
        coVerify(exactly = 0) { fetcher(any()) }
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()
        coVerify(exactly = 1) { fetcher(SingleAccountListFetcher.Params(userWalletId = WALLET_ID)) }
    }

    @Test
    fun `GIVEN a running ticker WHEN three intervals elapse THEN fetches three times`() = runTest {
        // Arrange
        val coordinator = createCoordinator(testScope = this)
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect {} }

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)
        runCurrent()

        // Assert
        coVerify(exactly = 3) { fetcher(any()) }
    }

    @Test
    fun `GIVEN the fetch adds a currency WHEN the tick runs THEN balance fetched only for the added one`() = runTest {
        // Arrange
        every { currenciesSupplier(any()) } returnsMany listOf(flowOf(setOf(ethereum)), flowOf(setOf(ethereum, cardano)))
        val coordinator = createCoordinator(testScope = this)
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect {} }

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Assert
        verify(exactly = 1) { balanceFetcher(WALLET_ID, listOf(cardano)) }
    }

    @Test
    fun `GIVEN the document did not change WHEN the tick runs THEN no balance is fetched`() = runTest {
        // Arrange
        val coordinator = createCoordinator(testScope = this)
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect {} }

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS * 2)
        runCurrent()

        // Assert
        verify(exactly = 0) { balanceFetcher(any(), any<List<CryptoCurrency>>()) }
    }

    @Test
    fun `GIVEN the supplier lags one tick WHEN the next tick sees the currency THEN its balance is fetched`() = runTest {
        // Arrange
        every { currenciesSupplier(any()) } returnsMany listOf(
            flowOf(setOf(ethereum)),
            flowOf(setOf(ethereum)),
            flowOf(setOf(ethereum, cardano)),
        )
        val coordinator = createCoordinator(testScope = this)
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect {} }

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS * 2)
        runCurrent()

        // Assert
        verify(exactly = 1) { balanceFetcher(WALLET_ID, listOf(cardano)) }
    }

    @Test
    fun `GIVEN the fetch fails WHEN the next interval elapses THEN the ticker keeps polling`() = runTest {
        // Arrange
        coEvery { fetcher(any()) } returns IllegalStateException("boom").left()
        val coordinator = createCoordinator(testScope = this)
        val received = mutableListOf<Unit>()
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { received += it } }

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS * 2)
        runCurrent()

        // Assert
        assertThat(received).hasSize(2)
        coVerify(exactly = 2) { fetcher(any()) }
        verify(exactly = 0) { balanceFetcher(any(), any<List<CryptoCurrency>>()) }
    }

    private fun createCoordinator(testScope: TestScope) = WalletAccountsPollingCoordinator(
        fetcher = fetcher,
        currenciesSupplier = currenciesSupplier,
        balanceFetcher = balanceFetcher,
        appScope = TestAppCoroutineScope(testScope.backgroundScope.coroutineContext),
        now = { testScope.testScheduler.currentTime },
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        const val POLL_INTERVAL_MILLIS = 10_000L
        val ethereum: CryptoCurrency by lazy { MockCryptoCurrencyFactory().ethereum }
        val cardano: CryptoCurrency by lazy { MockCryptoCurrencyFactory().cardano }
    }
}