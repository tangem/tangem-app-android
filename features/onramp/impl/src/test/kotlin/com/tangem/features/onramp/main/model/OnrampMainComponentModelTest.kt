package com.tangem.features.onramp.main.model

import arrow.core.Either
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
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
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
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

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler, getOnrampQuotesUseCase, getWalletsUseCase)
        every { getWalletsUseCase.invokeSync() } returns listOf(userWallet)
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
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

    @Test
    fun `GIVEN two restricted providers WHEN quotes arrive THEN one geo restriction event per provider`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val quotes = listOf(
            createQuote(isRestricted = true, providerName = "Mercuryo"),
            createQuote(isRestricted = true, providerName = "MoonPay"),
        )

        // Act
        val model = createModel(scheduler = scheduler, quotes = quotes)
        scheduler.runCurrent()

        // Assert
        assertThat(geoRestrictionEvents())
            .containsExactly(geoRestrictionEvent("Mercuryo"), geoRestrictionEvent("MoonPay"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN restricted and purchasable quotes WHEN quotes arrive THEN no geo restriction event`() {
        // Arrange — the screen shows offers rather than the restriction error, so nothing is reported
        val scheduler = TestCoroutineScheduler()
        val quotes = listOf(
            createQuote(isRestricted = true, providerName = "Mercuryo"),
            createQuote(isRestricted = false, providerName = "MoonPay"),
        )

        // Act
        val model = createModel(scheduler = scheduler, quotes = quotes)
        scheduler.runCurrent()

        // Assert
        assertThat(geoRestrictionEvents()).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN restriction reported WHEN quotes are polled again THEN event not repeated`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val restricted = listOf(createQuote(isRestricted = true, providerName = "Mercuryo"))
        val quotesFlow = quotesFlow()
        val model = createModel(scheduler = scheduler, quotesFlow = quotesFlow)
        quotesFlow.tryEmit(restricted.right())
        scheduler.runCurrent()

        // Act
        quotesFlow.tryEmit(restricted.right())
        scheduler.runCurrent()

        // Assert
        assertThat(geoRestrictionEvents()).containsExactly(geoRestrictionEvent("Mercuryo"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN restriction cleared WHEN it appears again THEN event sent again`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val restricted = listOf(createQuote(isRestricted = true, providerName = "Mercuryo"))
        val purchasable = listOf(createQuote(isRestricted = false, providerName = "MoonPay"))
        val quotesFlow = quotesFlow()
        val model = createModel(scheduler = scheduler, quotesFlow = quotesFlow)
        quotesFlow.tryEmit(restricted.right())
        scheduler.runCurrent()

        // Act
        quotesFlow.tryEmit(purchasable.right())
        scheduler.runCurrent()
        quotesFlow.tryEmit(restricted.right())
        scheduler.runCurrent()

        // Assert
        val event = geoRestrictionEvent("Mercuryo")
        assertThat(geoRestrictionEvents()).containsExactly(event, event)
        model.onDestroy()
    }

    @Test
    fun `GIVEN restriction reported WHEN quotes are resubscribed THEN event sent again`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val restricted = listOf(createQuote(isRestricted = true, providerName = "Mercuryo"))
        val quotesFlow = quotesFlow()
        val model = createModel(scheduler = scheduler, quotesFlow = quotesFlow)
        quotesFlow.tryEmit(restricted.right())
        scheduler.runCurrent()

        // Act — a refresh tears the screen state down and subscribes again
        model.handleOnrampAvailable()
        scheduler.runCurrent()
        quotesFlow.tryEmit(restricted.right())
        scheduler.runCurrent()

        // Assert
        val event = geoRestrictionEvent("Mercuryo")
        assertThat(geoRestrictionEvents()).containsExactly(event, event)
        model.onDestroy()
    }

    @Test
    fun `GIVEN restriction event sent WHEN inspected THEN it carries the agreed params`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val quotes = listOf(createQuote(isRestricted = true, providerName = "Mercuryo"))

        // Act
        val model = createModel(scheduler = scheduler, quotes = quotes)
        scheduler.runCurrent()

        // Assert
        val event = geoRestrictionEvents().single()
        assertThat(event.id).isEqualTo("[Onramp] Notice - Geo Restriction")
        assertThat(event.params).containsExactlyEntriesIn(
            mapOf("Token" to cryptoCurrency.symbol, "Provider" to "Mercuryo"),
        )
        model.onDestroy()
    }

    /** `replay = 1` so an emission made before the model's collector starts is still delivered. */
    private fun quotesFlow() = MutableSharedFlow<Either<OnrampError, List<OnrampQuote>>>(
        replay = 1,
        extraBufferCapacity = 8,
    )

    private fun createQuote(isRestricted: Boolean, providerName: String = "Provider") = mockk<OnrampQuote.Data> {
        every { this@mockk.isRestricted } returns isRestricted
        every { provider.info.name } returns providerName
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

    private fun geoRestrictionEvents() = sentEvents.filterIsInstance<OnrampAnalyticsEvent.NoticeGeoRestriction>()

    private fun geoRestrictionEvent(providerName: String) = OnrampAnalyticsEvent.NoticeGeoRestriction(
        tokenSymbol = cryptoCurrency.symbol,
        providerName = providerName,
    )

    private fun createModel(scheduler: TestCoroutineScheduler, quotes: List<OnrampQuote>) =
        createModel(scheduler = scheduler, quotesFlow = flowOf(quotes.right()))

    private fun createModel(
        scheduler: TestCoroutineScheduler,
        quotesFlow: Flow<Either<OnrampError, List<OnrampQuote>>>,
    ): OnrampMainComponentModel {
        every { getOnrampQuotesUseCase.invoke() } returns quotesFlow
        val params = OnrampMainComponent.Params(
            userWalletId = USER_WALLET_ID,
            cryptoCurrency = cryptoCurrency,
            source = OnrampSource.TOKEN_DETAILS,
            openSettings = {},
            openRedirectPage = {},
        )
        return OnrampMainComponentModel(
            dispatchers = scheduler.createTestingCoroutineDispatcherProvider(),
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
            paramsContainer = MutableParamsContainer(value = params),
            getWalletsUseCase = getWalletsUseCase,
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "deadbeef")
    }
}