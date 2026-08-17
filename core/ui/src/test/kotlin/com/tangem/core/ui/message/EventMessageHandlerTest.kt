package com.tangem.core.ui.message

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.stringReference
import org.junit.jupiter.api.Test

internal class EventMessageHandlerTest {

    private val handler = EventMessageHandler()

    @Test
    fun `GIVEN message sent WHEN consumed THEN event is consumed`() {
        // Arrange
        handler.handleMessage(createMessage(text = "message"))
        val event = handler.value as StateEvent.Triggered

        // Act
        event.onConsume()

        // Assert
        assertThat(handler.value).isInstanceOf(StateEvent.Consumed::class.java)
    }

    @Test
    fun `GIVEN message replaced by another one WHEN replaced is consumed THEN new event stays triggered`() {
        // Arrange
        val replacedMessage = createMessage(text = "replaced")
        val newMessage = createMessage(text = "new")

        handler.handleMessage(replacedMessage)
        val replacedEvent = handler.value as StateEvent.Triggered
        handler.handleMessage(newMessage)

        // Act
        replacedEvent.onConsume()

        // Assert
        val actual = handler.value
        assertThat(actual).isInstanceOf(StateEvent.Triggered::class.java)
        assertThat((actual as StateEvent.Triggered).data).isSameInstanceAs(newMessage)
    }

    @Test
    fun `GIVEN equal message sent twice WHEN second is sent THEN new event is triggered`() {
        // Arrange
        handler.handleMessage(createMessage(text = "message"))
        val firstEvent = handler.value

        // Act
        handler.handleMessage(createMessage(text = "message"))

        // Assert
        assertThat(handler.value).isNotEqualTo(firstEvent)
        assertThat(handler.value).isInstanceOf(StateEvent.Triggered::class.java)
    }

    @Test
    fun `GIVEN not an event message WHEN handled THEN event is not triggered`() {
        // Act
        handler.handleMessage(object : UiMessage {})

        // Assert
        assertThat(handler.value).isInstanceOf(StateEvent.Consumed::class.java)
    }

    private fun createMessage(text: String) = SnackbarMessage(message = stringReference(text))
}