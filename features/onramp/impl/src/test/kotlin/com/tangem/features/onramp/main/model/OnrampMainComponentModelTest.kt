package com.tangem.features.onramp.main.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.GetOnrampQuotesUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Covers the branch selection in `OnrampMainComponentModel.handleQuoteResult` — the routing that decides
 * whether an all-restricted payload is treated as "nothing you can buy in your region" or as a plain
 * failure. Two observables tell every branch apart while the screen is still loading:
 *
 * | payload                        | ProviderCalculated analytics | errorNotification |
 * |--------------------------------|------------------------------|-------------------|
 * | all restricted                 | not sent                     | null              |
 * | all error (nothing restricted) | not sent                     | set               |
 * | restricted + purchasable       | sent                         | null              |
 *
 * The `Content`-state effects of each branch are covered by
 * [com.tangem.features.onramp.main.entity.factory.OnrampAmountStateFactoryTest]; splitting it this way
 * keeps this test free of a full `Content` fixture.
 *
 * The restricted cases assert an *absence* (no error notification, no analytics), which on its own would
 * also hold if the quote collector never ran. They are only meaningful next to the two positive cases —
 * all-error sets the notification and mixed sends the analytics — which prove the wiring delivers quotes
 * and can reach both other branches. Keep the set together.
 *
 * These tests deliberately do NOT use `runTest`: the model schedules a [com.tangem.utils.coroutines.PeriodicTask]
 * quote refresh, an unbounded `while` + `delay` loop, and `runTest`'s terminal `advanceUntilIdle()` would
 * run it forever in virtual time. Driving the scheduler with `runCurrent()` executes the init work and
 * leaves the periodic task parked on its first delay.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampMainComponentModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getOnrampQuotesUseCase: GetOnrampQuotesUseCase = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()

    private val cryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns USER_WALLET_ID
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler, getOnrampQuotesUseCase, getWalletsUseCase)
        every { getWalletsUseCase.invokeSync() } returns listOf(userWallet)
    }

    @Test
    fun `GIVEN only restricted quotes WHEN quotes arrive THEN not treated as a failure`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val quotes = listOf(createQuote(isRestricted = true), createQuote(isRestricted = true))

        // Act
        val model = createModel(scheduler = scheduler, quotes = quotes)
        scheduler.runCurrent()

        // Assert — the region-restriction branch, not the generic error branch
        assertThat(model.state.value.errorNotification).isNull()
        verify(exactly = 0) { analyticsEventHandler.send(ofType<OnrampAnalyticsEvent.ProviderCalculated>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN restricted and amount error quotes WHEN quotes arrive THEN restriction branch still wins`() {
        // Arrange — `quotes.all { it is AmountError }` fails first, so routing falls through to the
        // restricted check, which relies on the OnrampQuote.isRestricted extension answering false for
        // AmountError quotes
        val scheduler = TestCoroutineScheduler()
        val quotes = listOf(createQuote(isRestricted = true), createAmountErrorQuote())

        // Act
        val model = createModel(scheduler = scheduler, quotes = quotes)
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.errorNotification).isNull()
        verify(exactly = 0) { analyticsEventHandler.send(ofType<OnrampAnalyticsEvent.ProviderCalculated>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN only error quotes WHEN quotes arrive THEN generic error state shown`() {
        // Arrange — nothing purchasable and nothing restricted either
        val scheduler = TestCoroutineScheduler()
        val quotes = listOf(createErrorQuote(), createErrorQuote())

        // Act
        val model = createModel(scheduler = scheduler, quotes = quotes)
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.errorNotification).isNotNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN restricted and purchasable quotes WHEN quotes arrive THEN normal path is taken`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val quotes = listOf(createQuote(isRestricted = true), createQuote(isRestricted = false))

        // Act
        val model = createModel(scheduler = scheduler, quotes = quotes)
        scheduler.runCurrent()

        // Assert — a purchasable quote exists, so the screen behaves normally and reports the best provider
        assertThat(model.state.value.errorNotification).isNull()
        verify { analyticsEventHandler.send(ofType<OnrampAnalyticsEvent.ProviderCalculated>()) }
        model.onDestroy()
    }

    private fun createQuote(isRestricted: Boolean) = mockk<OnrampQuote.Data> {
        every { this@mockk.isRestricted } returns isRestricted
        every { provider.info.name } returns "Provider"
        every { paymentMethod.name } returns "Card"
        every { toAmount.value } returns BigDecimal.ONE
    }

    /**
     * The quote-error analytics pass runs before the routing decision and inspects the error, so a real
     * [OnrampError] is required — a mocked one makes that pass throw, which kills the collector and the
     * routing `when` never runs.
     */
    private fun createErrorQuote() = mockk<OnrampQuote.Error>(relaxed = true) {
        every { error } returns OnrampError.DataError(code = "500", description = "boom")
    }

    private fun createAmountErrorQuote() = mockk<OnrampQuote.AmountError>(relaxed = true) {
        every { error } returns OnrampError.AmountError.TooSmallError(requiredAmount = BigDecimal.TEN)
    }

    private fun createModel(scheduler: TestCoroutineScheduler, quotes: List<OnrampQuote>): OnrampMainComponentModel {
        every { getOnrampQuotesUseCase.invoke() } returns flowOf(quotes.right())
        val params = OnrampMainComponent.Params(
            userWalletId = USER_WALLET_ID,
            cryptoCurrency = cryptoCurrency,
            source = OnrampSource.TOKEN_DETAILS,
            openSettings = {},
            openRedirectPage = {},
        )
        return OnrampMainComponentModel(
            dispatchers = createTestingCoroutineDispatcherProvider(scheduler),
            analyticsEventHandler = analyticsEventHandler,
            router = mockk(relaxed = true),
            checkOnrampAvailabilityUseCase = mockk(relaxed = true) {
                coEvery { this@mockk.invoke(any()) } returns mockk(relaxed = true)
            },
            getOnrampCountryUseCase = mockk(relaxed = true),
            clearOnrampCacheUseCase = mockk(relaxed = true),
            fetchQuotesUseCase = mockk(relaxed = true),
            getOnrampQuotesUseCase = getOnrampQuotesUseCase,
            fetchPairsUseCase = mockk(relaxed = true),
            rampStateManager = mockk(relaxed = true),
            amountInputManager = mockk(relaxed = true),
            getOnrampOffersUseCase = mockk(relaxed = true),
            isDemoCardUseCase = mockk(relaxed = true),
            messageSender = mockk(relaxed = true),
            getCurrencyUSDQuoteUseCase = mockk(relaxed = true),
            featureTogglesManager = mockk(relaxed = true),
            paramsContainer = MutableParamsContainer(value = params),
            getWalletsUseCase = getWalletsUseCase,
        )
    }

    private fun createTestingCoroutineDispatcherProvider(
        scheduler: TestCoroutineScheduler,
    ): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(scheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "deadbeef")
    }
}