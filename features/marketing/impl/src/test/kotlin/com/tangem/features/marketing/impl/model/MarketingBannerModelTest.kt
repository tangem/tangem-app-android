package com.tangem.features.marketing.impl.model

import android.net.Uri
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.marketing.DismissMarketingBannerUseCase
import com.tangem.domain.marketing.GetMarketingBannerUseCase
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.domain.marketing.models.MarketingBanner
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.MarketingScreenType
import com.tangem.domain.quotes.GetFiatUSDRateUseCase
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.features.marketing.api.CtaBannerRequest
import com.tangem.features.marketing.api.LinkedBannerRequest
import com.tangem.features.marketing.api.MarketingBannerAmount
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.marketing.api.MarketingBannerRequest
import com.tangem.features.marketing.impl.ui.state.MarketingBannerListUM
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketingBannerModelTest {

    private val getMarketingBanner: GetMarketingBannerUseCase = mockk()
    private val dismissMarketingBanner: DismissMarketingBannerUseCase = mockk()
    private val deeplinkLauncher: DeeplinkLauncher = mockk(relaxed = true)
    private val quotesRepository: QuotesRepository = mockk()
    private val marketingUidBindingRepository: MarketingUidBindingRepository = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)

    @BeforeAll
    fun mockUriParsing() {
        mockkStatic(Uri::class)
    }

    @AfterAll
    fun unmockUriParsing() {
        unmockkStatic(Uri::class)
    }

    @BeforeEach
    fun setup() {
        clearMocks(
            getMarketingBanner,
            dismissMarketingBanner,
            deeplinkLauncher,
            quotesRepository,
            marketingUidBindingRepository,
            urlOpener,
        )
        every { Uri.parse(any()) } returns uriWithUids()
    }

    private fun TestScope.createModel(
        params: MarketingBannerComponent.Params,
        clock: Clock = testClock(),
    ): MarketingBannerModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : CoroutineDispatcherProvider {
            override val main = dispatcher
            override val mainImmediate = dispatcher
            override val io = dispatcher
            override val default = dispatcher
            override val single = dispatcher
        }
        return MarketingBannerModel(
            dispatchers = dispatchers,
            paramsContainer = MutableParamsContainer(params),
            getMarketingBanner = getMarketingBanner,
            dismissMarketingBanner = dismissMarketingBanner,
            deeplinkLauncher = deeplinkLauncher,
            getFiatUSDRate = GetFiatUSDRateUseCase(quotesRepository),
            marketingUidBindingRepository = marketingUidBindingRepository,
            appCoroutineScope = TestAppCoroutineScope(),
            urlOpener = urlOpener,
            clock = clock,
        )
    }

    private fun TestScope.testClock(baseInstant: Instant = BASE_TEST_INSTANT): Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = baseInstant.plusMillis(testScheduler.currentTime)
    }

    private class MutableClock(var current: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = current
    }

    private fun uriWithUids(vararg uids: String): Uri = mockk {
        every { getQueryParameters("uid") } returns uids.toList()
    }

    private fun campaign(
        id: Int,
        uiType: MarketingBanner.UiType,
        providerIds: List<String>? = null,
        deeplink: String? = "tangem://support",
        minAmount: java.math.BigDecimal? = null,
        startAt: String? = null,
        endAt: String? = null,
    ) = MarketingCampaign(
        id = id,
        type = MarketingScreenType.ONRAMP,
        priority = id,
        startAt = startAt,
        endAt = endAt,
        minAmount = minAmount,
        maxAmount = null,
        providerIds = providerIds,
        banner = MarketingBanner(
            uiType = uiType,
            text = "text-$id",
            iconUrl = null,
            iconAlign = null,
            bgColor = null,
            deeplink = deeplink,
            isDismissible = true,
        ),
        targets = emptyList(),
    )

    private val onrampScreen = MarketingScreen.Onramp("USD", "ethereum", "0xabc")
    private val eurScreen = MarketingScreen.Onramp("EUR", "ethereum", "0xabc")

    private fun usdAmount(value: Int) = MarketingBannerAmount.Usd(java.math.BigDecimal(value))

    private fun eurAmount(value: String) =
        MarketingBannerAmount.Fiat(value = java.math.BigDecimal(value), currencyCode = "EUR")

    private fun swapScreen(fromContract: String = "0xF") =
        MarketingScreen.Swap(fromNetwork = "eth", fromContractAddress = fromContract, toNetwork = "btc", toContractAddress = "0xT")

    private fun gatedCampaign(id: Int) = MarketingCampaign(
        id = id, type = MarketingScreenType.SWAP, priority = id, startAt = null, endAt = null,
        minAmount = java.math.BigDecimal(50), maxAmount = java.math.BigDecimal(300), providerIds = null,
        banner = MarketingBanner(
            uiType = MarketingBanner.UiType.STANDALONE, text = "t$id", iconUrl = null,
            iconAlign = null, bgColor = null, deeplink = null, isDismissible = false,
        ),
        targets = emptyList(),
    )

    @Test
    fun `GIVEN amount changes WHEN same pair THEN re-filters locally without re-fetch`() = runTest {
        // Arrange
        val screen = swapScreen()
        coEvery { getMarketingBanner(screen, null) } returns listOf(gatedCampaign(1)).right()
        val requests = MutableStateFlow<MarketingBannerRequest?>(
            MarketingBannerRequest(screen, usdAmount(10)), // below min -> hidden
        )
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))

        // Act + Assert
        advanceUntilIdle()
        assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden) // 10 < 50

        requests.value = MarketingBannerRequest(screen, usdAmount(100)) // in range
        advanceUntilIdle()
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)

        // fetched once for the pair, despite two different amounts
        coVerify(exactly = 1) { getMarketingBanner(screen, null) }
    }

    @Test
    fun `GIVEN bounded campaign and eur amount WHEN amount changes THEN filtered by converted usd amount`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(eurScreen, null) } returns listOf(gatedCampaign(1)).right()
        coEvery { quotesRepository.getFiatUSDRate("EUR") } returns java.math.BigDecimal("1.1")
        val requests = MutableStateFlow<MarketingBannerRequest?>(
            MarketingBannerRequest(eurScreen, amount = eurAmount("40")), // 44 USD < 50 -> hidden
        )
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))

        // Act
        advanceUntilIdle()
        val stateBelowMin = model.uiState.value
        requests.value = MarketingBannerRequest(eurScreen, amount = eurAmount("50")) // 55 USD -> shown
        advanceUntilIdle()

        // Assert
        assertThat(stateBelowMin).isEqualTo(MarketingBannerListUM.Hidden)
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)
        coVerify(exactly = 1) { quotesRepository.getFiatUSDRate("EUR") }
    }

    @Test
    fun `GIVEN no bounded campaigns WHEN fiat amount emitted THEN rate not fetched`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(eurScreen, null) } returns listOf(
            campaign(1, MarketingBanner.UiType.STANDALONE),
        ).right()
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(
                flowOf(MarketingBannerRequest(eurScreen, amount = eurAmount("100"))),
            ),
        )

        // Act
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)
        coVerify(exactly = 0) { quotesRepository.getFiatUSDRate(any()) }
    }

    @Test
    fun `GIVEN bounded campaign and usd fiat amount WHEN emitted THEN shown without fetching a rate`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(gatedCampaign(1)).right()
        val amount = MarketingBannerAmount.Fiat(value = java.math.BigDecimal("100"), currencyCode = "USD")
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen, amount))),
        )

        // Act
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)
        coVerify(exactly = 0) { quotesRepository.getFiatUSDRate(any()) }
    }

    @Test
    fun `GIVEN rate unavailable WHEN fiat amount emitted THEN only unbounded campaigns shown`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(eurScreen, null) } returns listOf(
            gatedCampaign(1),
            campaign(2, MarketingBanner.UiType.STANDALONE),
        ).right()
        coEvery { quotesRepository.getFiatUSDRate("EUR") } returns null
        val requests = MutableStateFlow<MarketingBannerRequest?>(
            MarketingBannerRequest(eurScreen, amount = eurAmount("100")),
        )
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))

        // Act
        advanceUntilIdle()
        requests.value = MarketingBannerRequest(eurScreen, amount = eurAmount("200"))
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(2)
        coVerify(exactly = 2) { quotesRepository.getFiatUSDRate("EUR") }
    }

    @Test
    fun `GIVEN slow rate lookup WHEN amount changes meanwhile THEN lookup not restarted`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(eurScreen, null) } returns listOf(gatedCampaign(1)).right()
        coEvery { quotesRepository.getFiatUSDRate("EUR") } coAnswers {
            delay(1_000)
            java.math.BigDecimal("1.1")
        }
        val requests = MutableStateFlow<MarketingBannerRequest?>(
            MarketingBannerRequest(eurScreen, amount = eurAmount("10")),
        )
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))
        advanceTimeBy(500) // past the request debounce, lookup in flight

        // Act
        requests.value = MarketingBannerRequest(eurScreen, amount = eurAmount("100"))
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)
        coVerify(exactly = 1) { quotesRepository.getFiatUSDRate("EUR") }
    }

    @Test
    fun `GIVEN lookup fails after amount cleared WHEN amount retyped THEN rate fetched again`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(eurScreen, null) } returns listOf(gatedCampaign(1)).right()
        coEvery { quotesRepository.getFiatUSDRate("EUR") } coAnswers {
            delay(1_000)
            null
        } andThen java.math.BigDecimal("1.1")
        val requests = MutableStateFlow<MarketingBannerRequest?>(
            MarketingBannerRequest(eurScreen, amount = eurAmount("100")),
        )
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))
        advanceTimeBy(500) // past the request debounce, lookup in flight
        requests.value = MarketingBannerRequest(eurScreen, amount = null)
        advanceUntilIdle() // the unawaited lookup fails

        // Act
        requests.value = MarketingBannerRequest(eurScreen, amount = eurAmount("100"))
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)
        coVerify(exactly = 2) { quotesRepository.getFiatUSDRate("EUR") }
    }

    @Test
    fun `GIVEN shown banner WHEN screen changes THEN hidden until new campaigns fetched`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(1, MarketingBanner.UiType.STANDALONE),
        ).right()
        coEvery { getMarketingBanner(eurScreen, null) } coAnswers {
            delay(1_000)
            listOf(campaign(2, MarketingBanner.UiType.STANDALONE)).right()
        }
        val requests = MutableStateFlow<MarketingBannerRequest?>(MarketingBannerRequest(onrampScreen))
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))
        advanceUntilIdle()
        val stateBefore = model.uiState.value

        // Act
        requests.value = MarketingBannerRequest(eurScreen)
        runCurrent()
        val stateWhileFetching = model.uiState.value
        advanceUntilIdle()

        // Assert
        assertThat((stateBefore as MarketingBannerListUM.Content).banners.map { it.campaignId }).containsExactly(1)
        assertThat(stateWhileFetching).isEqualTo(MarketingBannerListUM.Hidden)
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(2)
    }

    @Test
    fun `GIVEN shown bounded banner WHEN new amount awaits conversion THEN hidden until converted`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(eurScreen, null) } returns listOf(gatedCampaign(1)).right()
        coEvery { quotesRepository.getFiatUSDRate("EUR") } coAnswers {
            delay(1_000)
            java.math.BigDecimal("1.1")
        }
        val requests = MutableStateFlow<MarketingBannerRequest?>(MarketingBannerRequest(eurScreen, usdAmount(100)))
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))
        advanceUntilIdle()
        val stateBefore = model.uiState.value

        // Act
        requests.value = MarketingBannerRequest(eurScreen, amount = eurAmount("100"))
        runCurrent()
        val stateWhileConverting = model.uiState.value
        advanceUntilIdle()

        // Assert
        assertThat((stateBefore as MarketingBannerListUM.Content).banners.map { it.campaignId }).containsExactly(1)
        assertThat(stateWhileConverting).isEqualTo(MarketingBannerListUM.Hidden)
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)
    }

    @Test
    fun `GIVEN bounds only on other ui type WHEN fiat amount emitted THEN rate not fetched`() = runTest {
        // Arrange
        val boundedLinked = gatedCampaign(2).let { base ->
            base.copy(banner = base.banner.copy(uiType = MarketingBanner.UiType.LINKED_TO_PROVIDER))
        }
        coEvery { getMarketingBanner(eurScreen, null) } returns listOf(
            campaign(1, MarketingBanner.UiType.STANDALONE),
            boundedLinked,
        ).right()
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(
                flowOf(MarketingBannerRequest(eurScreen, amount = eurAmount("100"))),
            ),
        )

        // Act
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)
        coVerify(exactly = 0) { quotesRepository.getFiatUSDRate(any()) }
    }

    @Test
    fun `GIVEN standalone campaigns WHEN request emitted THEN only STANDALONE banners shown`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(1, MarketingBanner.UiType.STANDALONE),
            campaign(2, MarketingBanner.UiType.LINKED_TO_PROVIDER),
        ).right()
        val params = MarketingBannerComponent.Params.Standalone(
            requestFlow = flowOf(MarketingBannerRequest(onrampScreen, amount = null)),
        )
        val model = createModel(params)

        // Act + Assert
        model.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(MarketingBannerListUM.Content::class.java)
            val content = state as MarketingBannerListUM.Content
            assertThat(content.banners.map { it.campaignId }).containsExactly(1)
        }
    }

    @Test
    fun `GIVEN empty result WHEN request emitted THEN Hidden`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns emptyList<MarketingCampaign>().right()
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))),
        )

        // Act + Assert
        model.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem()).isEqualTo(MarketingBannerListUM.Hidden)
        }
    }

    @Test
    fun `GIVEN campaign fetch failed WHEN same screen updates arrive THEN not fetched again`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns RuntimeException("404").left()
        val requests = MutableStateFlow<MarketingBannerRequest?>(MarketingBannerRequest(onrampScreen))
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))
        advanceUntilIdle()

        // Act
        repeat(10) { step ->
            requests.value = MarketingBannerRequest(onrampScreen, amount = usdAmount(step + 1))
            advanceUntilIdle()
        }

        // Assert
        assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)
        coVerify(exactly = 1) { getMarketingBanner(onrampScreen, null) }
    }

    @Test
    fun `GIVEN use case fails WHEN request emitted THEN Hidden`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns RuntimeException("boom").left()
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))),
        )

        // Act + Assert
        model.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem()).isEqualTo(MarketingBannerListUM.Hidden)
        }
    }

    @Test
    fun `GIVEN linked campaigns WHEN request emitted THEN all LINKED banners shown with providerIds`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(1, MarketingBanner.UiType.LINKED_TO_PROVIDER, providerIds = listOf("mercuryo")),
            campaign(2, MarketingBanner.UiType.LINKED_TO_PROVIDER, providerIds = listOf("moonpay")),
            campaign(3, MarketingBanner.UiType.STANDALONE),
        ).right()
        val model = createModel(
            MarketingBannerComponent.Params.Linked(
                flowOf(LinkedBannerRequest(onrampScreen, amount = null)),
            ),
        )

        // Act + Assert
        // Model no longer filters by provider: it emits all LINKED banners (not STANDALONE), carrying their
        // providerIds; per-offer provider matching happens at render time in LinkedContent(providerId).
        model.uiState.test {
            advanceUntilIdle()
            val content = expectMostRecentItem() as MarketingBannerListUM.Content
            assertThat(content.banners.map { it.campaignId }).containsExactly(1, 2)
            assertThat(content.banners.first { it.campaignId == 1 }.providerIds).containsExactly("mercuryo")
            assertThat(content.banners.first { it.campaignId == 2 }.providerIds).containsExactly("moonpay")
        }
    }

    @Test
    fun `GIVEN shown banner WHEN dismissed THEN removed from state and use case called`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(1, MarketingBanner.UiType.STANDALONE),
        ).right()
        coEvery { dismissMarketingBanner(1) } returns Unit.right()
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))),
        )

        // Act
        advanceUntilIdle()
        model.onDismiss(campaignId = 1)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)
        coVerify(exactly = 1) { dismissMarketingBanner(1) }
    }

    @Test
    fun `GIVEN non-blank deeplink WHEN clicked THEN launcher called`() = runTest {
        // Arrange
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(MutableStateFlow(null)),
        )

        // Act
        model.onBannerClick("tangem://promo/1")

        // Assert
        verify(exactly = 1) { deeplinkLauncher.launch("tangem://promo/1") }
    }

    @Test
    fun `GIVEN blank deeplink WHEN clicked THEN launcher not called`() = runTest {
        val model = createModel(MarketingBannerComponent.Params.Standalone(MutableStateFlow(null)))

        model.onBannerClick(null)
        model.onBannerClick("")

        verify(exactly = 0) { deeplinkLauncher.launch(any()) }
    }

    @Test
    fun `GIVEN host handles deeplink WHEN clicked THEN launcher not called`() = runTest {
        // Arrange
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(
                requestFlow = MutableStateFlow(null),
                onDeeplinkClick = { true },
            ),
        )

        // Act
        model.onBannerClick("tangem://swap")

        // Assert
        verify(exactly = 0) { deeplinkLauncher.launch(any()) }
    }

    @Test
    fun `GIVEN host does not handle deeplink WHEN clicked THEN launcher called`() = runTest {
        // Arrange
        val model = createModel(
            MarketingBannerComponent.Params.Standalone(
                requestFlow = MutableStateFlow(null),
                onDeeplinkClick = { false },
            ),
        )

        // Act
        model.onBannerClick("https://tangem.com/promo")

        // Assert
        verify(exactly = 1) { deeplinkLauncher.launch("https://tangem.com/promo") }
        coVerify(exactly = 0) { marketingUidBindingRepository.bind(any()) }
    }

    @ParameterizedTest
    @MethodSource("provideHostHandledUidModels")
    fun `GIVEN host handles deeplink WHEN clicked THEN the first non-empty uid is bound`(model: HostHandledUidModel) =
        runTest {
            // Arrange
            every { Uri.parse(HOST_HANDLED_LINK) } returns uriWithUids(*model.uids.toTypedArray())
            val bannerModel = createModel(
                MarketingBannerComponent.Params.Standalone(
                    requestFlow = MutableStateFlow(null),
                    onDeeplinkClick = { true },
                ),
            )

            // Act
            bannerModel.onBannerClick(HOST_HANDLED_LINK)

            // Assert
            if (model.expectedUid != null) {
                coVerify(exactly = 1) { marketingUidBindingRepository.bind(model.expectedUid) }
            } else {
                coVerify(exactly = 0) { marketingUidBindingRepository.bind(any()) }
            }
        }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UiTypeSurfaces {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN campaigns of every uiType WHEN surface requested THEN only its uiTypes shown in priority order`(
            model: SurfaceModel,
        ) = runTest {
            // Arrange
            coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
                campaign(id = 1, uiType = MarketingBanner.UiType.STANDALONE),
                campaign(id = 2, uiType = MarketingBanner.UiType.STANDALONE_CTA),
                campaign(id = 3, uiType = MarketingBanner.UiType.LINKED_TO_PROVIDER),
                campaign(id = 4, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER),
                campaign(id = 5, uiType = MarketingBanner.UiType.STANDALONE_CTA, deeplink = WAITLIST_LINK),
            ).right()
            val bannerModel = createModel(model.params)

            // Act
            advanceUntilIdle()

            // Assert
            val content = bannerModel.uiState.value as MarketingBannerListUM.Content
            assertThat(content.banners.map { it.campaignId }).containsExactlyElementsIn(model.expectedIds).inOrder()
        }

        private fun provideTestModels() = listOf(
            SurfaceModel(
                params = MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))),
                expectedIds = listOf(1),
            ),
            SurfaceModel(
                params = MarketingBannerComponent.Params.Cta(flowOf(ctaRequest(isDemandSurveyCompleted = false))),
                expectedIds = listOf(2, 4, 5),
            ),
            SurfaceModel(
                params = MarketingBannerComponent.Params.Cta(flowOf(ctaRequest(isDemandSurveyCompleted = true))),
                expectedIds = listOf(2, 4),
            ),
        )
    }

    internal data class SurfaceModel(
        val params: MarketingBannerComponent.Params,
        val expectedIds: List<Int>,
    )

    @Test
    fun `GIVEN survey completed WHEN cta banners shown THEN only banners with the waitlist link hidden`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(id = 1, uiType = MarketingBanner.UiType.STANDALONE_CTA, deeplink = WAITLIST_LINK),
            campaign(id = 2, uiType = MarketingBanner.UiType.STANDALONE_CTA, deeplink = "tangem://support"),
            campaign(id = 3, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = "https://partner.example"),
            campaign(id = 4, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = WAITLIST_LINK),
        ).right()

        // Act
        val bannerModel = createModel(
            MarketingBannerComponent.Params.Cta(flowOf(ctaRequest(isDemandSurveyCompleted = true))),
        )
        advanceUntilIdle()

        // Assert
        assertThat(bannerModel.uiState.value.shownIds()).containsExactly(2, 3).inOrder()
    }

    @Test
    fun `GIVEN waitlist strip shown WHEN demand survey completed THEN strip hidden without re-fetch`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(id = 1, uiType = MarketingBanner.UiType.STANDALONE_CTA, deeplink = WAITLIST_LINK),
        ).right()
        val requests = MutableStateFlow<CtaBannerRequest?>(ctaRequest(isDemandSurveyCompleted = false))
        val bannerModel = createModel(MarketingBannerComponent.Params.Cta(requests))
        advanceUntilIdle()

        // Act
        requests.value = ctaRequest(isDemandSurveyCompleted = true)
        advanceUntilIdle()

        // Assert
        assertThat(bannerModel.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)
        coVerify(exactly = 1) { getMarketingBanner(onrampScreen, null) }
    }

    @Test
    fun `GIVEN survey completed WHEN request becomes null THEN rows hidden before the debounce ends`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(id = 1, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER),
            campaign(id = 2, uiType = MarketingBanner.UiType.STANDALONE_CTA),
        ).right()
        val requests = MutableStateFlow<CtaBannerRequest?>(ctaRequest(isDemandSurveyCompleted = true))
        val bannerModel = createModel(MarketingBannerComponent.Params.Cta(requests))
        advanceUntilIdle()

        // Act
        requests.value = null
        runCurrent()

        // Assert
        assertThat(bannerModel.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)
    }

    @Test
    fun `GIVEN cta campaigns WHEN shown THEN only banners whose link Android opens remain`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(id = 1, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = "tangem://support"),
            campaign(id = 2, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = "https://partner.example"),
            campaign(
                id = 3,
                uiType = MarketingBanner.UiType.STANDALONE_CTA,
                deeplink = "tangem://onramp?type=concierge_waitlist",
            ),
            campaign(id = 4, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = "tangem://"),
            campaign(id = 5, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = "http://partner.example"),
            campaign(id = 6, uiType = MarketingBanner.UiType.STANDALONE_CTA, deeplink = null),
            campaign(id = 7, uiType = MarketingBanner.UiType.STANDALONE_CTA, deeplink = "tangem://swap"),
        ).right()

        // Act
        val bannerModel = createModel(
            MarketingBannerComponent.Params.Cta(flowOf(ctaRequest(isDemandSurveyCompleted = false))),
        )
        advanceUntilIdle()

        // Assert
        val content = bannerModel.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1, 2, 3, 7).inOrder()
    }

    @Test
    fun `GIVEN cta partner link WHEN clicked THEN opened in the browser without the launcher`() = runTest {
        // Arrange
        val link = "https://partner.example/otc"
        val model = createCtaModelShowing(link)

        // Act
        model.onBannerClick(link)

        // Assert
        verify(exactly = 1) { urlOpener.openUrl(link) }
        verify(exactly = 0) { deeplinkLauncher.launch(any(), any()) }
    }

    @Test
    fun `GIVEN cta partner link with uid WHEN clicked THEN uid bound, as the page never reaches MainActivity`() =
        runTest {
            // Arrange
            val link = "https://partner.example/otc?uid=abc"
            every { Uri.parse(link) } returns uriWithUids("abc")
            val model = createCtaModelShowing(link)

            // Act
            model.onBannerClick(link)

            // Assert
            coVerify(exactly = 1) { marketingUidBindingRepository.bind("abc") }
            verify(exactly = 1) { urlOpener.openUrl(link) }
        }

    @Test
    fun `GIVEN cta link with surrounding whitespace WHEN shown and clicked THEN opened as the trimmed link`() =
        runTest {
            // Arrange
            coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
                campaign(
                    id = 1,
                    uiType = MarketingBanner.UiType.PSEUDO_PROVIDER,
                    deeplink = " https://partner.example ",
                ),
            ).right()
            val model = createModel(
                MarketingBannerComponent.Params.Cta(flowOf(ctaRequest(isDemandSurveyCompleted = false))),
            )
            advanceUntilIdle()

            // Act
            model.onBannerClick(" https://partner.example ")

            // Assert
            assertThat(model.uiState.value.shownIds()).containsExactly(1)
            verify(exactly = 1) { urlOpener.openUrl("https://partner.example") }
        }

    @Test
    fun `GIVEN support chat cta banner WHEN fetched THEN shown alongside other banners`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(id = 1, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = "tangem://support"),
            campaign(id = 2, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = "https://partner.example"),
        ).right()

        // Act
        val model = createModel(
            MarketingBannerComponent.Params.Cta(flowOf(ctaRequest(isDemandSurveyCompleted = false))),
        )
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.shownIds()).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `GIVEN cta in-app link WHEN clicked THEN launcher routes it`() = runTest {
        // Arrange
        val model = createModel(MarketingBannerComponent.Params.Cta(MutableStateFlow(null)))

        // Act
        model.onBannerClick("tangem://support")

        // Assert
        verify(exactly = 1) { deeplinkLauncher.launch("tangem://support") }
        verify(exactly = 0) { urlOpener.openUrl(any()) }
    }

    @Test
    fun `GIVEN standalone https link WHEN clicked THEN launcher keeps the trusted-host check`() = runTest {
        // Arrange
        val model = createModel(MarketingBannerComponent.Params.Standalone(MutableStateFlow(null)))

        // Act
        model.onBannerClick("https://partner.example/otc")

        // Assert
        verify(exactly = 1) { deeplinkLauncher.launch("https://partner.example/otc") }
        verify(exactly = 0) { urlOpener.openUrl(any()) }
    }

    @Test
    fun `GIVEN rate lookup pending WHEN demand survey completed THEN strip hidden at once and not shown again`() =
        runTest {
            // Arrange
            coEvery { getMarketingBanner(eurScreen, null) } returns listOf(
                campaign(id = 1, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, minAmount = java.math.BigDecimal(50)),
                campaign(id = 2, uiType = MarketingBanner.UiType.STANDALONE_CTA, deeplink = WAITLIST_LINK),
            ).right()
            coEvery { quotesRepository.getFiatUSDRate("EUR") } coAnswers {
                delay(1_000)
                java.math.BigDecimal("1.1")
            }
            val requests = MutableStateFlow<CtaBannerRequest?>(
                ctaRequest(isDemandSurveyCompleted = false, screen = eurScreen, amount = eurAmount("100")),
            )
            val model = createModel(MarketingBannerComponent.Params.Cta(requests))
            val shownIds = getEmittedValues(model.uiState.map { it.shownIds() })
            advanceTimeBy(500)
            runCurrent()
            val idsBefore = shownIds.last()
            val emittedBefore = shownIds.size

            // Act
            requests.value = ctaRequest(isDemandSurveyCompleted = true, screen = eurScreen, amount = eurAmount("100"))
            runCurrent()
            val idsRightAfterCompletion = shownIds.last()
            advanceUntilIdle()

            // Assert
            assertThat(idsBefore).containsExactly(2)
            assertThat(idsRightAfterCompletion).isEmpty()
            assertThat(shownIds.drop(emittedBefore)).containsExactly(emptyList<Int>(), listOf(1)).inOrder()
        }

    @Test
    fun `GIVEN bounded cta shown WHEN request cleared and back out of bounds THEN hidden at once and never stale`() =
        runTest {
            // Arrange
            coEvery { getMarketingBanner(eurScreen, null) } returns listOf(
                campaign(id = 1, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, minAmount = java.math.BigDecimal(50)),
                campaign(id = 2, uiType = MarketingBanner.UiType.STANDALONE_CTA),
            ).right()
            coEvery { quotesRepository.getFiatUSDRate("EUR") } returns java.math.BigDecimal("1.1")
            val requests = MutableStateFlow<CtaBannerRequest?>(
                ctaRequest(isDemandSurveyCompleted = false, screen = eurScreen, amount = eurAmount("100")),
            )
            val model = createModel(MarketingBannerComponent.Params.Cta(requests))
            val shownIds = getEmittedValues(model.uiState.map { it.shownIds() })
            advanceUntilIdle()
            val idsBefore = shownIds.last()
            val emittedBefore = shownIds.size

            // Act
            requests.value = null
            runCurrent()
            val idsWhileCleared = shownIds.last()
            requests.value = ctaRequest(isDemandSurveyCompleted = false, screen = eurScreen, amount = eurAmount("10"))
            advanceUntilIdle()

            // Assert
            assertThat(idsBefore).containsExactly(1, 2).inOrder()
            assertThat(idsWhileCleared).isEmpty()
            assertThat(shownIds.drop(emittedBefore)).containsExactly(emptyList<Int>(), listOf(2)).inOrder()
        }

    @Test
    fun `GIVEN banners shown WHEN request cleared and back on the same screen THEN shown again without refetch`() =
        runTest {
            // Arrange
            coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
                campaign(id = 1, uiType = MarketingBanner.UiType.STANDALONE),
            ).right()
            val requests = MutableStateFlow<MarketingBannerRequest?>(MarketingBannerRequest(onrampScreen))
            val model = createModel(MarketingBannerComponent.Params.Standalone(requests))
            advanceUntilIdle()

            // Act
            requests.value = null
            runCurrent()
            val stateWhileCleared = model.uiState.value
            requests.value = MarketingBannerRequest(onrampScreen)
            runCurrent()

            // Assert
            assertThat(stateWhileCleared).isEqualTo(MarketingBannerListUM.Hidden)
            assertThat(model.uiState.value.shownIds()).containsExactly(1)
            coVerify(exactly = 1) { getMarketingBanner(onrampScreen, null) }
        }

    private fun MarketingBannerListUM.shownIds(): List<Int> =
        (this as? MarketingBannerListUM.Content)?.banners?.map { it.campaignId }.orEmpty()

    private fun TestScope.createCtaModelShowing(link: String): MarketingBannerModel {
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(id = 1, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER, deeplink = link),
        ).right()
        val model = createModel(
            MarketingBannerComponent.Params.Cta(flowOf(ctaRequest(isDemandSurveyCompleted = false))),
        )
        advanceUntilIdle()
        return model
    }

    private fun ctaRequest(
        isDemandSurveyCompleted: Boolean,
        screen: MarketingScreen.Onramp = onrampScreen,
        amount: MarketingBannerAmount? = null,
    ) = CtaBannerRequest(
        screen = screen,
        amount = amount,
        isDemandSurveyCompleted = isDemandSurveyCompleted,
    )

    @Test
    fun `GIVEN pseudo_provider campaign WHEN mapped THEN all banner fields reach the UM`() = runTest {
        // Arrange
        val base = campaign(id = 1, uiType = MarketingBanner.UiType.PSEUDO_PROVIDER)
        val campaign = base.copy(
            banner = base.banner.copy(
                deeplink = "tangem://support",
                isDismissible = false,
                categoryLabel = "Concierge for large purchases",
                subtitle = "Pay by bank transfer",
                etaLabel = "up to 4 hours",
                partnerLabel = "via MoonPay",
                buttonText = "Request",
                hasGlow = true,
            ),
        )
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(campaign).right()
        val bannerModel = createModel(
            MarketingBannerComponent.Params.Cta(flowOf(ctaRequest(isDemandSurveyCompleted = false))),
        )

        // Act
        advanceUntilIdle()

        // Assert
        val expected = MarketingBannerUM(
            campaignId = 1,
            text = "text-1",
            iconUrl = null,
            iconAlign = MarketingBannerUM.IconAlign.RIGHT,
            isDismissible = false,
            deeplink = "tangem://support",
            categoryLabel = "Concierge for large purchases",
            subtitle = "Pay by bank transfer",
            etaLabel = "up to 4 hours",
            partnerLabel = "via MoonPay",
            buttonText = "Request",
            hasGlow = true,
            style = MarketingBannerUM.Style.PSEUDO_PROVIDER,
        )
        assertThat(bannerModel.uiState.value).isEqualTo(MarketingBannerListUM.Content(persistentListOf(expected)))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN iconAlign and dismissible WHEN mapped THEN align follows design default`(
        model: IconAlignModel,
    ) = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            standaloneCampaign(id = 1, iconAlign = model.iconAlign, isDismissible = model.isDismissible),
        ).right()
        val bannerModel = createModel(
            MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))),
        )

        // Act
        advanceUntilIdle()

        // Assert
        val content = bannerModel.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.single().iconAlign).isEqualTo(model.expected)
    }

    private fun standaloneCampaign(id: Int, iconAlign: MarketingBanner.IconAlign?, isDismissible: Boolean) =
        campaign(id, MarketingBanner.UiType.STANDALONE).let { base ->
            base.copy(banner = base.banner.copy(iconAlign = iconAlign, isDismissible = isDismissible))
        }

    internal data class IconAlignModel(
        val iconAlign: MarketingBanner.IconAlign?,
        val isDismissible: Boolean,
        val expected: MarketingBannerUM.IconAlign,
    )

    private fun provideTestModels() = listOf(
        // Backend omits iconAlign -> derived from dismissible (design default)
        IconAlignModel(iconAlign = null, isDismissible = false, expected = MarketingBannerUM.IconAlign.RIGHT),
        IconAlignModel(iconAlign = null, isDismissible = true, expected = MarketingBannerUM.IconAlign.LEFT),
        // Explicit backend value is always honored regardless of dismissible
        IconAlignModel(
            iconAlign = MarketingBanner.IconAlign.LEFT,
            isDismissible = false,
            expected = MarketingBannerUM.IconAlign.LEFT,
        ),
        IconAlignModel(
            iconAlign = MarketingBanner.IconAlign.RIGHT,
            isDismissible = true,
            expected = MarketingBannerUM.IconAlign.RIGHT,
        ),
    )

    internal data class HostHandledUidModel(val uids: List<String>, val expectedUid: String?)

    private fun provideHostHandledUidModels() = listOf(
        HostHandledUidModel(uids = listOf("", "spring-campaign", "other"), expectedUid = "spring-campaign"),
        HostHandledUidModel(uids = listOf(""), expectedUid = null),
        HostHandledUidModel(uids = emptyList(), expectedUid = null),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CampaignWindows {

        @Test
        fun `GIVEN changing fetch responses WHEN campaign expires THEN one fetch supplies display and timer`() = runTest {
            // Arrange
            val activeCampaign = campaign(
                id = 1,
                uiType = MarketingBanner.UiType.STANDALONE,
                endAt = "2026-03-30T10:01:00Z",
            )
            coEvery { getMarketingBanner(onrampScreen, null) } returnsMany listOf(
                listOf(activeCampaign).right(),
                emptyList<MarketingCampaign>().right(),
            )
            val model = createModel(
                MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))),
            )
            advanceTimeBy(300)
            runCurrent()
            val beforeExpiry = model.uiState.value.shownIds()

            // Act
            advanceTimeBy(59_701)
            runCurrent()

            // Assert
            assertThat(beforeExpiry).containsExactly(1)
            assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)
            coVerify(exactly = 1) { getMarketingBanner(onrampScreen, null) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN future campaign WHEN startAt reached THEN campaign appears dynamically`() = runTest {
            // Arrange
            val startAt = "2026-03-30T10:01:00Z"
            coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
                campaign(id = 1, uiType = MarketingBanner.UiType.STANDALONE, startAt = startAt),
            ).right()
            val model = createModel(MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))))
            advanceTimeBy(300)
            runCurrent()

            // Act & Assert
            assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)

            advanceTimeBy(59_699)
            runCurrent()
            assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)

            advanceTimeBy(1)
            runCurrent()
            assertThat(model.uiState.value.shownIds()).containsExactly(1)
        }

        @Test
        fun `GIVEN active campaign with endAt WHEN endAt passes THEN campaign hidden dynamically`() = runTest {
            // Arrange
            val endAt = "2026-03-30T10:01:00Z"
            coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
                campaign(id = 1, uiType = MarketingBanner.UiType.STANDALONE, endAt = endAt),
            ).right()
            val model = createModel(MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))))
            advanceTimeBy(300)
            runCurrent()

            // Act & Assert
            assertThat(model.uiState.value.shownIds()).containsExactly(1)

            advanceTimeBy(59_700)
            runCurrent()
            assertThat(model.uiState.value.shownIds()).containsExactly(1)

            advanceTimeBy(1)
            runCurrent()
            assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)
        }

        @Test
        fun `GIVEN campaigns with future and active bounds WHEN transitions occur THEN uiState updates at each boundary`() =
            runTest {
                // Arrange
                val campaign1 = campaign(
                    id = 1,
                    uiType = MarketingBanner.UiType.STANDALONE,
                    endAt = "2026-03-30T10:01:00Z",
                )
                val campaign2 = campaign(
                    id = 2,
                    uiType = MarketingBanner.UiType.STANDALONE,
                    startAt = "2026-03-30T10:01:30Z",
                    endAt = "2026-03-30T10:02:00Z",
                )
                coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(campaign1, campaign2).right()
                val model = createModel(
                    MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))),
                )
                advanceTimeBy(300)
                runCurrent()

                // Act & Assert
                assertThat(model.uiState.value.shownIds()).containsExactly(1)

                advanceTimeBy(59_701)
                runCurrent()
                assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)

                advanceTimeBy(29_999)
                runCurrent()
                assertThat(model.uiState.value.shownIds()).containsExactly(2)

                advanceTimeBy(30_001)
                runCurrent()
                assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)
            }

        @Test
        fun `GIVEN future campaign WHEN onResume called after system clock jumps past startAt THEN banner shown immediately`() =
            runTest {
                // Arrange
                val clock = MutableClock(current = BASE_TEST_INSTANT)
                val startAt = "2026-03-30T11:00:00Z"
                coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
                    campaign(id = 1, uiType = MarketingBanner.UiType.STANDALONE, startAt = startAt),
                ).right()
                val model = createModel(
                    params = MarketingBannerComponent.Params.Standalone(flowOf(MarketingBannerRequest(onrampScreen))),
                    clock = clock,
                )
                advanceTimeBy(300)
                runCurrent()
                assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)

                // Act
                clock.current = Instant.parse("2026-03-30T11:05:00Z")
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.shownIds()).containsExactly(1)
            }

        @Test
        fun `GIVEN pending boundary delay WHEN screen changes THEN timer cancelled and new screen campaigns observed`() =
            runTest {
                // Arrange
                val futureCampaign = campaign(
                    id = 1,
                    uiType = MarketingBanner.UiType.STANDALONE,
                    startAt = "2026-03-30T10:05:00Z",
                )
                val currentCampaign = campaign(
                    id = 2,
                    uiType = MarketingBanner.UiType.STANDALONE,
                )
                val requests = MutableStateFlow<MarketingBannerRequest?>(MarketingBannerRequest(onrampScreen))
                coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(futureCampaign).right()
                coEvery { getMarketingBanner(eurScreen, null) } returns listOf(currentCampaign).right()

                val model = createModel(MarketingBannerComponent.Params.Standalone(requests))
                advanceTimeBy(300)
                runCurrent()
                assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden)

                // Act
                requests.value = MarketingBannerRequest(eurScreen)
                advanceTimeBy(300)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.shownIds()).containsExactly(2)
            }
    }

    private companion object {
        val BASE_TEST_INSTANT: Instant = Instant.parse("2026-03-30T10:00:00Z")
        const val HOST_HANDLED_LINK = "tangem://buy"
        const val WAITLIST_LINK = "tangem://onramp?type=concierge_waitlist"
    }
}