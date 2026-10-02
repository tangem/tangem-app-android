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
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoBannerCampaignMatcher
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoCampaignAvailabilityChecker
import com.tangem.features.promobanners.impl.repository.PromoBannersRepository
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val availabilityChecker: PromoCampaignAvailabilityChecker = mockk()

    private val selectedWallet: UserWallet = mockk { every { walletId } returns UserWalletId(SELECTED_WALLET_ID) }
    private val otherWallet: UserWallet = mockk { every { walletId } returns UserWalletId(OTHER_WALLET_ID) }

    private val plainBanner = createBanner(id = 1, campaignId = null)
    private val openCampaignBanner = createBanner(id = 2, campaignId = OPEN_CAMPAIGN_ID)
    private val enrolledCampaignBanner = createBanner(id = 3, campaignId = ENROLLED_CAMPAIGN_ID)
    private val allBanners = listOf(plainBanner, openCampaignBanner, enrolledCampaignBanner)

    @BeforeEach
    fun setup() {
        clearMocks(repository, userWalletsListRepository, promoBannersFeatureToggles, getPromoCampaignsUseCase)
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(selectedWallet)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(selectedWallet, otherWallet))
        coEvery { repository.getCachedBanners(any(), any()) } returns null
        coEvery { repository.getBanners(any(), any(), any()) } returns allBanners
        every { availabilityChecker.isAvailable(any(), any()) } returns true
        coEvery { getPromoCampaignsUseCase(any(), any()) } returns Either.Right(
            listOf(
                createPromoCampaign(id = OPEN_CAMPAIGN_ID),
                createPromoCampaign(id = ENROLLED_CAMPAIGN_ID, isAlreadyEnrolled = true),
            ),
        )
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
            coVerify(exactly = 1) { getPromoCampaignsUseCase(selectedWallet, any()) }
            coVerify(exactly = 1) { getPromoCampaignsUseCase(otherWallet, any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN v2 enabled and campaigns fail WHEN model created THEN only banners without campaign shown`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        coEvery { getPromoCampaignsUseCase(selectedWallet, any()) } returns Either.Left(RuntimeException("network"))

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
        coVerify(exactly = 0) { getPromoCampaignsUseCase(any(), any()) }
        model.onDestroy()
    }

    private fun TestScope.createModel() = PromoBannersBlockModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(value = PromoBannersBlockComponent.Params(Placeholder.MAIN)),
        repository = repository,
        deeplinkLauncher = mockk<DeeplinkLauncher>(relaxed = true),
        analyticsEventHandler = mockk<AnalyticsEventHandler>(relaxed = true),
        userWalletsListRepository = userWalletsListRepository,
        uiMessageSender = mockk<UiMessageSender>(relaxed = true),
        promoBannersFeatureToggles = promoBannersFeatureToggles,
        getPromoCampaignsUseCase = getPromoCampaignsUseCase,
        campaignMatcher = PromoBannerCampaignMatcher(availabilityChecker),
    )

    private fun createBanner(id: Int, campaignId: Int?) = PromoBannerDisplay(
        id = id,
        placeholder = Placeholder.MAIN.value,
        priority = PromoBannerPriority.MEDIUM,
        title = "Title $id",
        subtitle = "Subtitle",
        iconUrl = null,
        deeplink = null,
        isButtonEnabled = false,
        buttonText = null,
        isDismissable = false,
        campaignId = campaignId,
    )

    private companion object {
        const val SELECTED_WALLET_ID = "00aa"
        const val OTHER_WALLET_ID = "00bb"
        const val OPEN_CAMPAIGN_ID = 42
        const val ENROLLED_CAMPAIGN_ID = 43
    }
}