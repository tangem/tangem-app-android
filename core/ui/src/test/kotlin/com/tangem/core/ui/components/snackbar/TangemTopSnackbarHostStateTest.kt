package com.tangem.core.ui.components.snackbar

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class TangemTopSnackbarHostStateTest {

    private val state = TangemTopSnackbarHostState()

    @Test
    fun `GIVEN snackbar is displayed WHEN deeper host is registered THEN snackbar stays in the same host`() = runTest {
        // Arrange
        val rootHost = state.registerHost(accessibilityManager = null)
        backgroundScope.launch { state.showSnackbar(createMessage(text = "message")) }
        runCurrent()

        // Act
        val bottomSheetHost = state.registerHost(accessibilityManager = null)

        // Assert
        assertThat(state.isSnackbarHost(rootHost)).isTrue()
        assertThat(state.isSnackbarHost(bottomSheetHost)).isFalse()
    }

    @Test
    fun `GIVEN snackbar is displayed WHEN its host is unregistered THEN snackbar moves to the remaining host`() =
        runTest {
            // Arrange
            val rootHost = state.registerHost(accessibilityManager = null)
            val bottomSheetHost = state.registerHost(accessibilityManager = null)
            backgroundScope.launch { state.showSnackbar(createMessage(text = "message")) }
            runCurrent()

            // Act
            state.unregisterHost(bottomSheetHost)

            // Assert
            assertThat(state.isSnackbarHost(rootHost)).isTrue()
            assertThat(state.currentSnackbar).isNotNull()
        }

    @Test
    fun `GIVEN snackbar is displayed WHEN its host is changed THEN duration is not prolonged`() = runTest {
        // Arrange
        val rootHost = state.registerHost(accessibilityManager = null)
        val bottomSheetHost = state.registerHost(accessibilityManager = null)
        backgroundScope.launch {
            state.showSnackbar(createMessage(text = "message", duration = SnackbarMessage.Duration.Short))
        }
        runCurrent()

        // Act
        advanceTimeBy(SHORT_DURATION_MILLIS / 2)
        state.unregisterHost(bottomSheetHost)
        advanceTimeBy(SHORT_DURATION_MILLIS / 2 + 1)

        // Assert
        assertThat(state.currentSnackbar).isNull()
        assertThat(state.isSnackbarHost(rootHost)).isTrue()
    }

    @Test
    fun `GIVEN snackbar is displayed WHEN duration is not passed yet THEN snackbar is still displayed`() = runTest {
        // Arrange
        state.registerHost(accessibilityManager = null)
        backgroundScope.launch {
            state.showSnackbar(createMessage(text = "message", duration = SnackbarMessage.Duration.Short))
        }
        runCurrent()

        // Act
        advanceTimeBy(SHORT_DURATION_MILLIS - 1)

        // Assert
        assertThat(state.currentSnackbar).isNotNull()
    }

    @Test
    fun `GIVEN snackbar is displayed WHEN duration is passed THEN dismissal is requested once`() = runTest {
        // Arrange
        var dismissRequestsCount = 0
        state.registerHost(accessibilityManager = null)
        backgroundScope.launch {
            state.showSnackbar(createMessage(text = "message", onDismissRequest = { dismissRequestsCount++ }))
        }
        runCurrent()

        // Act
        advanceTimeBy(SHORT_DURATION_MILLIS + 1)

        // Assert
        assertThat(dismissRequestsCount).isEqualTo(1)
        assertThat(state.currentSnackbar).isNull()
    }

    @Test
    fun `GIVEN snackbar is displayed WHEN dismissal is requested THEN it is not requested again by timeout`() =
        runTest {
            // Arrange
            var dismissRequestsCount = 0
            state.registerHost(accessibilityManager = null)
            backgroundScope.launch {
                state.showSnackbar(createMessage(text = "message", onDismissRequest = { dismissRequestsCount++ }))
            }
            runCurrent()

            // Act
            state.currentSnackbar?.onDismissRequest?.invoke()
            runCurrent()
            advanceTimeBy(SHORT_DURATION_MILLIS + 1)

            // Assert
            assertThat(dismissRequestsCount).isEqualTo(1)
            assertThat(state.currentSnackbar).isNull()
        }

    @Test
    fun `GIVEN indefinite snackbar WHEN long time is passed THEN snackbar is still displayed`() = runTest {
        // Arrange
        state.registerHost(accessibilityManager = null)
        backgroundScope.launch {
            state.showSnackbar(createMessage(text = "message", duration = SnackbarMessage.Duration.Indefinite))
        }
        runCurrent()

        // Act
        advanceTimeBy(INDEFINITE_CHECK_MILLIS)

        // Assert
        assertThat(state.currentSnackbar).isNotNull()
    }

    @Test
    fun `GIVEN displayed snackbar is cancelled WHEN another one is shown THEN it replaces the cancelled one`() =
        runTest {
            // Arrange
            state.registerHost(accessibilityManager = null)
            val replacedMessage = createMessage(text = "replaced")
            val displayingJob = backgroundScope.launch { state.showSnackbar(replacedMessage) }
            runCurrent()

            // Act
            displayingJob.cancel()
            val newMessage = createMessage(text = "new")
            backgroundScope.launch { state.showSnackbar(newMessage) }
            runCurrent()

            // Assert
            assertThat(state.currentSnackbar?.message).isEqualTo(newMessage.message)
        }

    private fun createMessage(
        text: String,
        duration: SnackbarMessage.Duration = SnackbarMessage.Duration.Short,
        onDismissRequest: () -> Unit = {},
    ) = SnackbarMessage(message = stringReference(text), duration = duration, onDismissRequest = onDismissRequest)

    private companion object {
        const val SHORT_DURATION_MILLIS = 4000L
        const val INDEFINITE_CHECK_MILLIS = 60 * 60 * 1000L
    }
}