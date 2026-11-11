package com.tangem.features.earn.impl.widgets.chart.ui

import com.google.common.truth.Truth.assertThat
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BarSelectionListenerTest {

    private val haptic: HapticManager = mockk(relaxed = true)
    private val marker: CartesianMarker = mockk()
    private val selected = mutableListOf<Int?>()

    private val listener = BarSelectionListener(haptic = haptic, onBarSelect = { selected += it })

    @BeforeEach
    fun resetMocks() {
        clearMocks(haptic)
        selected.clear()
    }

    @Test
    fun `GIVEN nothing selected WHEN shown on a bar THEN the bar is selected with a context click`() {
        // Act
        listener.onShown(marker, target(bar = 3))

        // Assert
        assertThat(selected).containsExactly(3)
        verify(exactly = 1) { haptic.perform(TangemHapticEffect.View.ContextClick) }
    }

    @Test
    fun `GIVEN a bar selected WHEN updated with the same bar THEN neither the selection nor the haptic repeats`() {
        // Arrange
        listener.onShown(marker, target(bar = 3))

        // Act
        listener.onUpdated(marker, target(bar = 3))

        // Assert
        assertThat(selected).containsExactly(3)
        verify(exactly = 1) { haptic.perform(any()) }
    }

    @Test
    fun `GIVEN a bar selected WHEN updated with another bar THEN that bar is selected with a text handle tick`() {
        // Arrange
        listener.onShown(marker, target(bar = 3))

        // Act
        listener.onUpdated(marker, target(bar = 4))

        // Assert
        assertThat(selected).containsExactly(3, 4).inOrder()
        verify(exactly = 1) { haptic.perform(TangemHapticEffect.View.TextHandleMove) }
    }

    @Test
    fun `GIVEN a bar selected WHEN hidden and shown on the same bar THEN the selection clears and is made again`() {
        // Arrange
        listener.onShown(marker, target(bar = 3))

        // Act
        listener.onHidden(marker)
        listener.onShown(marker, target(bar = 3))

        // Assert
        assertThat(selected).containsExactly(3, null, 3).inOrder()
        verify(exactly = 2) { haptic.perform(TangemHapticEffect.View.ContextClick) }
    }

    private fun target(bar: Int): List<CartesianMarker.Target> = listOf(
        object : ColumnCartesianLayerMarkerTarget {
            override val x: Double = bar.toDouble()
            override val canvasX: Float = 0f
            override val columns: List<ColumnCartesianLayerMarkerTarget.Column> = emptyList()
        },
    )
}