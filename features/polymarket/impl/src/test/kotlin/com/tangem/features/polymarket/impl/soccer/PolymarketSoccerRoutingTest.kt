package com.tangem.features.polymarket.impl.soccer

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ModelsEntryPoint
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.details.model.PolymarketEventDetailsModel
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.navigation.createEventDetailsComponent
import com.tangem.features.polymarket.impl.soccer.model.PolymarketSoccerModel
import dagger.hilt.EntryPoints
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import javax.inject.Provider

internal class PolymarketSoccerRoutingTest {
    private val soccer: PolymarketSoccerModel = mockk(relaxed = true)
    private val generic: PolymarketEventDetailsModel = mockk(relaxed = true)
    private val lifecycle = LifecycleRegistry()

    @AfterEach
    fun tearDown() { unmockkStatic(EntryPoints::class) }

    @ParameterizedTest
    @CsvSource("true,true,true", "false,true,false", "true,false,false", "true,,false")
    fun `GIVEN route and toggle WHEN opened and paused THEN only the selected screen lifecycle runs`(
        enabled: Boolean,
        isSoccerMatch: Boolean?,
        expectSoccer: Boolean,
    ) {
        // Arrange
        val route = PolymarketRoute.EventDetails("child", UserWalletId("011"), isSoccerMatch = isSoccerMatch)
        every { soccer.rulesNavigation } returns SlotNavigation()
        val context = context()

        // Act
        val component = route.createEventDetailsComponent(context, enabled)
        lifecycle.create()
        lifecycle.start()
        lifecycle.resume()
        lifecycle.pause()

        // Assert
        assertThat(component).isInstanceOf(
            if (expectSoccer) PolymarketSoccerComponent::class.java else PolymarketEventDetailsComponent::class.java,
        )
        verify(exactly = if (expectSoccer) 1 else 0) { soccer.onResume(); soccer.onPause() }
        verify(exactly = if (expectSoccer) 0 else 1) { generic.onResume(); generic.onPause() }
    }

    private fun context(): AppComponentContext {
        val builder: ModelComponent.Builder = mockk()
        every { builder.router(any()) } returns builder
        every { builder.uiMessageSender(any()) } returns builder
        every { builder.paramsContainer(any()) } returns builder
        every { builder.build() } returns mockk()
        val entryPoint: ModelsEntryPoint = mockk()
        every { entryPoint.models() } returns mapOf(
            PolymarketSoccerModel::class.java to Provider<Model> { soccer },
            PolymarketEventDetailsModel::class.java to Provider<Model> { generic },
        )
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), ModelsEntryPoint::class.java) } returns entryPoint
        return mockk<AppComponentContext>(relaxed = true).also {
            every { it.instanceKeeper } returns InstanceKeeperDispatcher()
            every { it.tags } returns HashMap()
            every { it.hiltComponentBuilder } returns builder
            every { it.lifecycle } returns lifecycle
            every { it.stateKeeper } returns StateKeeperDispatcher()
            every { it.backHandler } returns BackDispatcher()
            every { it.activity } throws IllegalStateException("No Android activity in this JVM test")
        }
    }
}