package com.tangem.data.txhistory.list

import arrow.core.Either
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.data.txhistory.list.chain.Action
import com.tangem.data.txhistory.list.chain.BsdkOnChainHistory
import com.tangem.data.txhistory.list.chain.IndexTableOnChainHistory
import com.tangem.data.txhistory.list.chain.TangemPayOnChainHistory
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistorySources
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.list.HistoryTxListManager.OnChainSource
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Verifies the orchestration in [DefaultHistoryTxListManager]: which on-chain backbone is chosen, availability gating,
 * the universal state sequence (initial load → error → retry → content), analytics on unexpected failures, and
 * forwarding of external actions to the chosen backbone. The backbones themselves are stubbed (tested separately).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultHistoryTxListManagerTest {

    private val userWalletId = UserWalletId(stringValue = "01")
    private val currency = mockk<CryptoCurrency>(relaxed = true)

    private val expressServiceFetcher = mockk<ExpressServiceFetcher>()
    private val paymentAccountCurrency = mockk<GetPaymentAccountCryptoCurrencyStatusUseCase>()
    private val getAccountCurrencyStatusUseCase = mockk<GetAccountCurrencyStatusUseCase>()
    private val txHistoryItemsCountUseCase = mockk<GetTxHistoryItemsCountUseCase>()
    private val analyticsExceptionHandler = mockk<AnalyticsExceptionHandler>(relaxed = true)

    private val bsdk = mockk<BsdkOnChainHistory>(relaxed = true)
    private val tangemPay = mockk<TangemPayOnChainHistory>(relaxed = true)
    private val indexTable = mockk<IndexTableOnChainHistory>(relaxed = true)
    private val bsdkFactory = mockk<BsdkOnChainHistory.Factory>()
    private val tangemPayFactory = mockk<TangemPayOnChainHistory.Factory>()
    private val indexTableFactory = mockk<IndexTableOnChainHistory.Factory>()

    @BeforeEach
    fun setup() {
        clearMocks(
            expressServiceFetcher,
            paymentAccountCurrency,
            getAccountCurrencyStatusUseCase,
            txHistoryItemsCountUseCase,
            analyticsExceptionHandler,
            bsdk,
            tangemPay,
            indexTable,
            bsdkFactory,
            tangemPayFactory,
            indexTableFactory,
        )
        every { bsdkFactory.create(any()) } returns bsdk
        every { tangemPayFactory.create(any()) } returns tangemPay
        every { indexTableFactory.create(any()) } returns indexTable
        every { bsdk.history() } returns emptyFlow()
        every { tangemPay.history() } returns emptyFlow()
        every { indexTable.history() } returns emptyFlow()

        // Defaults: not a crypto portfolio, not a payment account, express available.
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns none()
        coEvery { paymentAccountCurrency.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns none()
        coEvery { txHistoryItemsCountUseCase(any(), any()) } returns 5.right()
        coEvery { expressServiceFetcher.getOrFetch(any(), any()) } returns asset(exchange = true, onramp = false).right()
    }

    // region source resolution

    @ParameterizedTest
    @ProvideTestModels
    fun resolveOnChainSource(model: ResolutionModel) = runTest {
        // Arrange
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns
            if (model.isCryptoPortfolio) mockk<AccountCryptoCurrencyStatus>().some() else none()
        model.itemsCount?.let { coEvery { txHistoryItemsCountUseCase(any(), any()) } returns it }
        coEvery { paymentAccountCurrency.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns
            if (model.isPayment) (mockk<Account.Payment>() to mockk<CryptoCurrencyStatus>()).some() else none()

        // Act
        val scope = runManager()

        // Assert
        verify(exactly = if (model.expected == OnChainSource.BSDK) 1 else 0) { bsdkFactory.create(any()) }
        verify(exactly = if (model.expected == OnChainSource.TangemPay) 1 else 0) { tangemPayFactory.create(any()) }
        verify(exactly = if (model.expected == OnChainSource.IndexTable) 1 else 0) { indexTableFactory.create(any()) }
        scope.cancel()
    }

    private fun provideTestModels() = listOf(
        ResolutionModel(
            name = "crypto portfolio with tx history",
            isCryptoPortfolio = true,
            itemsCount = 5.right(),
            expected = OnChainSource.BSDK,
        ),
        ResolutionModel(
            name = "crypto portfolio with empty tx history",
            isCryptoPortfolio = true,
            itemsCount = Either.Left(TxHistoryStateError.EmptyTxHistories),
            expected = OnChainSource.BSDK,
        ),
        ResolutionModel(
            name = "crypto portfolio without tx history support",
            isCryptoPortfolio = true,
            itemsCount = Either.Left(TxHistoryStateError.TxHistoryNotImplemented),
            expected = OnChainSource.IndexTable,
        ),
        ResolutionModel(
            name = "payment account",
            isCryptoPortfolio = false,
            isPayment = true,
            expected = OnChainSource.TangemPay,
        ),
        ResolutionModel(
            name = "neither crypto nor payment",
            isCryptoPortfolio = false,
            isPayment = false,
            expected = OnChainSource.IndexTable,
        ),
    )

    // endregion

    // region availability

    @Test
    fun `GIVEN index-table source and express unavailable WHEN loading THEN Unavailable`() = runTest {
        // Arrange: no on-chain source and express not available → nothing to show.
        coEvery { expressServiceFetcher.getOrFetch(any(), any()) } returns asset(exchange = false, onramp = false).right()

        // Act
        val states: List<HistoryState>
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val manager = createManager(scope)
        states = getEmittedValues(manager.state)
        advanceUntilIdle()

        // Assert
        assertThat(states.last()).isEqualTo(HistoryState.Unavailable)
        verify(exactly = 0) { indexTableFactory.create(any()) }
        scope.cancel()
    }

    // endregion

    // region state sequence

    @Test
    fun `GIVEN backbone emits content WHEN loading THEN Loading then Content`() = runTest {
        // Arrange: BSDK source that emits a single content page.
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns
            mockk<AccountCryptoCurrencyStatus>().some()
        every { bsdk.history() } returns flowOf(content())

        // Act
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val states = getEmittedValues(createManager(scope).state)
        advanceUntilIdle()

        // Assert
        assertThat(states.first()).isEqualTo(HistoryState.Loading)
        assertThat(states.last()).isInstanceOf(HistoryState.Content::class.java)
        scope.cancel()
    }

    @Test
    fun `GIVEN load fails WHEN retried from UI THEN Error then Loading then Content`() = runTest {
        // Arrange: first load throws (DataError), retry succeeds with a content page.
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns
            mockk<AccountCryptoCurrencyStatus>().some()
        coEvery { txHistoryItemsCountUseCase(any(), any()) } returnsMany listOf(
            Either.Left(TxHistoryStateError.DataError(RuntimeException("boom"))),
            5.right(),
        )
        every { bsdk.history() } returns flowOf(content())

        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val manager = createManager(scope)
        val states = getEmittedValues(manager.state)
        advanceUntilIdle()

        // Act: retry from the UI layer after the full-screen error.
        manager.reload()
        advanceUntilIdle()

        // Assert
        assertThat(states).containsExactly(
            HistoryState.Loading,
            HistoryState.Error,
            HistoryState.Loading,
            content(),
        ).inOrder()
        scope.cancel()
    }

    @Test
    fun `GIVEN backbone throws unexpectedly WHEN collecting THEN exception reported and pipeline recovers`() = runTest {
        // Arrange: BSDK source; the history flow throws once, then recovers on the retry.
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns
            mockk<AccountCryptoCurrencyStatus>().some()
        every { bsdk.history() } returnsMany listOf(
            flow { throw RuntimeException("stream boom") },
            flowOf(content()),
        )

        // Act
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val states = getEmittedValues(createManager(scope).state)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsExceptionHandler.sendException(any()) }
        assertThat(states.last()).isInstanceOf(HistoryState.Content::class.java)
        scope.cancel()
    }

    // endregion

    // region historySources & action forwarding

    @Test
    fun `GIVEN successful load WHEN observed THEN historySources emitted`() = runTest {
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns
            mockk<AccountCryptoCurrencyStatus>().some()
        coEvery { expressServiceFetcher.getOrFetch(any(), any()) } returns asset(exchange = true, onramp = false).right()
        every { bsdk.history() } returns MutableStateFlow(content())

        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val sources = getEmittedValues(createManager(scope).historySources)
        advanceUntilIdle()

        assertThat(sources).containsExactly(
            HistorySources(onChainSource = OnChainSource.BSDK, isExchangeAvailable = true, isOnrampAvailable = false),
        )
        scope.cancel()
    }

    @Test
    fun `GIVEN loaded backbone WHEN reload and loadMore THEN forwarded to the backbone`() = runTest {
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(any<UserWalletId>(), any<CryptoCurrency>()) } returns
            mockk<AccountCryptoCurrencyStatus>().some()
        // Keep the backbone open so the forwarding collector stays active.
        every { bsdk.history() } returns MutableStateFlow(content())

        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val manager = createManager(scope)
        getEmittedValues(manager.state)
        advanceUntilIdle()

        // Act — the actions channel is rendezvous, so let each action be consumed before sending the next.
        manager.reload()
        advanceUntilIdle()
        manager.loadMore()
        advanceUntilIdle()

        // Assert
        verify { bsdk.sendAction(Action.Reload(shouldRefresh = true)) }
        verify { bsdk.sendAction(Action.LoadMore) }
        scope.cancel()
    }

    // endregion

    private fun TestScope.runManager(): CoroutineScope {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val manager = createManager(scope)
        getEmittedValues(manager.state)
        advanceUntilIdle()
        return scope
    }

    private fun TestScope.createManager(modelScope: CoroutineScope) = DefaultHistoryTxListManager(
        dispatchers = testDispatchers(),
        expressServiceFetcher = expressServiceFetcher,
        paymentAccountCurrency = paymentAccountCurrency,
        getAccountCryptoCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
        txHistoryItemsCountUseCase = txHistoryItemsCountUseCase,
        analyticsExceptionHandler = analyticsExceptionHandler,
        bsdkOnChainHistoryFactory = bsdkFactory,
        tangemPayOnChainHistoryFactory = tangemPayFactory,
        indexTableOnChainHistoryFactory = indexTableFactory,
        userWalletId = userWalletId,
        currency = currency,
        modelScope = modelScope,
    )

    private fun TestScope.testDispatchers(): CoroutineDispatcherProvider {
        val dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
        return object : CoroutineDispatcherProvider {
            override val main = dispatcher
            override val mainImmediate = dispatcher
            override val io = dispatcher
            override val default = dispatcher
            override val single = dispatcher
        }
    }

    private fun content() = HistoryState.Content(items = emptyList(), isLoadingMore = false, hasMore = false)

    private fun asset(exchange: Boolean, onramp: Boolean) = ExpressAsset(
        id = ExpressAsset.ID(networkId = "eth", contractAddress = "0"),
        isExchangeAvailable = exchange,
        isOnrampAvailable = onramp,
    )

    internal data class ResolutionModel(
        val name: String,
        val isCryptoPortfolio: Boolean,
        val itemsCount: Either<TxHistoryStateError, Int>? = null,
        val isPayment: Boolean = false,
        val expected: OnChainSource,
    ) {
        override fun toString(): String = name
    }
}