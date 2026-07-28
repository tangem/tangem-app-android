package com.tangem.features.marketing.impl.model

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.domain.marketing.DismissMarketingBannerUseCase
import com.tangem.domain.marketing.GetMarketingBannerUseCase
import com.tangem.domain.marketing.models.MarketingBanner
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.MarketingScreenType
import com.tangem.features.marketing.api.LinkedBannerRequest
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.marketing.api.MarketingBannerRequest
import com.tangem.features.marketing.impl.ui.state.MarketingBannerListUM
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketingBannerModelTest {

    private val getMarketingBanner: GetMarketingBannerUseCase = mockk()
    private val dismissMarketingBanner: DismissMarketingBannerUseCase = mockk()
    private val deeplinkLauncher: DeeplinkLauncher = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(getMarketingBanner, dismissMarketingBanner, deeplinkLauncher)
    }

    private fun TestScope.createModel(params: MarketingBannerComponent.Params): MarketingBannerModel {
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
        )
    }

    private fun campaign(id: Int, uiType: MarketingBanner.UiType, providerIds: List<String>? = null) =
        MarketingCampaign(
            id = id,
            type = MarketingScreenType.ONRAMP,
            priority = id,
            startAt = null,
            endAt = null,
            minAmount = null,
            maxAmount = null,
            providerIds = providerIds,
            banner = MarketingBanner(
                uiType = uiType,
                text = "text-$id",
                iconUrl = null,
                iconAlign = null,
                bgColor = null,
                deeplink = "tangem://promo/$id",
                isDismissible = true,
            ),
            targets = emptyList(),
        )

    private val onrampScreen = MarketingScreen.Onramp("USD", "ethereum", "0xabc")

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
            MarketingBannerRequest(screen, amountUsd = java.math.BigDecimal(10)), // below min -> hidden
        )
        val model = createModel(MarketingBannerComponent.Params.Standalone(requests))

        // Act + Assert
        advanceUntilIdle()
        assertThat(model.uiState.value).isEqualTo(MarketingBannerListUM.Hidden) // 10 < 50

        requests.value = MarketingBannerRequest(screen, amountUsd = java.math.BigDecimal(100)) // in range
        advanceUntilIdle()
        val content = model.uiState.value as MarketingBannerListUM.Content
        assertThat(content.banners.map { it.campaignId }).containsExactly(1)

        // fetched once for the pair, despite two different amounts
        coVerify(exactly = 1) { getMarketingBanner(screen, null) }
    }

    @Test
    fun `GIVEN standalone campaigns WHEN request emitted THEN only STANDALONE banners shown`() = runTest {
        // Arrange
        coEvery { getMarketingBanner(onrampScreen, null) } returns listOf(
            campaign(1, MarketingBanner.UiType.STANDALONE),
            campaign(2, MarketingBanner.UiType.LINKED_TO_PROVIDER),
        ).right()
        val params = MarketingBannerComponent.Params.Standalone(
            requestFlow = flowOf(MarketingBannerRequest(onrampScreen, amountUsd = null)),
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
                flowOf(LinkedBannerRequest(onrampScreen, amountUsd = null)),
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
}