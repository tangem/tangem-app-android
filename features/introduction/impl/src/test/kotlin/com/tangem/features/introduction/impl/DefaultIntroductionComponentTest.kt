package com.tangem.features.introduction.impl

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ComponentContextFactory
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
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
import io.mockk.slot
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.google.common.truth.Truth.assertThat
import com.tangem.features.storiesv2.StoriesV2Component
import com.tangem.features.storiesv2.StoriesV2PendingAction
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.features.storiesv2.StoryV2Source
import com.tangem.features.storiesv2.StoryV2Type
import kotlinx.serialization.KSerializer
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import javax.inject.Provider

/** A relaxed lifecycle mock would never advance, so the registry here is real. */
internal class DefaultIntroductionComponentTest {

    private val model: IntroductionModel = mockk(relaxed = true)
    private val storiesComponentFactory: StoriesV2Component.Factory = mockk(relaxed = true)
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

    @Test
    fun `GIVEN a resumed screen WHEN the stories slot activates THEN the onboarding player is built for it`() {
        // Arrange
        val navigation = SlotNavigation<Unit>()
        every { model.storiesNavigation } returns navigation
        // The factory's return type is erased through ComponentFactory, so a relaxed mock cannot infer it.
        every { storiesComponentFactory.create(any(), any()) } returns mockk(relaxed = true)
        createComponent()
        lifecycle.create()
        lifecycle.start()
        lifecycle.resume()

        // Act
        navigation.activate(Unit)

        // Assert
        val params = slot<StoriesV2Component.Params>()
        verify(exactly = 1) { storiesComponentFactory.create(any(), capture(params)) }
        assertThat(params.captured.type).isEqualTo(StoryV2Type.ONBOARDING)
        assertThat(params.captured.source).isEqualTo(StoryV2Source.ONBOARDING)

        params.captured.onResult(StoriesV2Result.Dismissed)
        verify(exactly = 1) { model.onStoryResult(StoriesV2Result.Dismissed) }

        val action = mockk<StoriesV2PendingAction>()
        params.captured.onAction(action)
        verify(exactly = 1) { model.onStoryAction(action) }
    }

    private fun createComponent() = DefaultIntroductionComponent(
        appComponentContext = createAppComponentContext(),
        params = IntroductionComponent.Params(launchMode = InitScreenLaunchMode.Standard),
        storiesComponentFactory = storiesComponentFactory,
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

        // The stories slot consumes saved state on creation, and a relaxed mock would hand it a bare Object.
        val stateKeeper: StateKeeper = mockk(relaxed = true)
        every { stateKeeper.consume(any(), any<KSerializer<Any>>()) } returns null

        // The slot asks the context to build a child context; a relaxed mock would hand it a bare Object.
        val childContextFactory = ComponentContextFactory { _, _, _, _ ->
            mockk<ComponentContext>(relaxed = true)
        }

        return mockk<AppComponentContext>(relaxed = true).also {
            every { it.componentContextFactory } returns childContextFactory
            every { it.instanceKeeper } returns InstanceKeeperDispatcher()
            every { it.stateKeeper } returns stateKeeper
            every { it.tags } returns HashMap()
            every { it.hiltComponentBuilder } returns hiltComponentBuilder
            every { it.lifecycle } returns lifecycle
        }
    }
}