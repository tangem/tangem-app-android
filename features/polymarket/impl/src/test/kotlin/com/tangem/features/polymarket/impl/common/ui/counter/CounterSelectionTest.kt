package com.tangem.features.polymarket.impl.common.ui.counter

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class CounterSelectionTest {

    private val selection = CounterSelection()
    private val initial = CounterInput(listOf("first", "middle", "last"), selectedId = "middle")

    @Test
    fun `GIVEN stopped last page with pixel rounding WHEN checking settlement THEN selection is accepted`() {
        // Arrange
        val pageSize = 153
        val offset = 0.5f / pageSize

        // Act
        val settled = isCounterSettled(isScrolling = false, pageOffsetFraction = offset, pageSize = pageSize)

        // Assert
        assertThat(settled).isTrue()
    }

    @Test
    fun `GIVEN gesture passing through center WHEN checking settlement THEN selection is deferred`() {
        assertThat(isCounterSettled(isScrolling = true, pageOffsetFraction = 0f, pageSize = 153)).isFalse()
    }

    @Test
    fun `GIVEN interrupted animation between values WHEN checking settlement THEN selection is deferred`() {
        assertThat(isCounterSettled(isScrolling = false, pageOffsetFraction = 0.25f, pageSize = 153)).isFalse()
    }

    @Test
    fun `GIVEN initial selection WHEN positioned THEN no user change emitted`() {
        // Arrange
        selection.needsPositioning(initial)

        // Act
        selection.synchronized(initial)

        // Assert
        assertThat(selection.selection(initial, settledIndex = 1)).isNull()
    }

    @Test
    fun `GIVEN user settles a new value WHEN observed repeatedly THEN only one change emitted`() {
        // Arrange
        synchronize(initial)

        // Act
        val first = selection.selection(initial, settledIndex = 2)
        val repeated = selection.selection(initial, settledIndex = 2)

        // Assert
        assertThat(first).isEqualTo("last")
        assertThat(repeated).isNull()
    }

    @Test
    fun `GIVEN a new user selection WHEN parent acknowledges THEN ongoing scrolling is preserved`() {
        // Arrange
        synchronize(initial)
        selection.selection(initial, settledIndex = 2)
        val acknowledgement = initial.copy(selectedId = "last")

        // Act
        val needsPositioning = selection.needsPositioning(acknowledgement)
        selection.synchronized(acknowledgement)

        // Assert
        assertThat(needsPositioning).isFalse()
        assertThat(selection.selection(acknowledgement, settledIndex = 2)).isNull()
        assertThat(selection.selection(acknowledgement, settledIndex = 0)).isEqualTo("first")
    }

    @Test
    fun `GIVEN external selection WHEN applied THEN row repositions without user callback`() {
        // Arrange
        synchronize(initial)
        val external = initial.copy(selectedId = "first")

        // Act
        val needsPositioning = selection.needsPositioning(external)
        val oldPosition = selection.selection(external, settledIndex = 1)
        selection.synchronized(external)

        // Assert
        assertThat(needsPositioning).isTrue()
        assertThat(oldPosition).isNull()
        assertThat(selection.selection(external, settledIndex = 0)).isNull()
    }

    @Test
    fun `GIVEN reordered items WHEN old index observed THEN no wrong identity is selected`() {
        // Arrange
        synchronize(initial)
        val reordered = initial.copy(ids = listOf("middle", "last", "first"))

        // Act
        val needsPositioning = selection.needsPositioning(reordered)
        val stale = selection.selection(reordered, settledIndex = 1)
        selection.synchronized(reordered)

        // Assert
        assertThat(needsPositioning).isTrue()
        assertThat(stale).isNull()
        assertThat(selection.selection(reordered, settledIndex = 0)).isNull()
    }

    @Test
    fun `GIVEN selected item removed WHEN row settles THEN component invents no replacement`() {
        // Arrange
        synchronize(initial)
        val missing = initial.copy(ids = listOf("first", "last"))
        synchronize(missing)

        // Act
        val replacement = selection.selection(missing, settledIndex = 0)

        // Assert
        assertThat(replacement).isNull()
    }

    @Test
    fun `GIVEN empty list WHEN old page settles THEN no change emitted`() {
        // Arrange
        synchronize(initial)
        val empty = CounterInput(emptyList(), selectedId = null)
        synchronize(empty)

        // Act
        val result = selection.selection(empty, settledIndex = 2)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN refreshed input WHEN late old snapshot arrives THEN it cannot change selection`() {
        // Arrange
        synchronize(initial)
        val replacement = CounterInput(listOf("new-first", "new-last"), selectedId = "new-last")
        synchronize(replacement)

        // Act
        val stale = selection.selection(initial, settledIndex = 0)

        // Assert
        assertThat(stale).isNull()
    }

    private fun synchronize(input: CounterInput) {
        selection.needsPositioning(input)
        selection.synchronized(input)
    }
}