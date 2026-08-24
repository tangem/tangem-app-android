package com.tangem.features.polymarket.impl.main

import arrow.core.right
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ModelsEntryPoint
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.features.polymarket.impl.main.model.PolymarketMainModel
import com.tangem.features.polymarket.impl.main.model.PolymarketMainParams
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

/**
 * The feed is reached only after the gate resolves an access mode, so a model it cannot construct crashes the
 * feature on open. Its model reads [PolymarketMainParams] out of the params container, which only reaches it if
 * the component hands the params to `getOrCreateModel`.
 */
internal class PolymarketMainComponentTest {

    private val router: Router = mockk(relaxed = true)
    private val getPolymarketEventsBatchFlowUseCase: GetPolymarketEventsBatchFlowUseCase = mockk()
    private val getPolymarketCategoriesUseCase: GetPolymarketCategoriesUseCase = mockk()

    private val userWalletId = UserWalletId("011")

    init {
        // The model starts loading on construction; empty stubs keep this wiring test off the feed logic.
        coEvery { getPolymarketCategoriesUseCase() } returns emptyList<PolymarketCategory>().right()
        every { getPolymarketEventsBatchFlowUseCase(any(), any()) } returns object : PolymarketEventsBatchFlow {
            override val state = MutableStateFlow(
                BatchListState<Int, List<PolymarketEvent>>(
                    data = emptyList(),
                    status = PaginationStatus.InitialLoading,
                ),
            )
            override val updateResults = MutableSharedFlow<Pair<Nothing, BatchUpdateResult<Int, List<PolymarketEvent>>>>()
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
            every { it.tags } returns HashMap()
            every { it.hiltComponentBuilder } returns hiltComponentBuilder
            every { it.router } returns router
        }
    }

    private fun createModel(paramsContainer: ParamsContainer) = PolymarketMainModel(
        paramsContainer = paramsContainer,
        router = router,
        dispatchers = TestingCoroutineDispatcherProvider(),
        getPolymarketEventsBatchFlowUseCase = getPolymarketEventsBatchFlowUseCase,
        getPolymarketCategoriesUseCase = getPolymarketCategoriesUseCase,
    )
}