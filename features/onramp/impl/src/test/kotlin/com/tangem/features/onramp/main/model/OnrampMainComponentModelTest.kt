package com.tangem.features.onramp.main.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.marketing.ObserveDemandSurveyCompletedUseCase
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.onramp.CheckOnrampAvailabilityUseCase
import com.tangem.domain.onramp.GetOnrampCountryByIpUseCase
import com.tangem.domain.onramp.GetOnrampCountryUseCase
import com.tangem.domain.onramp.GetOnrampOffersUseCase
import com.tangem.domain.onramp.GetOnrampQuotesUseCase
import com.tangem.domain.onramp.OnrampFetchPairsUseCase
import com.tangem.domain.onramp.ResetOnrampCountryUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampOffer
import com.tangem.domain.onramp.model.OnrampOffersBlock
import com.tangem.domain.onramp.model.OnrampOfferCategory
import com.tangem.domain.onramp.model.OnrampOfferAdvantages
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.onramp.impl.R
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.marketing.api.CtaBannerRequest
import com.tangem.features.marketing.api.LinkedBannerRequest
import com.tangem.features.marketing.api.MarketingBannerAmount
import com.tangem.features.marketing.api.MarketingBannerRequest
import com.tangem.features.marketing.api.MarketingFeatureToggles
import com.tangem.features.marketing.api.MoneyAccountBannerAction
import com.tangem.features.marketing.api.concierge.ConciergeDemandSurveyComponent
import com.tangem.features.onramp.deeplink.DefaultConciergeWaitlistTrigger
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.features.onramp.featuretoggles.OnrampFeatureToggles
import com.tangem.features.onramp.main.OnrampMainComponent
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.features.onramp.main.entity.OnrampMainComponentUM
import com.tangem.features.onramp.main.entity.OnrampOffersBlockUM
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import com.tangem.features.onramp.main.entity.OnrampOfferCategoryUM
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val getOnrampOffersUseCase: GetOnrampOffersUseCase = mockk()
    private val getOnrampQuotesUseCase: GetOnrampQuotesUseCase = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase = mockk()
    private val fetchPairsUseCase: OnrampFetchPairsUseCase = mockk()
    private val resetOnrampCountryUseCase: ResetOnrampCountryUseCase = mockk()
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase = mockk()
    private val getOnrampCountryByIpUseCase: GetOnrampCountryByIpUseCase = mockk()
    private val observeDemandSurveyCompletedUseCase: ObserveDemandSurveyCompletedUseCase = mockk()
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()
    private val marketingFeatureToggles: MarketingFeatureToggles = mockk()
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
            getOnrampOffersUseCase,
            getWalletsUseCase,
            getOnrampCountryUseCase,
            fetchPairsUseCase,
            resetOnrampCountryUseCase,
            checkOnrampAvailabilityUseCase,
            getOnrampCountryByIpUseCase,
            observeDemandSurveyCompletedUseCase,
            moneyAccountStatusSupplier,
            marketingFeatureToggles,
            onrampFeatureToggles,
        )
        every { observeDemandSurveyCompletedUseCase(any()) } returns flowOf(false)
        every { moneyAccountStatusSupplier(any<UserWalletId>()) } returns emptyFlow()
        every { marketingFeatureToggles.isMoneyAccountBannerEnabled } returns true
        every { getWalletsUseCase.invokeSync() } returns listOf(userWallet)
        every { getOnrampCountryUseCase.invoke() } returns emptyFlow()
        every { getOnrampOffersUseCase.invoke() } returns emptyFlow()
        coEvery { fetchPairsUseCase.invoke(any(), any()) } returns Unit.right()
        coEvery { resetOnrampCountryUseCase.invoke() } returns Unit.right()
        coEvery { checkOnrampAvailabilityUseCase.invoke(any(), any(), any()) } returns
            OnrampAvailability.Available(onrampCountry("USD"), onrampCountry("USD").defaultCurrency).right()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns onrampCountry("USD").right()
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
            checkOnrampAvailabilityUseCase.invoke(userWallet, true, any())
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
        coVerify(exactly = 1) { fetchPairsUseCase.invoke(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN IP country rejected WHEN model created THEN only regional warning shown`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val restricted = onrampCountry("USD").copy(isOnrampAvailable = false)
        coEvery { checkOnrampAvailabilityUseCase.invoke(any(), any(), any()) } returns
            OnrampAvailability.NotSupported(restricted).right()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns restricted.right()

        // Act
        val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.buyNotSupportedMessage).isNull()
        assertThat(model.state.value.regionRestrictionMessage).isNotNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN restricted IP WHEN available country picked THEN banner stays and country does not block buy`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val restricted = onrampCountry("USD").copy(isOnrampAvailable = false)
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns restricted.right()
        every { getOnrampCountryUseCase.invoke() } returns flowOf(onrampCountry("EUR").right())
        val rampStateManager: RampStateManager = mockk()
        coEvery { rampStateManager.availableForBuy(any(), any()) } returns ScenarioUnavailabilityReason.None

        // Act
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            rampStateManager = rampStateManager,
        )
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.regionRestrictionMessage).isNotNull()
        assertThat(model.state.value.buyNotSupportedMessage).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN IP country restricted WHEN openRedirectPage THEN purchase is not launched`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val restricted = onrampCountry("USD").copy(isOnrampAvailable = false)
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns restricted.right()
        val opened = mutableListOf<OnrampProviderWithQuote.Data>()
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            openRedirectPage = { opened += it },
        )
        scheduler.runCurrent()

        // Act
        model.openRedirectPage(mockk())

        // Assert
        assertThat(opened).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN IP country restricted WHEN later lookup fails THEN restriction is retained`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val restricted = onrampCountry("USD").copy(isOnrampAvailable = false)
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns restricted.right()
        val opened = mutableListOf<OnrampProviderWithQuote.Data>()
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            openRedirectPage = { opened += it },
        )
        scheduler.runCurrent()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns OnrampError.PairsNotFound.left()

        // Act
        model.onRefresh()
        scheduler.runCurrent()
        model.openRedirectPage(mockk())

        // Assert
        assertThat(opened).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN IP country allowed WHEN lookup on screen return fails THEN buy is disabled`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val opened = mutableListOf<OnrampProviderWithQuote.Data>()
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            openRedirectPage = { opened += it },
        )
        model.onStart()
        scheduler.runCurrent()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns OnrampError.PairsNotFound.left()

        // Act
        model.onStart()
        scheduler.runCurrent()
        model.openRedirectPage(mockk())

        // Assert
        assertThat(model.isBuyEnabled.value).isFalse()
        assertThat(model.state.value.regionRestrictionMessage).isNull()
        assertThat(opened).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN lookup failed WHEN next lookup allowed THEN buy is enabled`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns OnrampError.PairsNotFound.left()
        val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
        model.onStart()
        scheduler.runCurrent()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns onrampCountry("USD").right()

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(model.isBuyEnabled.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN lookup failed on screen return WHEN amount typed THEN error with refresh stays`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        every { getOnrampCountryUseCase.invoke() } returns flowOf(onrampCountry("USD").right())
        val model = createModel(scheduler = scheduler, quotesFlow = emptyFlow())
        model.onStart()
        scheduler.runCurrent()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns OnrampError.DomainError(null).left()
        model.onStop()
        model.onStart()
        scheduler.runCurrent()

        // Act
        model.onAmountValueChanged("100")
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.errorNotification)
            .isInstanceOf(NotificationUM.Warning.OnrampErrorNotification::class.java)
        assertThat(model.isBuyEnabled.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN lookup failed on screen return WHEN next return lookup succeeds THEN error clears and pairs reload`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        every { getOnrampCountryUseCase.invoke() } returns flowOf(onrampCountry("USD").right())
        val model = createModel(scheduler = scheduler, quotesFlow = emptyFlow())
        model.onStart()
        scheduler.runCurrent()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns OnrampError.DomainError(null).left()
        model.onStop()
        model.onStart()
        scheduler.runCurrent()
        coEvery { getOnrampCountryByIpUseCase.invoke(any()) } returns onrampCountry("USD").right()

        // Act
        model.onStop()
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value).isInstanceOf(OnrampMainComponentUM.Content::class.java)
        assertThat(model.state.value.errorNotification).isNull()
        assertThat(model.isBuyEnabled.value).isTrue()
        coVerify(exactly = 2) { fetchPairsUseCase.invoke(userWallet, cryptoCurrency) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN IP country available WHEN openRedirectPage THEN purchase is launched`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val opened = mutableListOf<OnrampProviderWithQuote.Data>()
        val model = createModel(
            scheduler = scheduler,
            quotesFlow = flowOf(emptyList<OnrampQuote>().right()),
            openRedirectPage = { opened += it },
        )
        scheduler.runCurrent()
        val quote: OnrampProviderWithQuote.Data = mockk()

        // Act
        model.openRedirectPage(quote)

        // Assert
        assertThat(opened).containsExactly(quote)
        model.onDestroy()
    }

    @Test
    fun `GIVEN IP country available WHEN model created THEN no region banner`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()

        // Act
        val model = createModel(scheduler = scheduler, quotesFlow = flowOf(emptyList<OnrampQuote>().right()))
        scheduler.runCurrent()

        // Assert
        assertThat(model.state.value.regionRestrictionMessage).isNull()
        model.onDestroy()
    }

    @Nested
    inner class RegionRestriction {

        @Test
        fun `GIVEN overlapping cache writes WHEN later refresh fails THEN newest restriction remains`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val supported = onrampCountry("USD")
            val restricted = supported.copy(name = "Restricted IP", isOnrampAvailable = false)
            val first = CompletableDeferred<OnrampCountry>()
            var cached: OnrampCountry? = null
            var requests = 0
            val repository = mockk<OnrampRepository>()
            coEvery { repository.getCountryByIp(userWallet, false) } coAnswers {
                val result = when (++requests) {
                    1 -> first.await()
                    2 -> restricted
                    else -> error("Offline")
                }
                cached = result
                result
            }
            coEvery { repository.getCachedCountryByIp() } coAnswers { cached }
            every { getOnrampCountryUseCase() } returns flowOf(supported.right())
            val model = createModel(
                scheduler, quotesFlow = emptyFlow(),
                countryByIpUseCase = GetOnrampCountryByIpUseCase(repository, mockk()),
            )
            model.onStart()
            scheduler.runCurrent()
            model.onStop()
            model.onStart()
            scheduler.runCurrent()
            assertThat(model.isBuyEnabled.value).isFalse()
            first.complete(supported)
            scheduler.runCurrent()
            assertThat(model.isBuyEnabled.value).isFalse()

            // Act
            model.onStop()
            model.onStart()
            scheduler.runCurrent()

            // Assert
            try {
                assertThat(model.isBuyEnabled.value).isFalse()
                assertThat(model.state.value.regionRestrictionMessage).isNotNull()
            } finally {
                model.onDestroy()
            }
        }

        @Test
        fun `GIVEN manual refresh pending WHEN screen returns THEN content is restored`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val countries = MutableStateFlow<Either<OnrampError, OnrampCountry?>>(onrampCountry("USD").right())
            every { getOnrampCountryUseCase() } returns countries
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            model.onStart()
            scheduler.runCurrent()
            val refresh = CompletableDeferred<Either<OnrampError, OnrampAvailability>>()
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } coAnswers { refresh.await() }
            model.onRefresh()
            scheduler.runCurrent()
            assertThat(model.state.value).isInstanceOf(OnrampMainComponentUM.InitialLoading::class.java)
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } returns
                OnrampAvailability.Available(onrampCountry("USD"), onrampCountry("USD").defaultCurrency).right()

            // Act
            model.onStop()
            model.onStart()
            scheduler.runCurrent()

            // Assert
            try {
                assertThat(model.state.value).isInstanceOf(OnrampMainComponentUM.Content::class.java)
            } finally {
                model.onDestroy()
            }
        }

        @Test
        fun `GIVEN manual refresh pending WHEN screen returns THEN country loading waits for the replacement check`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            var countrySubscriptions = 0
            every { getOnrampCountryUseCase() } answers {
                countrySubscriptions++
                flowOf(onrampCountry("USD").right())
            }
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            model.onStart()
            scheduler.runCurrent()
            val refreshed = CompletableDeferred<Either<OnrampError, OnrampAvailability>>()
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } coAnswers { refreshed.await() }
            model.onRefresh()
            scheduler.runCurrent()
            val replacement = CompletableDeferred<Either<OnrampError, OnrampAvailability>>()
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } coAnswers { replacement.await() }
            val subscriptionsBeforeReturn = countrySubscriptions

            // Act
            model.onStop()
            model.onStart()
            scheduler.runCurrent()
            val subscriptionsWhileReplacementPending = countrySubscriptions
            replacement.complete(
                OnrampAvailability.Available(onrampCountry("USD"), onrampCountry("USD").defaultCurrency).right(),
            )
            scheduler.runCurrent()

            // Assert
            try {
                assertThat(subscriptionsWhileReplacementPending).isEqualTo(subscriptionsBeforeReturn)
                assertThat(countrySubscriptions).isEqualTo(subscriptionsBeforeReturn + 1)
            } finally {
                model.onDestroy()
            }
        }

        @Test
        fun `GIVEN superseded availability request WHEN old failure arrives THEN current content is retained`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val first = CompletableDeferred<Either<OnrampError, OnrampAvailability>>()
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } coAnswers { first.await() }
            every { getOnrampCountryUseCase() } returns flowOf(onrampCountry("USD").right())
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            model.onStart()
            scheduler.runCurrent()
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } returns
                OnrampAvailability.Available(onrampCountry("USD"), onrampCountry("USD").defaultCurrency).right()
            model.onStop()
            model.onStart()
            scheduler.runCurrent()
            assertThat(model.state.value.errorNotification).isNull()

            // Act
            first.complete(OnrampError.DomainError("Old request failed").left())
            scheduler.runCurrent()

            // Assert
            try {
                assertThat(model.state.value.errorNotification).isNull()
            } finally {
                model.onDestroy()
            }
        }

        @Test
        fun `GIVEN IP country detected WHEN checking availability THEN the same result is reused`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val restricted = onrampCountry("USD").copy(isOnrampAvailable = false)
            coEvery { getOnrampCountryByIpUseCase(userWallet) } returns restricted.right()
            val countries = MutableStateFlow<Either<OnrampError, OnrampCountry?>>(restricted.right())
            every { getOnrampCountryUseCase() } returns countries
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            scheduler.runCurrent()

            // Act
            countries.value = onrampCountry("EUR").copy(id = "selected").right()
            scheduler.runCurrent()

            // Assert
            coVerify(exactly = 1) { checkOnrampAvailabilityUseCase(userWallet, true, restricted) }
            coVerify(exactly = 1) { getOnrampCountryByIpUseCase(userWallet) }
            assertThat(model.isBuyEnabled.value).isFalse()
            assertThat(model.state.value.regionRestrictionMessage).isNotNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN availability loading fails WHEN IP lookup succeeds THEN restricted banner and buy block remain`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } returns OnrampError.PairsNotFound.left()
            coEvery { getOnrampCountryByIpUseCase(userWallet) } returns
                onrampCountry("USD").copy(isOnrampAvailable = false).right()

            // Act
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            scheduler.runCurrent()

            // Assert
            assertThat(model.state.value.regionRestrictionMessage).isNotNull()
            assertThat(model.isBuyEnabled.value).isFalse()
            coVerify(exactly = 1) { getOnrampCountryByIpUseCase(userWallet) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN availability loading pending WHEN IP lookup completes THEN restriction is already displayed`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val availability = CompletableDeferred<Either<OnrampError, OnrampAvailability>>()
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } coAnswers { availability.await() }
            coEvery { getOnrampCountryByIpUseCase(userWallet) } returns
                onrampCountry("USD").copy(isOnrampAvailable = false).right()

            // Act
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            scheduler.runCurrent()

            // Assert
            assertThat(model.state.value.regionRestrictionMessage).isNotNull()
            assertThat(model.isBuyEnabled.value).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN unsupported country and delayed token check WHEN token unsupported THEN loading becomes content`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val tokenSupport = CompletableDeferred<ScenarioUnavailabilityReason>()
            val rampStateManager: RampStateManager = mockk()
            coEvery { rampStateManager.availableForBuy(any(), any()) } coAnswers { tokenSupport.await() }
            every { getOnrampCountryUseCase() } returns
                flowOf(onrampCountry("USD").copy(isOnrampAvailable = false).right())
            val lookup = CompletableDeferred<Either<OnrampError, OnrampCountry>>()
            coEvery { getOnrampCountryByIpUseCase(userWallet) } coAnswers { lookup.await() }
            val model = createModel(scheduler, quotesFlow = emptyFlow(), rampStateManager = rampStateManager)
            scheduler.runCurrent()
            lookup.complete(onrampCountry("USD").right())
            scheduler.runCurrent()
            assertThat(model.state.value.buyNotSupportedMessage?.id).isEqualTo("country_not_supported")

            // Act
            tokenSupport.complete(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrency.name))
            scheduler.runCurrent()

            // Assert
            assertThat(model.state.value).isInstanceOf(OnrampMainComponentUM.Content::class.java)
            assertThat(model.state.value.buyNotSupportedMessage?.id).isEqualTo("buy_not_supported")
            assertThat(sentEvents.filterIsInstance<OnrampAnalyticsEvent.NoticeBuyNotSupported>()).hasSize(1)
            model.onDestroy()
        }

        @Test
        fun `GIVEN detected restricted IP WHEN second lookup fails THEN cached region banner and buy block remain`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val restricted = onrampCountry("USD").copy(name = "IP region", isOnrampAvailable = false)
            val repository: OnrampRepository = mockk()
            coEvery { repository.getCountryByIp(userWallet) } throws IllegalStateException("Offline")
            coEvery { repository.getCachedCountryByIp() } returns restricted
            every { getOnrampCountryUseCase() } returns flowOf(onrampCountry("EUR").right())
            val opened = mutableListOf<OnrampProviderWithQuote.Data>()

            // Act
            val model = createModel(
                scheduler = scheduler,
                quotesFlow = emptyFlow(),
                openRedirectPage = { opened += it },
                countryByIpUseCase = GetOnrampCountryByIpUseCase(repository, mockk()),
            )
            scheduler.runCurrent()
            model.openRedirectPage(mockk())

            // Assert
            assertThat(model.state.value.regionRestrictionMessage?.description).isEqualTo(
                resourceReference(R.string.onramp_region_restriction_description_v2, wrappedList("IP region")),
            )
            assertThat(model.isBuyEnabled.value).isFalse()
            assertThat(opened).isEmpty()
            model.onDestroy()
        }


        @Test
        fun `GIVEN restricted IP and country WHEN offers arrive THEN offers stay and buying is disabled`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val restricted = onrampCountry("USD").copy(isOnrampAvailable = false)
            every { getOnrampCountryUseCase() } returns flowOf(restricted.right())
            coEvery { getOnrampCountryByIpUseCase(userWallet) } returns restricted.right()
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } returns
                OnrampAvailability.NotSupported(restricted).right()
            val offers = MutableSharedFlow<Either<OnrampError, List<OnrampOffersBlock>>>()
            every { getOnrampOffersUseCase() } returns offers
            val quote = mockk<OnrampQuote.Data>(relaxed = true) {
                every { isRestricted } returns false
                every { toAmount } returns OnrampAmount(value = BigDecimal.ONE, decimals = 8, symbol = "ETH")
            }
            val model = createModel(scheduler, quotesFlow = emptyFlow(), initialFiatAmount = BigDecimal.TEN)
            scheduler.runCurrent()

            // Act
            TestScope(scheduler).launch {
                offers.emit(
                    listOf(
                        OnrampOffersBlock(
                            category = OnrampOfferCategory.Recommended,
                            offers = listOf(OnrampOffer(quote, BigDecimal.ZERO, OnrampOfferAdvantages.Default)),
                            hasMoreOffers = true,
                        ),
                    ).right(),
                )
            }
            scheduler.runCurrent()

            // Assert
            val content = model.state.value as OnrampMainComponentUM.Content
            val block = content.offersBlockState as OnrampOffersBlockUM.Content
            assertThat(block.recommended).hasSize(1)
            assertThat(block.recommended.single().isBuyEnabled).isFalse()
            assertThat(block.onrampAllOffersButtonConfig).isNotNull()
            assertThat(content.regionRestrictionMessage).isNotNull()
            assertThat(content.buyNotSupportedMessage).isNull()
            coVerify(exactly = 1) { fetchPairsUseCase(userWallet, cryptoCurrency) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN restricted country WHEN quotes arrive THEN displayed provider analytics are sent`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            every { getOnrampCountryUseCase() } returns
                flowOf(onrampCountry("USD").copy(isOnrampAvailable = false).right())
            val quotes = quotesFlow()
            val model = createModel(scheduler, quotesFlow = quotes)
            scheduler.runCurrent()

            // Act
            quotes.tryEmit(listOf(createQuote(isRestricted = false, providerName = "Mercuryo")).right())
            scheduler.runCurrent()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    OnrampAnalyticsEvent.ProviderCalculated(
                        providerName = "Mercuryo",
                        tokenSymbol = cryptoCurrency.symbol,
                        paymentMethod = "Card",
                    ),
                )
            }
            assertThat(model.isBuyRestricted).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN delayed IP check WHEN supported country selected THEN old result cannot restore blocked country`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val restricted = onrampCountry("USD").copy(isOnrampAvailable = false)
            val selected = onrampCountry("EUR").copy(id = "new", name = "Selected")
            val countries = MutableStateFlow<Either<OnrampError, OnrampCountry?>>(restricted.right())
            every { getOnrampCountryUseCase() } returns countries
            coEvery { checkOnrampAvailabilityUseCase(userWallet, true, any()) } returns
                OnrampAvailability.NotSupported(restricted).right()
            val lookup = CompletableDeferred<Either<OnrampError, OnrampCountry>>()
            coEvery { getOnrampCountryByIpUseCase(userWallet) } coAnswers { lookup.await() }
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            scheduler.runCurrent()

            // Act
            countries.value = selected.right()
            scheduler.runCurrent()
            lookup.complete(onrampCountry("USD").right())
            scheduler.runCurrent()

            // Assert
            assertThat(model.state.value.topBarConfig.countryButtonUM?.countryName).isEqualTo("Selected")
            assertThat(model.state.value.buyNotSupportedMessage).isNull()
            assertThat(model.isBuyRestricted).isFalse()
            coVerify(exactly = 2) { fetchPairsUseCase(userWallet, cryptoCurrency) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN IP restriction WHEN screen returns in allowed region THEN banner clears and buy is allowed`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            coEvery { getOnrampCountryByIpUseCase(userWallet) } returns
                onrampCountry("USD").copy(isOnrampAvailable = false).right()
            val opened = mutableListOf<OnrampProviderWithQuote.Data>()
            val model = createModel(scheduler, quotesFlow = emptyFlow(), openRedirectPage = { opened += it })
            model.onStart()
            scheduler.runCurrent()
            assertThat(model.state.value.regionRestrictionMessage).isNotNull()
            coEvery { getOnrampCountryByIpUseCase(userWallet) } returns onrampCountry("USD").right()
            val quote = mockk<OnrampProviderWithQuote.Data>()

            // Act
            model.onStop()
            model.onStart()
            scheduler.runCurrent()
            model.openRedirectPage(quote)

            // Assert
            assertThat(model.state.value.regionRestrictionMessage).isNull()
            assertThat(model.isBuyEnabled.value).isTrue()
            assertThat(opened).containsExactly(quote)
            model.onDestroy()
        }

        @Test
        fun `GIVEN overlapping IP checks WHEN older restriction arrives last THEN latest decision is retained`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val firstLookup = CompletableDeferred<Either<OnrampError, OnrampCountry>>()
            coEvery { getOnrampCountryByIpUseCase(userWallet) } coAnswers { firstLookup.await() }
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            model.onStart()
            scheduler.runCurrent()
            coEvery { getOnrampCountryByIpUseCase(userWallet) } returns onrampCountry("USD").right()

            // Act
            model.onStop()
            model.onStart()
            scheduler.runCurrent()
            firstLookup.complete(onrampCountry("USD").copy(isOnrampAvailable = false).right())
            scheduler.runCurrent()

            // Assert
            assertThat(model.isBuyEnabled.value).isTrue()
            assertThat(model.state.value.regionRestrictionMessage).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN restricted country WHEN buy clicked THEN purchase and buy analytics are suppressed`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            every { getOnrampCountryUseCase() } returns
                flowOf(onrampCountry("USD").copy(isOnrampAvailable = false).right())
            val opened = mutableListOf<OnrampProviderWithQuote.Data>()
            val model = createModel(scheduler, quotesFlow = emptyFlow(), openRedirectPage = { opened += it })
            scheduler.runCurrent()

            // Act
            model.onBuyClick(
                quote = mockk(),
                onrampOfferAdvantagesUM = OnrampOfferAdvantagesUM.Default,
                categoryUM = OnrampOfferCategoryUM.Recommended,
            )
            model.openRedirectPage(mockk())

            // Assert
            assertThat(opened).isEmpty()
            assertThat(sentEvents.filterIsInstance<OnrampAnalyticsEvent.OnBuyClick>()).isEmpty()
            model.onDestroy()
        }

        @Test
        fun `GIVEN returned screen WHEN IP changes THEN buy follows the refreshed restriction`() {
            // Arrange
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler, quotesFlow = emptyFlow())
            model.onStart()
            scheduler.runCurrent()
            val lookup = CompletableDeferred<Either<OnrampError, OnrampCountry>>()
            coEvery { getOnrampCountryByIpUseCase(userWallet) } coAnswers { lookup.await() }

            // Act
            model.onStop()
            model.onStart()
            scheduler.runCurrent()

            // Assert
            assertThat(model.isBuyRestricted).isTrue()
            lookup.complete(onrampCountry("USD").copy(isOnrampAvailable = false).right())
            scheduler.runCurrent()
            assertThat(model.state.value.regionRestrictionMessage).isNotNull()
            assertThat(model.isBuyRestricted).isTrue()
            coVerify(exactly = 2) { getOnrampCountryByIpUseCase(userWallet) }
            model.onDestroy()
        }
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

        @Test
        fun `GIVEN money account onboarding WHEN status arrives THEN request asks to activate`() {
            // Arrange
            val status = mockk<AccountStatus.Money> {
                every { value } returns mockk<MoneyAccountStatusValue.Onboarding.Failed>()
            }
            every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns flowOf(status)
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")

            // Act
            val requests = collectAll(scheduler, model.ctaMarketingRequest)
            scheduler.runCurrent()
            model.onAmountValueChanged("100")
            scheduler.runCurrent()

            // Assert
            assertThat(requests.last()).isEqualTo(
                ctaRequest(
                    fiatCode = "USD",
                    amount = "100",
                    isDemandSurveyCompleted = false,
                    moneyAccountAction = MoneyAccountBannerAction.ACTIVATE,
                ),
            )
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account error WHEN status becomes known THEN request updates without changing amount`() {
            // Arrange
            val unknown = mockk<AccountStatus.Money> {
                every { value } returns mockk<MoneyAccountStatusValue.Error.NotSynced>()
            }
            val onboarding = mockk<AccountStatus.Money> {
                every { value } returns mockk<MoneyAccountStatusValue.Onboarding.Failed>()
            }
            val statuses = MutableStateFlow(unknown)
            every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns statuses
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")
            val requests = collectAll(scheduler, model.ctaMarketingRequest)

            // Act
            scheduler.runCurrent()
            model.onAmountValueChanged("100")
            scheduler.runCurrent()
            val before = requests.last()
            statuses.value = onboarding
            scheduler.runCurrent()

            // Assert
            assertThat(before).isEqualTo(ctaRequest("USD", "100", isDemandSurveyCompleted = false))
            assertThat(requests.last()).isEqualTo(
                ctaRequest("USD", "100", isDemandSurveyCompleted = false, moneyAccountAction = MoneyAccountBannerAction.ACTIVATE),
            )
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account status empty WHEN request built THEN campaign keeps its own button text`() {
            // Arrange
            val status = mockk<AccountStatus.Money> { every { value } returns MoneyAccountStatusValue.Empty }
            every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns flowOf(status)
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")

            // Act
            val requests = collectAll(scheduler, model.ctaMarketingRequest)
            scheduler.runCurrent()
            model.onAmountValueChanged("100")
            scheduler.runCurrent()

            // Assert
            assertThat(requests.last()).isEqualTo(ctaRequest("USD", "100", isDemandSurveyCompleted = false))
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account status fails WHEN request built THEN action stays unknown`() {
            // Arrange
            every { moneyAccountStatusSupplier(USER_WALLET_ID) } returns flow { error("no status") }
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")

            // Act
            val requests = collectAll(scheduler, model.ctaMarketingRequest)
            scheduler.runCurrent()
            model.onAmountValueChanged("100")
            scheduler.runCurrent()

            // Assert
            assertThat(requests.last()).isEqualTo(
                ctaRequest(fiatCode = "USD", amount = "100", isDemandSurveyCompleted = false),
            )
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account banner disabled WHEN request built THEN status not looked up`() {
            // Arrange
            every { marketingFeatureToggles.isMoneyAccountBannerEnabled } returns false
            val scheduler = TestCoroutineScheduler()
            val model = createModel(scheduler = scheduler, quotes = emptyList(), fiatCode = "USD")

            // Act
            val requests = collectAll(scheduler, model.ctaMarketingRequest)
            scheduler.runCurrent()
            model.onAmountValueChanged("100")
            scheduler.runCurrent()

            // Assert
            assertThat(requests.last()).isEqualTo(ctaRequest("USD", "100", isDemandSurveyCompleted = false))
            verify(exactly = 0) { moneyAccountStatusSupplier(any<UserWalletId>()) }
            model.onDestroy()
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

    private fun ctaRequest(
        fiatCode: String,
        amount: String?,
        isDemandSurveyCompleted: Boolean,
        moneyAccountAction: MoneyAccountBannerAction? = null,
    ) = CtaBannerRequest(
        screen = onrampScreen(fiatCode),
        amount = amount?.let { MarketingBannerAmount.Fiat(value = BigDecimal(it), currencyCode = fiatCode) },
        isDemandSurveyCompleted = isDemandSurveyCompleted,
        moneyAccountAction = moneyAccountAction,
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
        coVerify(exactly = 1) { checkOnrampAvailabilityUseCase.invoke(userWallet, true, any()) }
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
        rampStateManager: RampStateManager = mockk {
            coEvery { availableForBuy(any(), any()) } returns ScenarioUnavailabilityReason.None
        },
        openRedirectPage: (OnrampProviderWithQuote.Data) -> Unit = {},
        initialFiatAmount: BigDecimal? = null,
        countryByIpUseCase: GetOnrampCountryByIpUseCase = getOnrampCountryByIpUseCase,
    ): OnrampMainComponentModel {
        every { getOnrampQuotesUseCase.invoke() } returns quotesFlow
        every { onrampFeatureToggles.isCountryByIpEnabled } returns isCountryByIpEnabled
        val params = OnrampMainComponent.Params(
            userWalletId = USER_WALLET_ID,
            cryptoCurrency = cryptoCurrency,
            source = OnrampSource.TOKEN_DETAILS,
            openSettings = openSettings,
            openRedirectPage = openRedirectPage,
            initialFiatAmount = initialFiatAmount,
        )
        return OnrampMainComponentModel(
            dispatchers = scheduler.createTestingCoroutineDispatcherProvider(),
            analyticsEventHandler = analyticsEventHandler,
            router = mockk(relaxed = true),
            checkOnrampAvailabilityUseCase = checkOnrampAvailabilityUseCase,
            getOnrampCountryByIpUseCase = countryByIpUseCase,
            getOnrampCountryUseCase = getOnrampCountryUseCase,
            clearOnrampCacheUseCase = mockk(relaxed = true),
            resetOnrampCountryUseCase = resetOnrampCountryUseCase,
            fetchQuotesUseCase = mockk(relaxed = true),
            getOnrampQuotesUseCase = getOnrampQuotesUseCase,
            fetchPairsUseCase = fetchPairsUseCase,
            rampStateManager = rampStateManager,
            amountInputManager = mockk(relaxed = true),
            getOnrampOffersUseCase = getOnrampOffersUseCase,
            isDemoCardUseCase = mockk(relaxed = true),
            messageSender = mockk(relaxed = true),
            conciergeWaitlistListener = conciergeWaitlistTrigger,
            observeDemandSurveyCompletedUseCase = observeDemandSurveyCompletedUseCase,
            moneyAccountStatusSupplier = moneyAccountStatusSupplier,
            marketingFeatureToggles = marketingFeatureToggles,
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