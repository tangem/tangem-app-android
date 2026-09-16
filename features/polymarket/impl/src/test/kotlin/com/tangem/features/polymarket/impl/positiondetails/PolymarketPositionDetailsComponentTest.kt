package com.tangem.features.polymarket.impl.positiondetails

import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.pause
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ModelsEntryPoint
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.features.polymarket.impl.positiondetails.model.PolymarketPositionDetailsModel
import dagger.hilt.EntryPoints
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import javax.inject.Provider

internal class PolymarketPositionDetailsComponentTest {

    private val model: PolymarketPositionDetailsModel = mockk(relaxed = true)
    private val lifecycle = LifecycleRegistry()
    private val paramsSlot = slot<ParamsContainer>()

    @AfterEach
    fun tearDown() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun `GIVEN position details component WHEN created THEN passes wallet and outcome to model`() {
        // Arrange
        val params = PolymarketPositionDetailsComponent.Params(UserWalletId("022"), PolymarketPositionId("condition", "asset"))

        // Act
        PolymarketPositionDetailsComponent(createContext(), params)

        // Assert
        assertThat(paramsSlot.captured.require<PolymarketPositionDetailsComponent.Params>()).isEqualTo(params)
        verify(exactly = 0) { model.onResume() }
    }

    @Test
    fun `GIVEN position details component WHEN resumed and paused THEN forwards lifecycle to loader model`() {
        // Arrange
        PolymarketPositionDetailsComponent(
            createContext(),
            PolymarketPositionDetailsComponent.Params(UserWalletId("022"), PolymarketPositionId("condition", "asset")),
        )
        lifecycle.create()
        lifecycle.start()

        // Act
        lifecycle.resume()
        lifecycle.pause()

        // Assert
        verify(exactly = 1) { model.onResume() }
        verify(exactly = 1) { model.onPause() }
    }

    private fun createContext(): AppComponentContext {
        val builder: ModelComponent.Builder = mockk()
        every { builder.router(any()) } returns builder
        every { builder.uiMessageSender(any()) } returns builder
        every { builder.paramsContainer(capture(paramsSlot)) } returns builder
        every { builder.build() } returns mockk()
        val entryPoint: ModelsEntryPoint = mockk()
        every { entryPoint.models() } returns mapOf(
            PolymarketPositionDetailsModel::class.java to Provider<Model> { model },
        )
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), ModelsEntryPoint::class.java) } returns entryPoint
        return mockk<AppComponentContext>(relaxed = true).also {
            every { it.instanceKeeper } returns InstanceKeeperDispatcher()
            every { it.tags } returns HashMap()
            every { it.hiltComponentBuilder } returns builder
            every { it.lifecycle } returns lifecycle
        }
    }
}