package com.tangem.features.polymarket.impl.history

import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
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
import com.tangem.features.polymarket.impl.history.model.PolymarketHistoryModel
import dagger.hilt.EntryPoints
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketHistoryComponentTest {

    private val model: PolymarketHistoryModel = mockk(relaxed = true)
    private val lifecycle = LifecycleRegistry()

    @AfterEach
    fun tearDown() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun `GIVEN History paused behind another screen WHEN export finishes THEN waits until History resumes`() = runTest {
        // Arrange
        val files = Channel<File>(Channel.BUFFERED)
        var received = 0
        every { model.exports } returns files.receiveAsFlow().onEach { received++ }
        PolymarketHistoryComponent(createContext(backgroundScope), PolymarketHistoryComponent.Params(UserWalletId("022")))
        lifecycle.create()
        lifecycle.start()
        lifecycle.resume()
        runCurrent()

        // Act
        lifecycle.pause()
        files.send(File("predictions-history.csv"))
        runCurrent()

        // Assert
        assertThat(received).isEqualTo(0)
        lifecycle.resume()
        runCurrent()
        assertThat(received).isEqualTo(1)
        lifecycle.pause()
        lifecycle.resume()
        runCurrent()
        assertThat(received).isEqualTo(1)
    }

    private fun createContext(scope: CoroutineScope): AppComponentContext {
        val builder: ModelComponent.Builder = mockk()
        every { builder.router(any()) } returns builder
        every { builder.uiMessageSender(any()) } returns builder
        every { builder.paramsContainer(any()) } returns builder
        every { builder.build() } returns mockk()
        val entryPoint: ModelsEntryPoint = mockk()
        every { entryPoint.models() } returns mapOf(PolymarketHistoryModel::class.java to Provider<Model> { model })
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), ModelsEntryPoint::class.java) } returns entryPoint
        return mockk<AppComponentContext>(relaxed = true).also {
            every { it.instanceKeeper } returns InstanceKeeperDispatcher()
            every { it.tags } returns HashMap()
            every { it.hiltComponentBuilder } returns builder
            every { it.lifecycle } returns lifecycle
            every { it.componentScope } returns scope
            every { it.activity } throws IllegalStateException("No Android activity in this JVM test")
        }
    }
}