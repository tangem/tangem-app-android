package com.tangem.feature.usedesk

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
import com.tangem.feature.usedesk.api.UseDeskFeatureToggles
import com.tangem.feature.usedesk.api.UsedeskComponent
import com.tangem.feature.usedesk.model.UsedeskModel
import com.tangem.feature.usedesk.visibility.DefaultSupportChatVisibility
import dagger.hilt.EntryPoints
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import javax.inject.Provider

/** A relaxed lifecycle mock would never advance, so the registry here is real. */
internal class DefaultUsedeskComponentTest {

    private val model: UsedeskModel = mockk(relaxed = true)
    private val useDeskFeatureToggles: UseDeskFeatureToggles = mockk()
    private val supportChatVisibility = DefaultSupportChatVisibility()
    private val lifecycle = LifecycleRegistry()

    @AfterEach
    fun unmockEntryPoints() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun `GIVEN chat created WHEN lifecycle resumes THEN chat is visible`() {
        // Arrange
        createComponent()
        lifecycle.create()
        lifecycle.start()

        // Act
        lifecycle.resume()

        // Assert
        assertThat(supportChatVisibility.isChatVisible).isTrue()
    }

    @Test
    fun `GIVEN resumed chat WHEN lifecycle pauses THEN chat is hidden`() {
        // Arrange
        createComponent()
        lifecycle.create()
        lifecycle.start()
        lifecycle.resume()

        // Act
        lifecycle.pause()

        // Assert
        assertThat(supportChatVisibility.isChatVisible).isFalse()
    }

    @Test
    fun `GIVEN chat created WHEN never resumed THEN chat is hidden`() {
        // Act
        createComponent()
        lifecycle.create()
        lifecycle.start()

        // Assert
        assertThat(supportChatVisibility.isChatVisible).isFalse()
    }

    private fun createComponent() = DefaultUsedeskComponent(
        appComponentContext = createAppComponentContext(),
        params = UsedeskComponent.Params(walletMetaInfo = mockk()),
        supportChatVisibility = supportChatVisibility,
        useDeskFeatureToggles = useDeskFeatureToggles,
    )

    private fun createAppComponentContext(): AppComponentContext {
        val hiltComponentBuilder: ModelComponent.Builder = mockk()
        every { hiltComponentBuilder.router(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.uiMessageSender(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.paramsContainer(any()) } returns hiltComponentBuilder
        every { hiltComponentBuilder.build() } returns mockk()

        val entryPoint: ModelsEntryPoint = mockk()
        every { entryPoint.models() } returns mapOf(
            UsedeskModel::class.java to Provider<Model> { model },
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