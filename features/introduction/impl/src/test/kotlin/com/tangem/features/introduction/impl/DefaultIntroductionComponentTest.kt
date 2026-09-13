package com.tangem.features.introduction.impl

import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ModelsEntryPoint
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.model.IntroductionModel
import dagger.hilt.EntryPoints
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import javax.inject.Provider

/** A relaxed lifecycle mock would never advance, so the registry here is real. */
internal class DefaultIntroductionComponentTest {

    private val model: IntroductionModel = mockk(relaxed = true)
    private val lifecycle = LifecycleRegistry()

    @AfterEach
    fun unmockEntryPoints() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun `GIVEN introduction screen WHEN lifecycle resumes THEN video runs`() {
        // Arrange
        createComponent()
        lifecycle.create()
        lifecycle.start()

        // Act
        lifecycle.resume()

        // Assert
        verify(exactly = 1) { model.setVideoRunning(isRunning = true) }
    }

    @Test
    fun `GIVEN resumed screen WHEN lifecycle pauses THEN video stops`() {
        // Arrange
        createComponent()
        lifecycle.create()
        lifecycle.start()
        lifecycle.resume()

        // Act
        lifecycle.pause()

        // Assert
        verify(exactly = 1) { model.setVideoRunning(isRunning = false) }
    }

    @Test
    fun `GIVEN introduction screen WHEN never resumed THEN video is not touched`() {
        // Act
        createComponent()
        lifecycle.create()
        lifecycle.start()

        // Assert
        verify(exactly = 0) { model.setVideoRunning(any()) }
    }

    private fun createComponent() = DefaultIntroductionComponent(
        appComponentContext = createAppComponentContext(),
        params = IntroductionComponent.Params(launchMode = InitScreenLaunchMode.Standard),
    )

    private fun createAppComponentContext(): AppComponentContext {
        val hiltComponentBuilder: ModelComponent.Builder = mockk()
        every { hiltComponentBuilder.router(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.uiMessageSender(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.paramsContainer(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.build() } returns mockk()

        val entryPoint: ModelsEntryPoint = mockk()
        every { entryPoint.models() } returns mapOf(
            IntroductionModel::class.java to Provider<Model> { model },
        )
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), ModelsEntryPoint::class.java) } returns entryPoint

        return mockk<AppComponentContext>(relaxed = true).also {
            every { it.instanceKeeper } returns InstanceKeeperDispatcher()
            every { it.tags } returns HashMap()
            every { it.hiltComponentBuilder } returns hiltComponentBuilder
            every { it.lifecycle } returns lifecycle
        }
    }
}