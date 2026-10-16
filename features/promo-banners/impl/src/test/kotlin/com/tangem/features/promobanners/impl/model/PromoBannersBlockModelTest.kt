package com.tangem.features.promobanners.impl.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.usecase.GetPromoCampaignsUseCase
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.promobanners.api.PromoBannersBlockComponent.Placeholder
import com.tangem.features.promobanners.api.PromoBannersFeatureToggles
import com.tangem.features.promobanners.impl.analytics.PromoBannerAnalyticsEvent
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoBannerCampaignMatcher
import com.tangem.features.promobanners.impl.repository.PromoBannersRepository
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PromoBannersBlockModelTest {

    private val repository: PromoBannersRepository = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val promoBannersFeatureToggles: PromoBannersFeatureToggles = mockk()
    private val getPromoCampaignsUseCase: GetPromoCampaignsUseCase = mockk()
    private val campaignsService: CampaignsService = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val selectedWallet: UserWallet = mockk { every { walletId } returns UserWalletId(SELECTED_WALLET_ID) }
    private val otherWallet: UserWallet = mockk { every { walletId } returns UserWalletId(OTHER_WALLET_ID) }

    private val plainBanner = createBanner(id = 1, campaignId = null)
    private val openCampaignBanner = createBanner(id = 2, campaignId = OPEN_CAMPAIGN_ID)
    private val enrolledCampaignBanner = createBanner(id = 3, campaignId = ENROLLED_CAMPAIGN_ID)
    private val allBanners = listOf(plainBanner, openCampaignBanner, enrolledCampaignBanner)

    private val openCampaign = createPromoCampaign(id = OPEN_CAMPAIGN_ID, name = OPEN_CAMPAIGN_NAME)
    private val enrolledCampaign = createPromoCampaign(id = ENROLLED_CAMPAIGN_ID, isAlreadyEnrolled = true)
    private val campaigns = listOf(openCampaign, enrolledCampaign)

    @BeforeEach
    fun setup() {
        clearMocks(
            repository,
            userWalletsListRepository,
            promoBannersFeatureToggles,
            getPromoCampaignsUseCase,
            campaignsService,
            analyticsEventHandler,
        )
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(selectedWallet)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(selectedWallet, otherWallet))
        coEvery { repository.getCachedBanners(any(), any()) } returns null
        coEvery { repository.getBannersSync(any(), any(), any()) } returns allBanners
        every { repository.getBanners(any(), any()) } returns emptyFlow()
        coEvery { getPromoCampaignsUseCase.invokeSync(any(), any()) } returns Either.Right(campaigns)
        every { getPromoCampaignsUseCase(any()) } returns emptyFlow()
    }

    @Test
    fun `GIVEN v2 enabled WHEN model created THEN campaigns loaded once per wallet and enrolled banner hidden`() =
        runTest {
            // Arrange
            every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true

            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.banners.map { it.displayId })
                .containsExactly(plainBanner.id, openCampaignBanner.id).inOrder()
            coVerify(exactly = 1) { getPromoCampaignsUseCase.invokeSync(selectedWallet, any()) }
            coVerify(exactly = 1) { getPromoCampaignsUseCase.invokeSync(otherWallet, any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN banners shown WHEN banners cache refreshed THEN block shows the refreshed banners`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns false
        val cachedBanners = MutableStateFlow(allBanners)
        every { repository.getBanners(SELECTED_WALLET_ID, any()) } returns cachedBanners
        val model = createModel()
        advanceUntilIdle()
        assertThat(model.uiState.value.banners.map { it.displayId })
            .containsExactlyElementsIn(allBanners.map { it.id }).inOrder()

        // Act
        cachedBanners.value = listOf(plainBanner)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.banners.map { it.displayId }).containsExactly(plainBanner.id)
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled and unchanged banners WHEN campaigns cache drops a campaign THEN its banner hidden`() =
        runTest {
            // Arrange
            every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
            val cachedCampaigns = MutableStateFlow(campaigns)
            every { getPromoCampaignsUseCase(selectedWallet) } returns cachedCampaigns
            val model = createModel()
            advanceUntilIdle()
            assertThat(model.uiState.value.banners.map { it.displayId })
                .containsExactly(plainBanner.id, openCampaignBanner.id).inOrder()

            // Act
            cachedCampaigns.value = listOf(enrolledCampaign)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.banners.map { it.displayId }).containsExactly(plainBanner.id)
            model.onDestroy()
        }

    @Test
    fun `GIVEN v2 enabled and campaigns fail WHEN model created THEN only banners without campaign shown`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        coEvery { getPromoCampaignsUseCase.invokeSync(selectedWallet, any()) } returns Either.Left(RuntimeException("network"))

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.banners.map { it.displayId }).containsExactly(plainBanner.id)
        assertThat(model.bannerStates.value.getValue(OTHER_WALLET_ID).banners.map { it.displayId })
            .containsExactly(plainBanner.id, openCampaignBanner.id).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 disabled WHEN model created THEN campaigns not requested and all banners shown`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns false

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.banners.map { it.displayId })
            .containsExactlyElementsIn(allBanners.map { it.id }).inOrder()
        coVerify(exactly = 0) { getPromoCampaignsUseCase.invokeSync(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled WHEN campaign banner clicked THEN campaign sheet requested for selected wallet`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        val model = createModel()
        advanceUntilIdle()
        val campaignBanner = model.uiState.value.banners.single { it.displayId == openCampaignBanner.id }

        // Act
        campaignBanner.config.onClick!!.invoke()

        // Assert
        verify(exactly = 1) {
            campaignsService.show(
                campaignId = OPEN_CAMPAIGN_ID.toString(),
                userWalletId = UserWalletId(SELECTED_WALLET_ID),
            )
        }
        assertThat(model.uiState.value.banners.single { it.displayId == plainBanner.id }.config.onClick).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 disabled WHEN model created THEN campaign banner is not clickable`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns false

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.banners.single { it.displayId == openCampaignBanner.id }.config.onClick)
            .isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled WHEN banners shown THEN only campaign banner reports campaign`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onBannerShown(plainBanner.id)
        model.uiState.value.onBannerShown(openCampaignBanner.id)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(PromoBannerAnalyticsEvent.Shown(plainBanner.id, PLACEHOLDER))
            analyticsEventHandler.send(
                PromoBannerAnalyticsEvent.Shown(openCampaignBanner.id, PLACEHOLDER, OPEN_CAMPAIGN_NAME),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled WHEN campaign banner clicked THEN click reports campaign`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        val model = createModel()
        advanceUntilIdle()
        val campaignBanner = model.uiState.value.banners.single { it.displayId == openCampaignBanner.id }

        // Act
        campaignBanner.config.onClick!!.invoke()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoBannerAnalyticsEvent.Clicked(openCampaignBanner.id, PLACEHOLDER, OPEN_CAMPAIGN_NAME),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled WHEN campaign banner dismissed THEN dismiss reports campaign`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        val dismissableBanner = createBanner(id = 4, campaignId = OPEN_CAMPAIGN_ID, isDismissable = true)
        coEvery { repository.getBannersSync(any(), any(), any()) } returns listOf(dismissableBanner)
        coEvery { repository.dismissBanner(any(), any()) } just runs
        val model = createModel()
        advanceUntilIdle()
        val banner = model.uiState.value.banners.single { it.displayId == dismissableBanner.id }

        // Act
        banner.config.onCloseClick!!.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoBannerAnalyticsEvent.Dismissed(dismissableBanner.id, PLACEHOLDER, OPEN_CAMPAIGN_NAME),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 disabled WHEN campaign banner shown THEN campaign not sent`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns false
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onBannerShown(openCampaignBanner.id)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(PromoBannerAnalyticsEvent.Shown(openCampaignBanner.id, PLACEHOLDER))
        }
        model.onDestroy()
    }

    private fun TestScope.createModel() = PromoBannersBlockModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(value = PromoBannersBlockComponent.Params(Placeholder.MAIN)),
        repository = repository,
        deeplinkLauncher = mockk<DeeplinkLauncher>(relaxed = true),
        analyticsEventHandler = analyticsEventHandler,
        userWalletsListRepository = userWalletsListRepository,
        uiMessageSender = mockk<UiMessageSender>(relaxed = true),
        promoBannersFeatureToggles = promoBannersFeatureToggles,
        getPromoCampaignsUseCase = getPromoCampaignsUseCase,
        campaignMatcher = PromoBannerCampaignMatcher(),
        campaignsService = campaignsService,
    )

    private fun createBanner(id: Int, campaignId: Int?, isDismissable: Boolean = false) = PromoBannerDisplay(
        id = id,
        placeholder = Placeholder.MAIN.value,
        priority = PromoBannerPriority.MEDIUM,
        title = "Title $id",
        subtitle = "Subtitle",
        iconUrl = null,
        deeplink = null,
        isButtonEnabled = false,
        buttonText = null,
        isDismissable = isDismissable,
        campaignId = campaignId,
    )

    private companion object {
        const val SELECTED_WALLET_ID = "00aa"
        const val OTHER_WALLET_ID = "00bb"
        const val OPEN_CAMPAIGN_ID = 42
        const val ENROLLED_CAMPAIGN_ID = 43
        const val OPEN_CAMPAIGN_NAME = "whale-swap-cashback"
        const val PLACEHOLDER = "main"
    }
}