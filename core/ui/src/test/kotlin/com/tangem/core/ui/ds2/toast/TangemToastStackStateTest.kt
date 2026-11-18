package com.tangem.core.ui.ds2.toast

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.TangemToastMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemToastStackStateTest {

    private lateinit var state: TangemToastStackState

    @BeforeEach
    fun setUp() {
        state = TangemToastStackState().apply { registerHost(accessibilityManager = null) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Timeout {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun `GIVEN toast WHEN its time is up THEN it is dismissed after show plus hold`(model: TimeoutModel) =
            runTest {
                // Arrange
                var dismissCount = 0
                val message = createMessage(duration = model.duration, onDismissRequest = { dismissCount++ })

                // Act
                showInBackground(message)
                state.entries.single().isMultiLine = model.isMultiLine
                advanceTimeBy(model.expectedMillis - 1)
                val isVisibleBeforeTimeout = state.entries.single().isVisible
                advanceTimeBy(2)

                // Assert
                assertThat(isVisibleBeforeTimeout).isTrue()
                assertThat(state.entries.single().isVisible).isFalse()
                assertThat(dismissCount).isEqualTo(1)
            }

        private fun provideTestModels() = listOf(
            TimeoutModel(duration = TangemToast.Duration.Short, isMultiLine = false, expectedMillis = 2350),
            TimeoutModel(duration = TangemToast.Duration.Long, isMultiLine = false, expectedMillis = 3350),
            TimeoutModel(duration = TangemToast.Duration.Short, isMultiLine = true, expectedMillis = 3350),
        )
    }

    data class TimeoutModel(val duration: TangemToast.Duration, val isMultiLine: Boolean, val expectedMillis: Long)

    @Nested
    inner class Show {

        @Test
        fun `GIVEN visible toast WHEN another is shown THEN the newest is first`() = runTest {
            // Arrange
            val first = createMessage(label = "first")
            val second = createMessage(label = "second")

            // Act
            showInBackground(first)
            showInBackground(second)

            // Assert
            assertThat(state.entries.map { it.message }).containsExactly(second, first).inOrder()
        }

        @Test
        fun `GIVEN three visible toasts WHEN a fourth is shown THEN the oldest is dismissed`() = runTest {
            // Arrange
            var oldestDismissCount = 0
            showInBackground(createMessage(label = "oldest", onDismissRequest = { oldestDismissCount++ }))
            showInBackground(createMessage(label = "2"))
            showInBackground(createMessage(label = "3"))

            // Act
            showInBackground(createMessage(label = "4"))

            // Assert
            assertThat(state.entries.map { it.isVisible }).containsExactly(true, true, true, false).inOrder()
            assertThat(oldestDismissCount).isEqualTo(1)
        }

        @Test
        fun `GIVEN dismissed toast WHEN its exit animation finishes THEN it is removed`() = runTest {
            // Arrange
            showInBackground(createMessage())
            val entry = state.entries.single()
            state.close(entry)
            runCurrent()

            // Act
            state.onExitFinished(entry)

            // Assert
            assertThat(state.entries).isEmpty()
        }
    }

    @Nested
    inner class Dismiss {

        @Test
        fun `GIVEN visible toast WHEN close is tapped THEN it hides and reports the dismissal`() = runTest {
            // Arrange
            var dismissCount = 0
            showInBackground(createMessage(onDismissRequest = { dismissCount++ }))

            // Act
            state.close(state.entries.single())
            runCurrent()

            // Assert
            assertThat(state.entries.single().isVisible).isFalse()
            assertThat(dismissCount).isEqualTo(1)
        }

        @Test
        fun `GIVEN toast with action WHEN action is tapped THEN action runs and dismissal is not reported`() =
            runTest {
                // Arrange
                var actionCount = 0
                var dismissCount = 0
                showInBackground(
                    createMessage(action = { actionCount++ }, onDismissRequest = { dismissCount++ }),
                )
                val entry = state.entries.single()

                // Act
                state.performAction(entry)
                state.performAction(entry)
                runCurrent()

                // Assert
                assertThat(entry.isVisible).isFalse()
                assertThat(actionCount).isEqualTo(1)
                assertThat(dismissCount).isEqualTo(0)
            }

        @Test
        fun `GIVEN showing caller WHEN it is cancelled THEN toast hides without callbacks`() = runTest {
            // Arrange
            var dismissCount = 0
            val job = backgroundScope.launch { state.show(createMessage(onDismissRequest = { dismissCount++ })) }
            runCurrent()

            // Act
            job.cancel()
            runCurrent()

            // Assert
            assertThat(state.entries.single().isVisible).isFalse()
            assertThat(dismissCount).isEqualTo(0)
        }
    }

    @Nested
    inner class Hosts {

        @Test
        fun `GIVEN sheet stack registered WHEN toast is shown THEN the sheet stack owns it`() = runTest {
            // Arrange
            val sheetDepth = state.registerHost(accessibilityManager = null)

            // Act
            showInBackground(createMessage())

            // Assert
            assertThat(state.entries.single().hostDepth).isEqualTo(sheetDepth)
        }

        @Test
        fun `GIVEN toast owned by sheet stack WHEN sheet stack leaves THEN toast is removed and dismissal reported`() =
            runTest {
                // Arrange
                var dismissCount = 0
                val sheetDepth = state.registerHost(accessibilityManager = null)
                showInBackground(createMessage(onDismissRequest = { dismissCount++ }))

                // Act
                state.unregisterHost(sheetDepth)
                runCurrent()

                // Assert
                assertThat(state.entries).isEmpty()
                assertThat(dismissCount).isEqualTo(1)
            }

        @Test
        fun `GIVEN toast owned by root stack WHEN sheet stack leaves THEN root stack keeps it`() = runTest {
            // Arrange
            showInBackground(createMessage())
            val sheetDepth = state.registerHost(accessibilityManager = null)

            // Act
            state.unregisterHost(sheetDepth)

            // Assert
            assertThat(state.entries.single().hostDepth).isEqualTo(sheetDepth - 1)
        }

        @Test
        fun `GIVEN no stack registered WHEN toast is dismissed THEN it is removed at once`() = runTest {
            // Arrange
            val orphanState = TangemToastStackState()
            backgroundScope.launch { orphanState.show(createMessage()) }
            runCurrent()

            // Act
            orphanState.close(orphanState.entries.single())
            runCurrent()

            // Assert
            assertThat(orphanState.entries).isEmpty()
        }
    }

    private fun TestScope.showInBackground(message: TangemToastMessage) {
        backgroundScope.launch { state.show(message) }
        runCurrent()
    }

    private fun createMessage(
        label: String = "Toast label",
        duration: TangemToast.Duration = TangemToast.Duration.Short,
        action: (() -> Unit)? = null,
        onDismissRequest: () -> Unit = {},
    ) = TangemToastMessage(
        label = stringReference(label),
        duration = duration,
        actionLabel = action?.let { stringReference("Action") },
        action = action,
        onDismissRequest = onDismissRequest,
    )
}