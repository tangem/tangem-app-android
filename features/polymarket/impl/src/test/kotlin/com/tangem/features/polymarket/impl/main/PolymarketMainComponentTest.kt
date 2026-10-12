package com.tangem.features.polymarket.impl.main

import arrow.core.right
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ModelsEntryPoint
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.features.polymarket.impl.main.model.PolymarketMainModel
import com.tangem.features.polymarket.impl.main.model.PolymarketMainParams
import com.tangem.features.polymarket.impl.main.model.PolymarketPositionsPreviewLoader
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import dagger.hilt.EntryPoints
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import javax.inject.Provider
import com.tangem.domain.models.account.PredictionAccountStatusValue
import kotlinx.coroutines.flow.flowOf

/**
 * The feed is reached only after the gate resolves an access mode, so a model it cannot construct crashes the
 * feature on open. Its model reads [PolymarketMainParams] out of the params container, which only reaches it if
 * the component hands the params to `getOrCreateModel`.
 */
internal class PolymarketMainComponentTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val getPolymarketEventsBatchFlowUseCase: GetPolymarketEventsBatchFlowUseCase = mockk()
    private val getPolymarketCategoriesUseCase: GetPolymarketCategoriesUseCase = mockk()
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier = mockk(relaxed = true)
    private val refreshRequisites: RefreshPredictionDepositRequisitesInteractor = mockk(relaxed = true)
    private val checkGeoblock: CheckPolymarketGeoblockUseCase = mockk<CheckPolymarketGeoblockUseCase>().also {
        coEvery { it() } returns false.right()
    }
    private val positionsPreviewLoader: PolymarketPositionsPreviewLoader = mockk(relaxed = true)
    private val fundingWindow: PredictionFundingWindow = mockk(relaxed = true)

    private val userWalletId = UserWalletId("011")

    init {
        every { positionsPreviewLoader.state } returns MutableStateFlow(PolymarketPositionsPreviewUM.Loading)
        // The model starts loading on construction; empty stubs keep this wiring test off the feed logic.
        coEvery { getPolymarketCategoriesUseCase() } returns emptyList<PolymarketCategory>().right()
        every { predictionAccountStatusSupplier(any<UserWalletId>()) } returns
            flowOf(PredictionAccountStatusValue.Loading)
        every { fundingWindow.isOpen(any()) } returns MutableStateFlow(false)
        every { getPolymarketEventsBatchFlowUseCase(any(), any()) } returns object : PolymarketEventsBatchFlow {
            override val state = MutableStateFlow(
                BatchListState<Int, PolymarketEventsBatch>(
                    data = emptyList(),
                    status = PaginationStatus.InitialLoading,
                ),
            )
            override val updateResults =
                MutableSharedFlow<Pair<PolymarketEventsUpdateRequest, BatchUpdateResult<Int, PolymarketEventsBatch>>>()
        }
    }

    @AfterEach
    fun unmockEntryPoints() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun `GIVEN the feed is created WHEN its model is resolved THEN the params are handed over`() {
        // Arrange
        val paramsContainerSlot = slot<ParamsContainer>()
        val appComponentContext = createAppComponentContext(paramsContainerSlot = paramsContainerSlot)

        // Act
        PolymarketMainComponent(
            appComponentContext = appComponentContext,
            userWalletId = userWalletId,
            tokenReceiveComponentFactory = mockk(relaxed = true),
        )

        // Assert
        assertThat(paramsContainerSlot.captured.require<PolymarketMainParams>())
            .isEqualTo(PolymarketMainParams(userWalletId = userWalletId))
    }

    private fun createAppComponentContext(paramsContainerSlot: CapturingSlot<ParamsContainer>): AppComponentContext {
        val hiltComponentBuilder: ModelComponent.Builder = mockk()
        every { hiltComponentBuilder.router(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.uiMessageSender(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.paramsContainer(capture(paramsContainerSlot)) } returns hiltComponentBuilder
        every { hiltComponentBuilder.build() } returns mockk()

        val entryPoint: ModelsEntryPoint = mockk()
        every { entryPoint.models() } returns mapOf(
            PolymarketMainModel::class.java to Provider<Model> { createModel(paramsContainerSlot.captured) },
        )
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), ModelsEntryPoint::class.java) } returns entryPoint

        return mockk<AppComponentContext>(relaxed = true).also {
            every { it.instanceKeeper } returns InstanceKeeperDispatcher()
            every { it.stateKeeper } returns StateKeeperDispatcher()
            every { it.lifecycle } returns LifecycleRegistry()
            every { it.backHandler } returns BackDispatcher()
            every { it.tags } returns HashMap()
            every { it.hiltComponentBuilder } returns hiltComponentBuilder
            every { it.router } returns router
        }
    }

    private fun createModel(paramsContainer: ParamsContainer) = PolymarketMainModel(
        paramsContainer = paramsContainer,
        router = router,
        messageSender = messageSender,
        dispatchers = TestingCoroutineDispatcherProvider(),
        getPolymarketEventsBatchFlowUseCase = getPolymarketEventsBatchFlowUseCase,
        getPolymarketCategoriesUseCase = getPolymarketCategoriesUseCase,
        predictionAccountStatusSupplier = predictionAccountStatusSupplier,
        refreshPredictionDepositRequisitesInteractor = refreshRequisites,
        checkPolymarketGeoblockUseCase = checkGeoblock,
        predictionAccountStatusFetcher = mockk(relaxed = true),
        positionsPreviewLoader = positionsPreviewLoader,
        predictionBalancePoller = mockk(relaxed = true),
        predictionFundingWindow = fundingWindow,
    )
}