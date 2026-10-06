package com.tangem.features.promobanners.impl.campaigns.model

import arrow.core.Either
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.usecase.GetPromoCampaignsUseCase
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.promobanners.api.PromoBannersFeatureToggles
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.usecase.GetPromoCampaignStateUseCase
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent
import com.tangem.features.promobanners.impl.campaigns.converters.CampaignIdConverter
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.service.CampaignRequest
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CampaignsModelTest {

    private val getPromoCampaignStateUseCase: GetPromoCampaignStateUseCase = mockk()
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val promoBannersFeatureToggles: PromoBannersFeatureToggles = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getPromoCampaignsUseCase: GetPromoCampaignsUseCase = mockk()
    private val userWallet: UserWallet = mockk()

    private val campaignIdConverter = CampaignIdConverter()

    @BeforeEach
    fun setup() {
        clearMocks(
            getPromoCampaignStateUseCase,
            messageSender,
            analyticsEventHandler,
            promoBannersFeatureToggles,
            getUserWalletUseCase,
            getPromoCampaignsUseCase,
        )
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns false
        every { getUserWalletUseCase(userWalletId) } returns Either.Right(userWallet)
    }

    @Test
    fun `GIVEN v2 enabled and campaign found WHEN emitted THEN v2 campaign sheet opened`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        val campaign = createPromoCampaign(name = "whale")
        coEvery { getPromoCampaignsUseCase(userWallet, any()) } returns
            Either.Right(listOf(createPromoCampaign(name = "other"), campaign))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "whale", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.V2.Campaign(campaign))
        coVerify(exactly = 0) { getPromoCampaignStateUseCase.invoke(any(), any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled and campaign id requested WHEN emitted THEN campaign with that id opened`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        val campaign = createPromoCampaign(name = "whale", id = 42)
        coEvery { getPromoCampaignsUseCase(userWallet, any()) } returns
            Either.Right(listOf(createPromoCampaign(name = "other", id = 7), campaign))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "42", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.V2.Campaign(campaign))
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled and value matches one id and another name WHEN emitted THEN id match opened`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        val matchedById = createPromoCampaign(name = "whale", id = 42)
        coEvery { getPromoCampaignsUseCase(userWallet, any()) } returns
            Either.Right(listOf(createPromoCampaign(name = "42", id = 7), matchedById))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "42", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.V2.Campaign(matchedById))
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled and campaign already enrolled WHEN emitted THEN already enrolled sheet opened and analytics sent`() =
        runTest {
            // Arrange
            every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
            val campaign = createPromoCampaign(name = "whale", isAlreadyEnrolled = true)
            coEvery { getPromoCampaignsUseCase(userWallet, any()) } returns Either.Right(listOf(campaign))
            val model = createModel(
                campaignFlow = flowOf(CampaignRequest(campaignId = "whale", userWalletId = userWalletId)),
            )
            val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
            model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.V2.AlreadyEnrolled(campaign))
            verify(exactly = 1) {
                analyticsEventHandler.send(PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened())
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN v2 enabled and campaign missing WHEN emitted THEN v2 not found sheet opened`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        coEvery { getPromoCampaignsUseCase(userWallet, any()) } returns Either.Right(listOf(createPromoCampaign("other")))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "whale", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.V2.NotFound)
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled and campaigns fail WHEN emitted THEN error message sent and nothing opened`() = runTest {
        // Arrange
        every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
        coEvery { getPromoCampaignsUseCase(userWallet, any()) } returns Either.Left(RuntimeException("network"))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "whale", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(any()) }
        assertThat(openedSheets).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN v2 enabled and user wallet missing WHEN emitted THEN error message sent and campaigns not requested`() =
        runTest {
            // Arrange
            every { promoBannersFeatureToggles.isPromoCampaignsV2Enabled } returns true
            every { getUserWalletUseCase(userWalletId) } returns Either.Left(GetUserWalletError.UserWalletNotFound)
            val model = createModel(
                campaignFlow = flowOf(CampaignRequest(campaignId = "whale", userWalletId = userWalletId)),
            )
            val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
            model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

            // Act
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
            coVerify(exactly = 0) { getPromoCampaignsUseCase(any(), any()) }
            assertThat(openedSheets).isEmpty()
            model.onDestroy()
        }

    @Test
    fun `GIVEN v2 disabled WHEN emitted THEN v3 campaigns are not requested`() = runTest {
        // Arrange
        coEvery { getPromoCampaignStateUseCase.invoke(any(), any(), any()) } returns
            Either.Right(PromoCampaignState.NotActive(PromoCampaignId.WhaleSwapCashback))
        val model = createModel(
            campaignFlow = flowOf(CampaignRequest(campaignId = "whale-swap-cashback", userWalletId = userWalletId)),
        )

        // Act
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { getPromoCampaignsUseCase(any(), any()) }
        coVerify(exactly = 1) { getPromoCampaignStateUseCase.invoke(any(), any(), any()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideKnownCampaignModels")
    fun `GIVEN known campaign request WHEN emitted THEN campaign state is checked with mapped id`(
        model: KnownCampaignModel,
    ) = runTest {
        // Arrange
        coEvery { getPromoCampaignStateUseCase.invoke(any(), any(), any()) } returns
            Either.Right(PromoCampaignState.NotActive(model.expectedPromoId))
        val campaignsModel = createModel(
            campaignFlow = flowOf(CampaignRequest(campaignId = model.campaignId, userWalletId = userWalletId)),
        )

        // Act
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            getPromoCampaignStateUseCase.invoke(
                campaign = model.expectedPromoId,
                userWalletId = userWalletId,
                forceRefresh = any(),
            )
        }
        verify { messageSender wasNot Called }
        campaignsModel.onDestroy()
    }

    @Test
    fun `GIVEN campaign state fails WHEN emitted THEN error message is sent`() = runTest {
        // Arrange
        coEvery { getPromoCampaignStateUseCase.invoke(any(), any(), any()) } returns
            Either.Left(RuntimeException("network"))
        val model = createModel(
            campaignFlow = flowOf(CampaignRequest(campaignId = "whale-swap-cashback", userWalletId = userWalletId)),
        )

        // Act
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN unknown campaign id WHEN emitted THEN campaign state is not checked`() = runTest {
        // Arrange
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "unknown", userWalletId = userWalletId)))

        // Act
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { getPromoCampaignStateUseCase.invoke(any(), any(), any()) }
        verify { messageSender wasNot Called }
        model.onDestroy()
    }

    @Test
    fun `WHEN onAlreadyActivated THEN analytics sent`() = runTest {
        // Arrange
        val model = createModel(campaignFlow = emptyFlow())
        advanceUntilIdle()

        // Act
        model.onAlreadyActivated(CampaignType.WhaleSwapCashback(campaignId = "1"))

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened())
        }
        model.onDestroy()
    }

    @Test
    fun `WHEN onActivated THEN no analytics sent`() = runTest {
        // Arrange
        val model = createModel(campaignFlow = emptyFlow())
        advanceUntilIdle()

        // Act
        model.onActivated(CampaignType.WhaleSwapCashback(campaignId = "1"))

        // Assert
        verify { analyticsEventHandler wasNot Called }
        model.onDestroy()
    }

    private fun TestScope.createModel(campaignFlow: Flow<CampaignRequest>): CampaignsModel {
        val campaignsService: CampaignsService = mockk {
            every { this@mockk.campaignFlow } returns campaignFlow
        }
        return CampaignsModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            campaignIdConverter = campaignIdConverter,
            campaignsService = campaignsService,
            getPromoCampaignStateUseCase = getPromoCampaignStateUseCase,
            messageSender = messageSender,
            analyticsEventHandler = analyticsEventHandler,
            promoBannersFeatureToggles = promoBannersFeatureToggles,
            getUserWalletUseCase = getUserWalletUseCase,
            getPromoCampaignsUseCase = getPromoCampaignsUseCase,
        )
    }

    private fun provideKnownCampaignModels() = listOf(
        KnownCampaignModel(campaignId = "whale-swap-cashback", expectedPromoId = PromoCampaignId.WhaleSwapCashback),
        KnownCampaignModel(campaignId = "reactivation-cashback", expectedPromoId = PromoCampaignId.ReactivationCashback),
    )

    internal data class KnownCampaignModel(
        val campaignId: String,
        val expectedPromoId: PromoCampaignId,
    ) {
        override fun toString(): String = "\"$campaignId\" -> $expectedPromoId"
    }

    private companion object {
        val userWalletId = UserWalletId("0011223344556677")
    }
}