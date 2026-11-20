package com.tangem.features.onramp.main.model

import arrow.core.Either
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.marketing.ObserveDemandSurveyCompletedUseCase
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.CheckOnrampAvailabilityUseCase
import com.tangem.domain.onramp.GetOnrampCountryUseCase
import com.tangem.domain.onramp.GetOnrampQuotesUseCase
import com.tangem.domain.onramp.OnrampFetchPairsUseCase
import com.tangem.domain.onramp.ResetOnrampCountryUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.marketing.api.CtaBannerRequest
import com.tangem.features.marketing.api.LinkedBannerRequest
import com.tangem.features.marketing.api.MarketingBannerAmount
import com.tangem.features.marketing.api.MarketingBannerRequest
import com.tangem.features.marketing.api.concierge.ConciergeDemandSurveyComponent
import com.tangem.features.onramp.deeplink.DefaultConciergeWaitlistTrigger
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.features.onramp.featuretoggles.OnrampFeatureToggles
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

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
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase = mockk()
    private val fetchPairsUseCase: OnrampFetchPairsUseCase = mockk()
    private val resetOnrampCountryUseCase: ResetOnrampCountryUseCase = mockk()
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase = mockk()
    private val observeDemandSurveyCompletedUseCase: ObserveDemandSurveyCompletedUseCase = mockk()
    private val onrampFeatureToggles: OnrampFeatureToggles = mockk()

    private val cryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns USER_WALLET_ID
    }

    private val sentEvents = mutableListOf<AnalyticsEvent>()
    private val conciergeWaitlistTrigger = DefaultConciergeWaitlistTrigger()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            analyticsEventHandler,
            getOnrampQuotesUseCase,
            getWalletsUseCase,
            getOnrampCountryUseCase,
            fetchPairsUseCase,
            resetOnrampCountryUseCase,
            checkOnrampAvailabilityUseCase,
            observeDemandSurveyCompletedUseCase,
            onrampFeatureToggles,
        )
        every { observeDemandSurveyCompletedUseCase(any()) } returns flowOf(false)
        every { getWalletsUseCase.invokeSync() } returns listOf(userWallet)
        every { getOnrampCountryUseCase.invoke() } returns emptyFlow()
        coEvery { fetchPairsUseCase.invoke(any(), any()) } returns Unit.right()
        coEvery { resetOnrampCountryUseCase.invoke() } returns Unit.right()
        coEvery { checkOnrampAvailabilityUseCase.invoke(any(), any()) } returns
            OnrampAvailability.Available(onrampCountry("USD"), onrampCountry("USD").defaultCurrency).right()
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

    @Test
    fun `GIVEN started screen WHEN waitlist deeplink triggered THEN survey opened for the Buy wallet`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList())
        model.attachBottomSheetSlot(initial = null)
        val activatedSurveys = model.collectActivatedSurveys()
        model.onStart()
        scheduler.runCurrent()

        // Act
        triggerWaitlist(scheduler)

        // Assert
        assertThat(activatedSurveys).containsExactly(ConciergeDemandSurveyComponent.Params(USER_WALLET_ID))
        model.onDestroy()
    }

    @Test
    fun `GIVEN started screen WHEN waitlist deeplink triggered THEN screen state kept`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList())
        model.onStart()
        scheduler.runCurrent()
        val stateBefore = model.state.value

        // Act
        triggerWaitlist(scheduler)

        // Assert
        assertThat(model.state.value).isSameInstanceAs(stateBefore)
        model.onDestroy()
    }

    @Test
    fun `GIVEN stopped screen WHEN waitlist deeplink triggered THEN survey not opened`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList())
        model.attachBottomSheetSlot(initial = null)
        val activatedSurveys = model.collectActivatedSurveys()
        model.onStart()
        scheduler.runCurrent()
        model.onStop()
        scheduler.runCurrent()

        // Act
        triggerWaitlist(scheduler)

        // Assert
        assertThat(activatedSurveys).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN all offers sheet open WHEN waitlist deeplink triggered THEN sheet closed and survey opened`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList())
        val bottomSheetSlot = model.attachBottomSheetSlot(initial = OnrampMainBottomSheetConfig.AllOffers("USD"))
        val activatedSurveys = model.collectActivatedSurveys()
        model.onStart()
        scheduler.runCurrent()

        // Act
        triggerWaitlist(scheduler)

        // Assert
        assertThat(bottomSheetSlot.config).isNull()
        assertThat(activatedSurveys).containsExactly(ConciergeDemandSurveyComponent.Params(USER_WALLET_ID))
        model.onDestroy()
    }

    @Test
    fun `GIVEN country selection open WHEN waitlist deeplink triggered THEN survey not opened`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList())
        val countrySelection = OnrampMainBottomSheetConfig.SelectCountry
        val bottomSheetSlot = model.attachBottomSheetSlot(initial = countrySelection)
        val activatedSurveys = model.collectActivatedSurveys()
        model.onStart()
        scheduler.runCurrent()

        // Act
        triggerWaitlist(scheduler)

        // Assert
        assertThat(bottomSheetSlot.config).isEqualTo(countrySelection)
        assertThat(activatedSurveys).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN model created WHEN init THEN saved country reset before availability check`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()

        // Act
        val model = createModel(scheduler = scheduler, quotes = emptyList())
        scheduler.runCurrent()

        // Assert
        coVerifyOrder {
            resetOnrampCountryUseCase.invoke()
            checkOnrampAvailabilityUseCase.invoke(userWallet, true)
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN country with onramp available WHEN country emitted THEN flag shown`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        every { getOnrampCountryUseCase.invoke() } returns
            flowOf(onrampCountry("USD").copy(image = "https://flag.png").right())

        // Act
        val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
        scheduler.runCurrent()

        // Assert
        val countryButton = model.state.value.topBarConfig.countryButtonUM
        assertThat(countryButton?.flagUrl).isEqualTo("https://flag.png")
        assertThat(countryButton?.isEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN country without onramp WHEN country emitted THEN onramp blocked with warning`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        every { getOnrampCountryUseCase.invoke() } returns
            flowOf(onrampCountry("USD").copy(isOnrampAvailable = false).right())

        // Act
        val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.buyNotSupportedMessage?.id).isEqualTo("country_not_supported")
        coVerify(exactly = 0) { fetchPairsUseCase.invoke(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN availability check rejects country WHEN model created THEN onramp blocked with warning`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { checkOnrampAvailabilityUseCase.invoke(any(), any()) } returns
            OnrampAvailability.NotSupported(onrampCountry("USD").copy(isOnrampAvailable = false)).right()

        // Act
        val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.buyNotSupportedMessage?.id).isEqualTo("country_not_supported")
        model.onDestroy()
    }

    @Test
    fun `GIVEN blocked country WHEN quotes arrive late THEN offers stay empty`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        every { getOnrampCountryUseCase.invoke() } returns
            flowOf(onrampCountry("USD").copy(isOnrampAvailable = false).right())
        val quotesFlow = quotesFlow()
        val model = createModel(scheduler = scheduler, quotesFlow = quotesFlow)
        scheduler.runCurrent()

        // Act
        quotesFlow.tryEmit(listOf(createQuote(isRestricted = false, providerName = "Mercuryo")).right())
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.buyNotSupportedMessage?.id).isEqualTo("country_not_supported")
        assertThat(geoRestrictionEvents()).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN loaded screen WHEN country button clicked THEN country selection opened`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList())
        val bottomSheetSlot = model.attachBottomSheetSlot(initial = null)
        scheduler.runCurrent()

        // Act
        model.state.value.topBarConfig.countryButtonUM?.onClick?.invoke()
        scheduler.runCurrent()

        // Assert
        assertThat(bottomSheetSlot.config).isEqualTo(OnrampMainBottomSheetConfig.SelectCountry)
        model.onDestroy()
    }

    @Test
    fun `GIVEN amount above every provider limit WHEN amount typed THEN marketing requests carry fiat amount`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val quotes = listOf(createAmountErrorQuote(OnrampError.AmountError.TooBigError(BigDecimal("1000"))))
        val model = createModel(scheduler = scheduler, quotes = quotes, fiatCode = "USD")
        val requests = collectAll(scheduler, model.marketingRequest)
        val linkedRequests = collectAll(scheduler, model.linkedMarketingRequest)
        scheduler.runCurrent()

        // Act
        model.onAmountValueChanged("50000")
        scheduler.runCurrent()

        // Assert
        val expectedScreen = onrampScreen(fiatCode = "USD")
        val expectedAmount = MarketingBannerAmount.Fiat(value = BigDecimal("50000"), currencyCode = "USD")
        assertThat(requests.last()).isEqualTo(MarketingBannerRequest(screen = expectedScreen, amount = expectedAmount))
        assertThat(linkedRequests.last())
            .isEqualTo(LinkedBannerRequest(screen = expectedScreen, amount = expectedAmount))
        model.onDestroy()
    }

    @Test
    fun `GIVEN eur selected WHEN amount typed THEN request carries eur amount`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "EUR")
        val requests = collectAll(scheduler, model.marketingRequest)
        scheduler.runCurrent()

        // Act
        model.onAmountValueChanged("100")
        scheduler.runCurrent()

        // Assert
        val expectedAmount = MarketingBannerAmount.Fiat(value = BigDecimal("100"), currencyCode = "EUR")
        assertThat(requests.last())
            .isEqualTo(MarketingBannerRequest(screen = onrampScreen("EUR"), amount = expectedAmount))
        model.onDestroy()
    }

    @Test
    fun `GIVEN typed zero WHEN amount typed THEN request carries zero amount`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")
        val requests = collectAll(scheduler, model.marketingRequest)
        scheduler.runCurrent()

        // Act
        model.onAmountValueChanged("0")
        scheduler.runCurrent()

        // Assert
        val expectedAmount = MarketingBannerAmount.Fiat(value = BigDecimal("0"), currencyCode = "USD")
        assertThat(requests.last())
            .isEqualTo(MarketingBannerRequest(screen = onrampScreen("USD"), amount = expectedAmount))
        model.onDestroy()
    }

    @Test
    fun `GIVEN typed amount WHEN field cleared THEN request has no amount`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")
        val requests = collectAll(scheduler, model.marketingRequest)
        scheduler.runCurrent()
        model.onAmountValueChanged("100")
        scheduler.runCurrent()
        val requestBeforeClearing = requests.last()

        // Act
        model.onAmountValueChanged("")
        scheduler.runCurrent()

        // Assert
        val typedAmount = MarketingBannerAmount.Fiat(value = BigDecimal("100"), currencyCode = "USD")
        assertThat(requestBeforeClearing)
            .isEqualTo(MarketingBannerRequest(screen = onrampScreen("USD"), amount = typedAmount))
        assertThat(requests.last()).isEqualTo(MarketingBannerRequest(screen = onrampScreen("USD"), amount = null))
        model.onDestroy()
    }

    private class FakeBottomSheetSlot(var config: OnrampMainBottomSheetConfig?)

    private fun OnrampMainComponentModel.attachBottomSheetSlot(
        initial: OnrampMainBottomSheetConfig?,
    ): FakeBottomSheetSlot {
        val slot = FakeBottomSheetSlot(initial)
        bottomSheetNavigation.subscribe { event ->
            val oldConfig = slot.config
            slot.config = event.transformer(oldConfig)
            event.onComplete(slot.config, oldConfig)
        }
        return slot
    }

    private fun OnrampMainComponentModel.collectActivatedSurveys(): List<ConciergeDemandSurveyComponent.Params> {
        val activated = mutableListOf<ConciergeDemandSurveyComponent.Params>()
        demandSurveyNavigation.subscribe { event -> event.transformer(null)?.let(activated::add) }
        return activated
    }

    private fun triggerWaitlist(scheduler: TestCoroutineScheduler) {
        TestScope(scheduler).launch { conciergeWaitlistTrigger.triggerOpenWaitlist() }
        scheduler.runCurrent()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CtaMarketingRequest {

        @Test
        fun `GIVEN survey status pending WHEN it arrives THEN request hides the strip until then`() {
            // Arrange
            val surveyStatus = MutableSharedFlow<Boolean>()
            every { observeDemandSurveyCompletedUseCase(USER_WALLET_ID) } returns surveyStatus
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")
            val requests = collectAll(scheduler, model.ctaMarketingRequest)
            scheduler.runCurrent()
            model.onAmountValueChanged("100")
            scheduler.runCurrent()
            val emittedWhilePending = requests.toList()

            // Act
            TestScope(scheduler).launch { surveyStatus.emit(false) }
            scheduler.runCurrent()

            // Assert
            assertThat(emittedWhilePending).containsExactly(
                ctaRequest(fiatCode = "USD", amount = null, isDemandSurveyCompleted = true),
                ctaRequest(fiatCode = "USD", amount = "100", isDemandSurveyCompleted = true),
            ).inOrder()
            assertThat(requests.drop(emittedWhilePending.size)).containsExactly(
                ctaRequest(fiatCode = "USD", amount = "100", isDemandSurveyCompleted = false),
            )
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN survey status WHEN eur amount typed THEN request carries the status and the eur amount`(
            model: SurveyStatusModel,
        ) {
            // Arrange
            every { observeDemandSurveyCompletedUseCase(USER_WALLET_ID) } returns flowOf(model.isCompleted)
            val scheduler = TestCoroutineScheduler()
            val onrampModel = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "EUR")
            val requests = collectAll(scheduler, onrampModel.ctaMarketingRequest)
            scheduler.runCurrent()

            // Act
            onrampModel.onAmountValueChanged("250")
            scheduler.runCurrent()

            // Assert
            assertThat(requests.last()).isEqualTo(
                ctaRequest(fiatCode = "EUR", amount = "250", isDemandSurveyCompleted = model.isCompleted),
            )
            onrampModel.onDestroy()
        }

        /** A failed status request reaches the model as `false`, see `DefaultDemandSurveyRepository` */
        private fun provideTestModels() = listOf(
            SurveyStatusModel(isCompleted = true),
            SurveyStatusModel(isCompleted = false),
        )

        @Test
        fun `GIVEN content WHEN screen refreshed THEN request is null until content returns, status requested once`() {
            // Arrange
            var statusRequests = 0
            every { observeDemandSurveyCompletedUseCase(USER_WALLET_ID) } returns flow {
                statusRequests++
                emit(false)
            }
            val country = MutableSharedFlow<Either<OnrampError, OnrampCountry?>>(replay = 1)
            country.tryEmit(onrampCountry("USD").right())
            every { getOnrampCountryUseCase.invoke() } returns country
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
            // The banner drops repeated requests, and Content comes back through more than one state update
            val requests = collectAll(scheduler, model.ctaMarketingRequest.distinctUntilChanged())
            scheduler.runCurrent()
            model.onAmountValueChanged("100")
            scheduler.runCurrent()
            val emittedBefore = requests.size

            // Act
            model.onRefresh()
            scheduler.runCurrent()
            TestScope(scheduler).launch { country.emit(onrampCountry("USD").right()) }
            scheduler.runCurrent()

            // Assert
            assertThat(requests.drop(emittedBefore - 1)).containsExactly(
                ctaRequest(fiatCode = "USD", amount = "100", isDemandSurveyCompleted = false),
                null,
                ctaRequest(fiatCode = "USD", amount = null, isDemandSurveyCompleted = false),
            ).inOrder()
            assertThat(statusRequests).isEqualTo(1)
            model.onDestroy()
        }

        @Test
        fun `GIVEN two collectors WHEN request collected THEN survey status requested once for the Buy wallet`() {
            // Arrange
            var statusRequests = 0
            every { observeDemandSurveyCompletedUseCase(USER_WALLET_ID) } returns flow {
                statusRequests++
                emit(false)
            }
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")

            // Act
            collectAll(scheduler, model.ctaMarketingRequest)
            collectAll(scheduler, model.ctaMarketingRequest)
            scheduler.runCurrent()

            // Assert
            assertThat(statusRequests).isEqualTo(1)
            model.onDestroy()
        }
    }

    internal data class SurveyStatusModel(val isCompleted: Boolean)

    private fun ctaRequest(fiatCode: String, amount: String?, isDemandSurveyCompleted: Boolean) = CtaBannerRequest(
        screen = onrampScreen(fiatCode),
        amount = amount?.let { MarketingBannerAmount.Fiat(value = BigDecimal(it), currencyCode = fiatCode) },
        isDemandSurveyCompleted = isDemandSurveyCompleted,
    )

    private fun <T> collectAll(scheduler: TestCoroutineScheduler, flow: Flow<T>): List<T> {
        val values = mutableListOf<T>()
        TestScope(scheduler).launch { flow.collect(values::add) }
        return values
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

    private fun createAmountErrorQuote(
        amountError: OnrampError.AmountError = OnrampError.AmountError.TooSmallError(requiredAmount = BigDecimal.TEN),
    ) = mockk<OnrampQuote.AmountError>(relaxed = true) {
        every { error } returns amountError
    }

    private fun onrampScreen(fiatCode: String) = MarketingScreen.Onramp(
        fromFiat = fiatCode,
        toNetwork = cryptoCurrency.network.rawId,
        toContractAddress = "",
    )

    private fun onrampCountry(fiatCode: String) = OnrampCountry(
        id = "1",
        name = "Country",
        code = "CC",
        image = "",
        alpha3 = "CCC",
        continent = "Europe",
        defaultCurrency = OnrampCurrency(
            name = fiatCode,
            code = fiatCode,
            image = null,
            precision = 2,
            unit = fiatCode,
        ),
        isOnrampAvailable = true,
    )

    @Test
    fun `GIVEN toggle off WHEN model created THEN legacy availability check requested and country not reset`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()

        // Act
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            isCountryByIpEnabled = false,
        )
        scheduler.runCurrent()

        // Assert
        coVerify(exactly = 1) { checkOnrampAvailabilityUseCase.invoke(userWallet, false) }
        coVerify(exactly = 0) { resetOnrampCountryUseCase.invoke() }
        assertThat(model.state.value.topBarConfig.countryButtonUM).isNull()
        assertThat(model.state.value.topBarConfig.endButtonUM).isNotNull()
    }

    @Test
    fun `GIVEN toggle off WHEN openSettings THEN settings screen opened`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        var settingsOpened = false
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            isCountryByIpEnabled = false,
            openSettings = { settingsOpened = true },
        )

        // Act
        model.openSettings()

        // Assert
        assertThat(settingsOpened).isTrue()
    }

    @Test
    fun `GIVEN toggle on WHEN model created THEN country button shown and country reset`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()

        // Act
        val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
        scheduler.runCurrent()

        // Assert
        coVerify(exactly = 1) { checkOnrampAvailabilityUseCase.invoke(userWallet, true) }
        coVerify(exactly = 1) { resetOnrampCountryUseCase.invoke() }
        assertThat(model.state.value.topBarConfig.countryButtonUM).isNotNull()
        assertThat(model.state.value.topBarConfig.endButtonUM).isNull()
    }

    @Test
    fun `GIVEN toggle on WHEN model destroyed THEN country reset runs outside the cancelled model scope`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
        scheduler.runCurrent()

        // Act
        model.onDestroy()

        // Assert
        coVerify(exactly = 2) { resetOnrampCountryUseCase.invoke() }
    }

    @Test
    fun `GIVEN toggle off WHEN model destroyed THEN country not reset`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            isCountryByIpEnabled = false,
        )
        scheduler.runCurrent()

        // Act
        model.onDestroy()

        // Assert
        coVerify(exactly = 0) { resetOnrampCountryUseCase.invoke() }
    }

    @Test
    fun `GIVEN token not supported for buy WHEN another country selected THEN flag follows the latest country`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val rampStateManager: RampStateManager = mockk()
        coEvery { rampStateManager.availableForBuy(any(), any()) } returns
            ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrencyName = cryptoCurrency.name)
        every { getOnrampCountryUseCase.invoke() } returns flowOf(
            onrampCountry("USD").copy(name = "First", image = "https://first.png").right(),
            onrampCountry("EUR").copy(name = "Second", image = "https://second.png").right(),
        )

        // Act
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            rampStateManager = rampStateManager,
        )
        scheduler.runCurrent()

        // Assert
        val countryButton = model.state.value.topBarConfig.countryButtonUM
        assertThat(countryButton?.flagUrl).isEqualTo("https://second.png")
        assertThat(countryButton?.countryName).isEqualTo("Second")
        model.onDestroy()
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
        quotes: List<OnrampQuote>,
        fiatCode: String,
    ): OnrampMainComponentModel {
        every { getOnrampCountryUseCase.invoke() } returns flowOf(onrampCountry(fiatCode).right())
        return createModel(scheduler = scheduler, quotesFlow = flowOf(quotes.right()))
    }

    private fun createModel(
        scheduler: TestCoroutineScheduler,
        quotesFlow: Flow<Either<OnrampError, List<OnrampQuote>>>,
        isCountryByIpEnabled: Boolean = true,
        openSettings: () -> Unit = {},
        rampStateManager: RampStateManager = mockk(relaxed = true),
    ): OnrampMainComponentModel {
        every { getOnrampQuotesUseCase.invoke() } returns quotesFlow
        every { onrampFeatureToggles.isCountryByIpEnabled } returns isCountryByIpEnabled
        val params = OnrampMainComponent.Params(
            userWalletId = USER_WALLET_ID,
            cryptoCurrency = cryptoCurrency,
            source = OnrampSource.TOKEN_DETAILS,
            openSettings = openSettings,
            openRedirectPage = {},
        )
        return OnrampMainComponentModel(
            dispatchers = scheduler.createTestingCoroutineDispatcherProvider(),
            analyticsEventHandler = analyticsEventHandler,
            router = mockk(relaxed = true),
            checkOnrampAvailabilityUseCase = checkOnrampAvailabilityUseCase,
            getOnrampCountryUseCase = getOnrampCountryUseCase,
            clearOnrampCacheUseCase = mockk(relaxed = true),
            resetOnrampCountryUseCase = resetOnrampCountryUseCase,
            fetchQuotesUseCase = mockk(relaxed = true),
            getOnrampQuotesUseCase = getOnrampQuotesUseCase,
            fetchPairsUseCase = fetchPairsUseCase,
            rampStateManager = rampStateManager,
            amountInputManager = mockk(relaxed = true),
            getOnrampOffersUseCase = mockk(relaxed = true),
            isDemoCardUseCase = mockk(relaxed = true),
            messageSender = mockk(relaxed = true),
            conciergeWaitlistListener = conciergeWaitlistTrigger,
            observeDemandSurveyCompletedUseCase = observeDemandSurveyCompletedUseCase,
            onrampFeatureToggles = onrampFeatureToggles,
            appCoroutineScope = TestAppCoroutineScope(),
            paramsContainer = MutableParamsContainer(value = params),
            getWalletsUseCase = getWalletsUseCase,
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "deadbeef")
    }
}