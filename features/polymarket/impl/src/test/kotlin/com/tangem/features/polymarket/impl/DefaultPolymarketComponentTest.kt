package com.tangem.features.polymarket.impl

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.DefaultAppComponentContext
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ModelsEntryPoint
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.common.walletlink.PolymarketWalletLinkGate
import com.tangem.features.polymarket.impl.entry.model.PolymarketEntryModel
import com.tangem.features.polymarket.impl.model.PolymarketModel
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import dagger.hilt.EntryPoints
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Provider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class DefaultPolymarketComponentTest {

    private val parentRouter: Router = mockk(relaxed = true)
    private val walletLinkGate: PolymarketWalletLinkGate = mockk()

    private val userWalletId = UserWalletId("011")

    @AfterEach
    fun unmockEntryPoints() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun `GIVEN the gate rejects the caller's wallet at once WHEN the feature is created THEN the feature is popped`() {
        // Arrange
        coEvery { walletLinkGate.ensureLinked(userWalletId) } returns false
        val appComponentContext = createAppComponentContext()

        // Act
        DefaultPolymarketComponent(
            appComponentContext = appComponentContext,
            params = PolymarketComponent.Params(userWalletId = userWalletId),
            portfolioSelectorComponentFactory = mockk(relaxed = true),
            tokenReceiveComponentFactory = mockk(relaxed = true),
            polymarketFeatureToggles = mockk(relaxed = true),
        )

        // Assert
        verify(exactly = 1) { parentRouter.pop(any()) }
    }

    private fun createAppComponentContext(): AppComponentContext {
        val routers = mutableListOf<Router>()
        val paramsContainers = mutableListOf<ParamsContainer>()
        val hiltComponentBuilder: ModelComponent.Builder = mockk()
        every { hiltComponentBuilder.router(capture(routers)) } returns hiltComponentBuilder
        every { hiltComponentBuilder.uiMessageSender(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.paramsContainer(capture(paramsContainers)) } returns hiltComponentBuilder
        every { hiltComponentBuilder.build() } returns mockk()

        val entryPoint: ModelsEntryPoint = mockk()
        every { entryPoint.models() } returns mapOf(
            PolymarketModel::class.java to Provider<Model> {
                PolymarketModel(
                    featureToggles = mockk(relaxed = true),
                    observeSessionKeyOutcome = mockk(relaxed = true),
                    observeSessionKeyOperation = mockk(relaxed = true),
                    acknowledgeSessionKeyOutcome = mockk(relaxed = true),
                    messageSender = mockk(relaxed = true),
                    dispatchers = TestingCoroutineDispatcherProvider(),
                )
            },
            PolymarketEntryModel::class.java to Provider<Model> {
                PolymarketEntryModel(
                    paramsContainer = paramsContainers.last(),
                    router = routers.last(),
                    getEligibleWalletsUseCase = mockk(relaxed = true),
                    walletLinkGate = walletLinkGate,
                    portfolioSelectorController = mockk(relaxed = true) {
                        every { selectedAccountWithData(any()) } returns MutableStateFlow(null)
                    },
                    portfolioFetcherFactory = mockk(relaxed = true),
                    dispatchers = TestingCoroutineDispatcherProvider(),
                )
            },
        )
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), ModelsEntryPoint::class.java) } returns entryPoint

        return DefaultAppComponentContext(
            componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry(), backHandler = BackDispatcher()),
            dispatchers = TestingCoroutineDispatcherProvider(),
            hiltComponentBuilder = hiltComponentBuilder,
            messageSender = mockk(relaxed = true),
            activity = mockk(relaxed = true),
            replaceRouter = parentRouter,
        )
    }
}