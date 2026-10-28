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
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened
import com.tangem.features.promobanners.impl.campaigns.service.CampaignRequest
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CampaignsModelTest {

    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getPromoCampaignsUseCase: GetPromoCampaignsUseCase = mockk()
    private val userWallet: UserWallet = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(
            messageSender,
            analyticsEventHandler,
            getUserWalletUseCase,
            getPromoCampaignsUseCase,
        )
        every { getUserWalletUseCase(userWalletId) } returns Either.Right(userWallet)
    }

    @Test
    fun `GIVEN campaign found WHEN emitted THEN v2 campaign sheet opened`() = runTest {
        // Arrange
        val campaign = createPromoCampaign(name = "whale")
        coEvery { getPromoCampaignsUseCase.invokeSync(userWallet, any()) } returns
            Either.Right(listOf(createPromoCampaign(name = "other"), campaign))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "whale", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.Campaign(campaign, userWalletId))
        model.onDestroy()
    }

    @Test
    fun `GIVEN campaign id requested WHEN emitted THEN campaign with that id opened`() = runTest {
        // Arrange
        val campaign = createPromoCampaign(name = "whale", id = 42)
        coEvery { getPromoCampaignsUseCase.invokeSync(userWallet, any()) } returns
            Either.Right(listOf(createPromoCampaign(name = "other", id = 7), campaign))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "42", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.Campaign(campaign, userWalletId))
        model.onDestroy()
    }

    @Test
    fun `GIVEN value matches one id and another name WHEN emitted THEN id match opened`() = runTest {
        // Arrange
        val matchedById = createPromoCampaign(name = "whale", id = 42)
        coEvery { getPromoCampaignsUseCase.invokeSync(userWallet, any()) } returns
            Either.Right(listOf(createPromoCampaign(name = "42", id = 7), matchedById))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "42", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.Campaign(matchedById, userWalletId))
        model.onDestroy()
    }

    @Test
    fun `GIVEN campaign already enrolled WHEN emitted THEN already enrolled sheet opened and analytics sent`() =
        runTest {
            // Arrange
            val campaign = createPromoCampaign(name = "whale", isAlreadyEnrolled = true)
            coEvery { getPromoCampaignsUseCase.invokeSync(userWallet, any()) } returns Either.Right(listOf(campaign))
            val model = createModel(
                campaignFlow = flowOf(CampaignRequest(campaignId = "whale", userWalletId = userWalletId)),
            )
            val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
            model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.AlreadyEnrolled(campaign))
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    AlreadyEnrolledScreenOpened(campaign = "whale", source = AlreadyEnrolledScreenOpened.Source.Entry),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN campaign missing WHEN emitted THEN v2 not found sheet opened`() = runTest {
        // Arrange
        coEvery { getPromoCampaignsUseCase.invokeSync(userWallet, any()) } returns Either.Right(listOf(createPromoCampaign("other")))
        val model = createModel(campaignFlow = flowOf(CampaignRequest(campaignId = "whale", userWalletId = userWalletId)))
        val openedSheets = mutableListOf<CampaignsBottomSheetConfig?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(CampaignsBottomSheetConfig.NotFound)
        model.onDestroy()
    }

    @Test
    fun `GIVEN campaigns fail WHEN emitted THEN error message sent and nothing opened`() = runTest {
        // Arrange
        coEvery { getPromoCampaignsUseCase.invokeSync(userWallet, any()) } returns Either.Left(RuntimeException("network"))
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
    fun `GIVEN user wallet missing WHEN emitted THEN error message sent and campaigns not requested`() =
        runTest {
            // Arrange
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
            coVerify(exactly = 0) { getPromoCampaignsUseCase.invokeSync(any(), any()) }
            assertThat(openedSheets).isEmpty()
            model.onDestroy()
        }

    private fun TestScope.createModel(campaignFlow: Flow<CampaignRequest>): CampaignsModel {
        val campaignsService: CampaignsService = mockk {
            every { this@mockk.campaignFlow } returns campaignFlow
        }
        return CampaignsModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            campaignsService = campaignsService,
            messageSender = messageSender,
            analyticsEventHandler = analyticsEventHandler,
            getUserWalletUseCase = getUserWalletUseCase,
            getPromoCampaignsUseCase = getPromoCampaignsUseCase,
        )
    }

    private companion object {
        val userWalletId = UserWalletId("0011223344556677")
    }
}