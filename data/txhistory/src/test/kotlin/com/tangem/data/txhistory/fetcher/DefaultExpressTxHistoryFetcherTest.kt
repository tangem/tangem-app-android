package com.tangem.data.txhistory.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.txhistory.repository.DefaultExpressHistoryRepository
import com.tangem.datasource.api.express.models.response.ExchangeHistoryDeltaResponse
import com.tangem.datasource.api.express.models.response.ExchangeHistoryResponse
import com.tangem.datasource.api.express.models.response.ExpressPagination
import com.tangem.datasource.api.express.models.response.ExpressPaginationDelta
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryResponse
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.domain.models.account.AccountId
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
    fun `exposes the address it was created with`() = runTest {
        val fetcher = createFetcher(createUtils())

        assertThat(fetcher.address).isEqualTo(ADDRESS)
    }

    @Test
    fun `on first trigger fetches initial exchange and onramp history for the address`() = runTest {
        stubAllSuccess(hasMore = false)
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.TokenDetailsOpen(walletId = WALLET_ID, currency = coin))
        advanceUntilIdle()

        // Assert
        coVerify(atLeast = 1) { expressHistoryRepository.fetchExchangeHistory(ADDRESS, any()) }
        coVerify(atLeast = 1) { expressHistoryRepository.fetchOnrampHistory(ADDRESS, any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchExchangeHistoryDelta(ADDRESS, any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchOnrampHistoryDelta(ADDRESS, any()) }
    }

    @Test
    fun `continues exchange initial pagination while hasMore is true`() = runTest {
        every { expressSyncStateDao.observe(any(), ADDRESS) } returns flowOf(null)
        // 1st call: initial fetch in fetchExchange (pagination ignored)
        // 2nd call: pagination loop, hasMore = true -> continue
        // 3rd call: pagination loop, hasMore = false -> stop
        coEvery { expressHistoryRepository.fetchExchangeHistory(ADDRESS, any()) } returnsMany listOf(
            exchangeResponse(hasMore = true),
            exchangeResponse(hasMore = true),
            exchangeResponse(hasMore = false),
        )
        coEvery { expressHistoryRepository.fetchExchangeHistoryDelta(ADDRESS, any()) } returns
            exchangeDeltaResponse(hasMore = false)
        coEvery { expressHistoryRepository.fetchOnrampHistory(ADDRESS, any()) } returns onrampResponse(hasMore = false)
        coEvery { expressHistoryRepository.fetchOnrampHistoryDelta(ADDRESS, any()) } returns
            onrampDeltaResponse(hasMore = false)
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.TokenDetailsOpen(walletId = WALLET_ID, currency = coin))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 3) { expressHistoryRepository.fetchExchangeHistory(ADDRESS, any()) }
    }

    @Test
    fun `skips initial pagination when it is already completed`() = runTest {
        every { expressSyncStateDao.observe(any(), ADDRESS) } returns flowOf(completedSyncState())
        coEvery { expressHistoryRepository.fetchExchangeHistoryDelta(ADDRESS, any()) } returns
            exchangeDeltaResponse(hasMore = false)
        coEvery { expressHistoryRepository.fetchOnrampHistoryDelta(ADDRESS, any()) } returns
            onrampDeltaResponse(hasMore = false)
        val fetcher = createFetcher(createUtils())

        // Act
        fetcher.invoke(TxHistoryFetchTrigger.TokenDetailsPTR(walletId = WALLET_ID, currency = coin))
        advanceUntilIdle()

        // Assert: no initial history fetch, only the delta pagination runs
        coVerify(exactly = 0) { expressHistoryRepository.fetchExchangeHistory(any(), any()) }
        coVerify(exactly = 0) { expressHistoryRepository.fetchOnrampHistory(any(), any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchExchangeHistoryDelta(ADDRESS, any()) }
        coVerify(exactly = 1) { expressHistoryRepository.fetchOnrampHistoryDelta(ADDRESS, any()) }
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
        every { expressSyncStateDao.observe(any(), ADDRESS) } returns flowOf(null)
        coEvery { expressHistoryRepository.fetchExchangeHistory(ADDRESS, any()) } returns exchangeResponse(hasMore)
        coEvery { expressHistoryRepository.fetchExchangeHistoryDelta(ADDRESS, any()) } returns
            exchangeDeltaResponse(hasMore)
        coEvery { expressHistoryRepository.fetchOnrampHistory(ADDRESS, any()) } returns onrampResponse(hasMore)
        coEvery { expressHistoryRepository.fetchOnrampHistoryDelta(ADDRESS, any()) } returns onrampDeltaResponse(hasMore)
    }

    private fun TestScope.createUtils(): DefaultTxHistoryFetcherUtils = DefaultTxHistoryFetcherUtils(
        appScope = TestAppCoroutineScope(testScope = this),
        analyticsEventHandler = mockk(relaxed = true),
        analyticsExceptionHandler = mockk(relaxed = true),
    )

    private fun createFetcher(utils: DefaultTxHistoryFetcherUtils) = DefaultExpressTxHistoryFetcher(
        address = ADDRESS,
        accountId = ACCOUNT_ID,
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
        address = ADDRESS,
        isInitialCompleted = true,
        afterCursor = null,
        deltaCursor = null,
    )

    private companion object {
        val WALLET_ID = MockAccounts.userWalletId
        val ACCOUNT_ID = AccountId.forMainCryptoPortfolio(WALLET_ID)
        const val ADDRESS = "0xEthAddress"
    }
}