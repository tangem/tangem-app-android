package com.tangem.feature.usedesk.visibility

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSupportChatVisibilityTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN lifecycle event sequence WHEN events applied THEN chat visibility matches`(model: Model) {
        // Arrange
        val visibility = DefaultSupportChatVisibility()

        // Act
        model.events.forEach { event ->
            when (event) {
                Event.RESUME -> visibility.onResumed()
                Event.PAUSE -> visibility.onPaused()
            }
        }

        // Assert
        assertThat(visibility.isChatVisible).isEqualTo(model.expectedVisible)
    }

    enum class Event { RESUME, PAUSE }

    data class Model(val events: List<Event>, val expectedVisible: Boolean)

    private fun provideTestModels() = listOf(
        Model(events = emptyList(), expectedVisible = false),
        Model(events = listOf(Event.RESUME), expectedVisible = true),
        Model(events = listOf(Event.RESUME, Event.PAUSE), expectedVisible = false),
        Model(events = listOf(Event.RESUME, Event.RESUME, Event.PAUSE), expectedVisible = true),
        Model(events = listOf(Event.RESUME, Event.RESUME, Event.PAUSE, Event.PAUSE), expectedVisible = false),
        Model(events = listOf(Event.PAUSE), expectedVisible = false),
        Model(events = listOf(Event.PAUSE, Event.RESUME), expectedVisible = true),
        Model(events = listOf(Event.PAUSE, Event.PAUSE, Event.RESUME, Event.PAUSE), expectedVisible = false),
    )
}