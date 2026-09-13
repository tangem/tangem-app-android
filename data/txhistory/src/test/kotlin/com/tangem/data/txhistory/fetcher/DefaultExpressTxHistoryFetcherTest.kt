package com.tangem.data.txhistory.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.txhistory.repository.DefaultExpressHistoryRepository
import com.tangem.grow.datasource.express.models.response.ExchangeHistoryDeltaResponse
import com.tangem.grow.datasource.express.models.response.ExchangeHistoryResponse
import com.tangem.grow.datasource.express.models.response.ExpressPagination
import com.tangem.grow.datasource.express.models.response.ExpressPaginationDelta
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryResponse
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.test.mock.MockAccounts
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultExpressTxHistoryFetcherTest {

    private val expressSyncStateDao: ExpressSyncStateDao = mockk()
    private val expressHistoryRepository: DefaultExpressHistoryRepository = mockk()

    private val coin: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    @BeforeEach
    fun setup() {
        clearMocks(expressSyncStateDao, expressHistoryRepository)
    }

    @Test
    fun `exposes the wallet it was created with`() = runTest {
        val fetcher = createFetcher(createUtils())

        assertThat(fetcher.walletId).isEqualTo(WALLET_ID)
    }

    @Test
    fun `on first trigger fetches initial exchange and onramp history for the wallet`() = runTest {
        stubAllSuccess(hasMore = false)
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.TokenDetailsOpen(walletId = WALLET_ID, currency = coin))
        advanceUntilIdle()

        // Assert
        coVerify(atLeast = 1) { expressHistoryRepository.fetchExchangeHistory(WALLET_ID, any()) }
        coVerify(atLeast = 1) { expressHistoryRepository.fetchOnrampHistory(WALLET_ID, any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchExchangeHistoryDelta(WALLET_ID, any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchOnrampHistoryDelta(WALLET_ID, any()) }
    }

    @Test
    fun `continues exchange initial pagination while hasMore is true`() = runTest {
        every { expressSyncStateDao.observe(any(), WALLET_ID.stringValue) } returns flowOf(null)
        // 1st call: initial fetch in fetchExchange (pagination ignored)
        // 2nd call: pagination loop, hasMore = true -> continue
        // 3rd call: pagination loop, hasMore = false -> stop
        coEvery { expressHistoryRepository.fetchExchangeHistory(WALLET_ID, any()) } returnsMany listOf(
            exchangeResponse(hasMore = true),
            exchangeResponse(hasMore = true),
            exchangeResponse(hasMore = false),
        )
        coEvery { expressHistoryRepository.fetchExchangeHistoryDelta(WALLET_ID, any()) } returns
            exchangeDeltaResponse(hasMore = false)
        coEvery { expressHistoryRepository.fetchOnrampHistory(WALLET_ID, any()) } returns onrampResponse(hasMore = false)
        coEvery { expressHistoryRepository.fetchOnrampHistoryDelta(WALLET_ID, any()) } returns
            onrampDeltaResponse(hasMore = false)
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.TokenDetailsOpen(walletId = WALLET_ID, currency = coin))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 3) { expressHistoryRepository.fetchExchangeHistory(WALLET_ID, any()) }
    }

    @Test
    fun `skips initial pagination when it is already completed`() = runTest {
        every { expressSyncStateDao.observe(any(), WALLET_ID.stringValue) } returns flowOf(completedSyncState())
        coEvery { expressHistoryRepository.fetchExchangeHistoryDelta(WALLET_ID, any()) } returns
            exchangeDeltaResponse(hasMore = false)
        coEvery { expressHistoryRepository.fetchOnrampHistoryDelta(WALLET_ID, any()) } returns
            onrampDeltaResponse(hasMore = false)
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.TokenDetailsPTR(walletId = WALLET_ID, currency = coin))
        advanceUntilIdle()

        // Assert: no initial history fetch, only the delta pagination runs
        coVerify(exactly = 0) { expressHistoryRepository.fetchExchangeHistory(any(), any()) }
        coVerify(exactly = 0) { expressHistoryRepository.fetchOnrampHistory(any(), any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchExchangeHistoryDelta(WALLET_ID, any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchOnrampHistoryDelta(WALLET_ID, any()) }
    }

    @Test
    fun `on wallet selected trigger fetches exchange and onramp history`() = runTest {
        stubAllSuccess(hasMore = false)
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.WalletSelected(walletId = WALLET_ID))
        advanceUntilIdle()

        // Assert
        coVerify(atLeast = 1) { expressHistoryRepository.fetchExchangeHistory(WALLET_ID, any()) }
        coVerify(atLeast = 1) { expressHistoryRepository.fetchOnrampHistory(WALLET_ID, any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchExchangeHistoryDelta(WALLET_ID, any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchOnrampHistoryDelta(WALLET_ID, any()) }
    }

    @Test
    fun `close cancels the fetcher scope`() = runTest {
        val utils = createUtils()
        val fetcher = createFetcher(utils)

        // Act
        fetcher.close()

        // Assert
        assertThat(utils.fetcherScope.coroutineContext.job.isActive).isFalse()
    }

    private fun stubAllSuccess(hasMore: Boolean) {
        every { expressSyncStateDao.observe(any(), WALLET_ID.stringValue) } returns flowOf(null)
        coEvery { expressHistoryRepository.fetchExchangeHistory(WALLET_ID, any()) } returns exchangeResponse(hasMore)
        coEvery { expressHistoryRepository.fetchExchangeHistoryDelta(WALLET_ID, any()) } returns
            exchangeDeltaResponse(hasMore)
        coEvery { expressHistoryRepository.fetchOnrampHistory(WALLET_ID, any()) } returns onrampResponse(hasMore)
        coEvery { expressHistoryRepository.fetchOnrampHistoryDelta(WALLET_ID, any()) } returns onrampDeltaResponse(hasMore)
    }

    private fun TestScope.createUtils(): DefaultTxHistoryFetcherUtils = DefaultTxHistoryFetcherUtils(
        appScope = TestAppCoroutineScope(testScope = this),
        analyticsEventHandler = mockk(relaxed = true),
        analyticsExceptionHandler = mockk(relaxed = true),
    )

    private fun createFetcher(utils: DefaultTxHistoryFetcherUtils) = DefaultExpressTxHistoryFetcher(
        walletId = WALLET_ID,
        utils = utils,
        expressSyncStateDao = expressSyncStateDao,
        expressHistoryRepository = expressHistoryRepository,
    )

    private fun exchangeResponse(hasMore: Boolean) =
        ExchangeHistoryResponse(items = emptyList(), pagination = pagination(hasMore))

    private fun exchangeDeltaResponse(hasMore: Boolean) =
        ExchangeHistoryDeltaResponse(items = emptyList(), pagination = paginationDelta(hasMore))

    private fun onrampResponse(hasMore: Boolean) =
        OnrampHistoryResponse(items = emptyList(), pagination = pagination(hasMore))

    private fun onrampDeltaResponse(hasMore: Boolean) =
        OnrampHistoryDeltaResponse(items = emptyList(), pagination = paginationDelta(hasMore))

    private fun pagination(hasMore: Boolean) =
        ExpressPagination(endCursor = null, startDeltaCursor = null, hasMore = hasMore)

    private fun paginationDelta(hasMore: Boolean) = ExpressPaginationDelta(startCursor = null, hasMore = hasMore)

    private fun completedSyncState() = ExpressSyncStateEntity(
        type = ExpressSyncStateEntity.Type.EXCHANGE.name,
        userWalletId = WALLET_ID.stringValue,
        isInitialCompleted = true,
        afterCursor = null,
        deltaCursor = null,
    )

    private companion object {
        val WALLET_ID = MockAccounts.userWalletId
    }
}