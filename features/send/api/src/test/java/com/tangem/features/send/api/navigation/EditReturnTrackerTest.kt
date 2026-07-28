package com.tangem.features.send.api.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class EditReturnTrackerTest {

    private sealed interface TestRoute {
        val isEditMode: Boolean

        data class Amount(override val isEditMode: Boolean) : TestRoute
        data class Destination(override val isEditMode: Boolean) : TestRoute
        data object Confirm : TestRoute {
            override val isEditMode: Boolean = false
        }
        data object Success : TestRoute {
            override val isEditMode: Boolean = false
        }
    }

    private val tracker = EditReturnTracker<TestRoute> { it.isEditMode }

    @Test
    fun `GIVEN no previous route WHEN first route activated THEN no edit return detected`() {
        val isReturnedFromEdit = tracker.onRouteActivated(TestRoute.Amount(isEditMode = false))

        assertThat(isReturnedFromEdit).isFalse()
    }

    @Test
    fun `GIVEN linear flow WHEN confirm activated first time THEN no edit return detected`() {
        // Arrange
        tracker.onRouteActivated(TestRoute.Amount(isEditMode = false))
        tracker.onRouteActivated(TestRoute.Destination(isEditMode = false))

        // Act
        val isReturnedFromEdit = tracker.onRouteActivated(TestRoute.Confirm)

        // Assert
        assertThat(isReturnedFromEdit).isFalse()
    }

    @Test
    fun `GIVEN amount edited from confirm WHEN popped back to confirm THEN edit return detected`() {
        // Arrange: [REDACTED_TASK_KEY] reproduction — Amount -> Destination -> Confirm -> Amount(edit) -> Confirm
        tracker.onRouteActivated(TestRoute.Amount(isEditMode = false))
        tracker.onRouteActivated(TestRoute.Destination(isEditMode = false))
        tracker.onRouteActivated(TestRoute.Confirm)
        tracker.onRouteActivated(TestRoute.Amount(isEditMode = true))

        // Act
        val isReturnedFromEdit = tracker.onRouteActivated(TestRoute.Confirm)

        // Assert
        assertThat(isReturnedFromEdit).isTrue()
    }

    @Test
    fun `GIVEN destination edited from confirm WHEN popped back to confirm THEN edit return detected`() {
        // Arrange
        tracker.onRouteActivated(TestRoute.Confirm)
        tracker.onRouteActivated(TestRoute.Destination(isEditMode = true))

        // Act
        val isReturnedFromEdit = tracker.onRouteActivated(TestRoute.Confirm)

        // Assert
        assertThat(isReturnedFromEdit).isTrue()
    }

    @Test
    fun `GIVEN edit return consumed WHEN next route activated THEN no edit return detected`() {
        // Arrange
        tracker.onRouteActivated(TestRoute.Confirm)
        tracker.onRouteActivated(TestRoute.Amount(isEditMode = true))
        tracker.onRouteActivated(TestRoute.Confirm)

        // Act
        val isReturnedFromEdit = tracker.onRouteActivated(TestRoute.Success)

        // Assert
        assertThat(isReturnedFromEdit).isFalse()
    }

    @Test
    fun `GIVEN consecutive edits WHEN each pops back to confirm THEN each return detected independently`() {
        tracker.onRouteActivated(TestRoute.Confirm)

        assertThat(tracker.onRouteActivated(TestRoute.Amount(isEditMode = true))).isFalse()
        assertThat(tracker.onRouteActivated(TestRoute.Confirm)).isTrue()
        assertThat(tracker.onRouteActivated(TestRoute.Destination(isEditMode = true))).isFalse()
        assertThat(tracker.onRouteActivated(TestRoute.Confirm)).isTrue()
    }

    @Test
    fun `GIVEN confirm re-entered from non-edit route WHEN confirm activated THEN no edit return detected`() {
        // Arrange: back from Confirm to Destination step, then Next re-pushes Confirm
        tracker.onRouteActivated(TestRoute.Destination(isEditMode = false))
        tracker.onRouteActivated(TestRoute.Confirm)
        tracker.onRouteActivated(TestRoute.Destination(isEditMode = false))

        // Act
        val isReturnedFromEdit = tracker.onRouteActivated(TestRoute.Confirm)

        // Assert
        assertThat(isReturnedFromEdit).isFalse()
    }
}