package com.tangem.features.polymarket.impl.onboarding

import arrow.core.right
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ModelsEntryPoint
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.interactor.ResolvePolymarketEntryInteractor
import com.tangem.domain.polymarket.usecase.RunPolymarketOnboardingUseCase
import com.tangem.features.polymarket.impl.onboarding.model.PolymarketOnboardingModel
import com.tangem.features.polymarket.impl.onboarding.model.PolymarketOnboardingParams
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import dagger.hilt.EntryPoints
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import javax.inject.Provider

/**
 * The gate is reached only for a wallet the entry route already settled, so a model it cannot construct
 * crashes the feature on open. Its model reads [PolymarketOnboardingParams] out of the params container, which
 * only reaches it if the component hands the wallet to `getOrCreateModel`.
 */
internal class PolymarketOnboardingComponentTest {

    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val resolvePolymarketEntryInteractor: ResolvePolymarketEntryInteractor = mockk()
    private val runPolymarketOnboardingUseCase: RunPolymarketOnboardingUseCase = mockk()

    private val userWalletId = UserWalletId("011")

    @AfterEach
    fun unmockEntryPoints() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun `GIVEN the gate is created WHEN its model is resolved THEN the settled wallet is handed over`() {
        // Arrange
        coEvery { resolvePolymarketEntryInteractor(userWalletId) } returns PolymarketEntry.RegionBlocked.right()
        val paramsContainerSlot = slot<ParamsContainer>()
        val appComponentContext = createAppComponentContext(paramsContainerSlot = paramsContainerSlot)

        // Act
        PolymarketOnboardingComponent(
            appComponentContext = appComponentContext,
            userWalletId = userWalletId,
        )

        // Assert
        assertThat(paramsContainerSlot.captured.require<PolymarketOnboardingParams>())
            .isEqualTo(PolymarketOnboardingParams(userWalletId = userWalletId))
    }

    private fun createAppComponentContext(paramsContainerSlot: CapturingSlot<ParamsContainer>): AppComponentContext {
        val hiltComponentBuilder: ModelComponent.Builder = mockk()
        every { hiltComponentBuilder.router(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.uiMessageSender(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.paramsContainer(capture(paramsContainerSlot)) } returns hiltComponentBuilder
        every { hiltComponentBuilder.build() } returns mockk()

        val entryPoint: ModelsEntryPoint = mockk()
        every { entryPoint.models() } returns mapOf(
            PolymarketOnboardingModel::class.java to Provider<Model> { createModel(paramsContainerSlot.captured) },
        )
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), ModelsEntryPoint::class.java) } returns entryPoint

        return mockk<AppComponentContext>(relaxed = true).also {
            every { it.instanceKeeper } returns InstanceKeeperDispatcher()
            every { it.stateKeeper } returns StateKeeperDispatcher()
            every { it.tags } returns HashMap()
            every { it.hiltComponentBuilder } returns hiltComponentBuilder
            every { it.router } returns router
        }
    }

    private fun createModel(paramsContainer: ParamsContainer) = PolymarketOnboardingModel(
        paramsContainer = paramsContainer,
        router = router,
        urlOpener = urlOpener,
        resolvePolymarketEntryInteractor = resolvePolymarketEntryInteractor,
        runPolymarketOnboardingUseCase = runPolymarketOnboardingUseCase,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )
}