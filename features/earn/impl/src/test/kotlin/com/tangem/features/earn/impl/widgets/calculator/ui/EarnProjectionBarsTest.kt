package com.tangem.features.earn.impl.widgets.calculator.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnProjectionBarUM
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class EarnProjectionBarsTest {

    @Nested
    inner class SelectBar {

        private val haptic: HapticManager = mockk(relaxed = true)
        private val selected = mutableListOf<Int?>()

        @Test
        fun `GIVEN nothing selected WHEN first bar touched THEN it is selected with a context click`() {
            // Act
            val actual = selectBar(index = 3, previous = null, haptic = haptic, onBarSelect = { selected += it })

            // Assert
            assertThat(actual).isEqualTo(3)
            assertThat(selected).containsExactly(3)
            verify(exactly = 1) { haptic.perform(TangemHapticEffect.View.ContextClick) }
        }

        @Test
        fun `GIVEN a bar selected WHEN finger moves to another bar THEN it is selected with a text handle tick`() {
            // Act
            val actual = selectBar(index = 4, previous = 3, haptic = haptic, onBarSelect = { selected += it })

            // Assert
            assertThat(actual).isEqualTo(4)
            assertThat(selected).containsExactly(4)
            verify(exactly = 1) { haptic.perform(TangemHapticEffect.View.TextHandleMove) }
            verify(exactly = 1) { haptic.perform(any()) }
        }

        @Test
        fun `GIVEN a bar selected WHEN finger stays on the same bar THEN nothing repeats`() {
            // Act
            val actual = selectBar(index = 3, previous = 3, haptic = haptic, onBarSelect = { selected += it })

            // Assert
            assertThat(actual).isEqualTo(3)
            assertThat(selected).isEmpty()
            verify(exactly = 0) { haptic.perform(any()) }
        }

        @Test
        fun `GIVEN a bar selected WHEN gesture ends THEN the selection clears without a haptic`() {
            // Act
            val actual = selectBar(index = null, previous = 3, haptic = haptic, onBarSelect = { selected += it })

            // Assert
            assertThat(actual).isNull()
            assertThat(selected).containsExactly(null)
            verify(exactly = 0) { haptic.perform(any()) }
        }

        @Test
        fun `GIVEN nothing selected WHEN gesture ends THEN nothing is reported`() {
            // Act
            val actual = selectBar(index = null, previous = null, haptic = haptic, onBarSelect = { selected += it })

            // Assert
            assertThat(actual).isNull()
            assertThat(selected).isEmpty()
            verify(exactly = 0) { haptic.perform(any()) }
        }
    }

    @Nested
    inner class TouchedBar {

        @Test
        fun `GIVEN drawn bars WHEN touched THEN the nearest bar is returned`() {
            // Arrange
            val geometry = EarnBarsGeometry(width = WIDTH, barCount = 3, maxBarWidth = MAX_BAR_WIDTH)
            val bars = listOf(bar(height = 0.2f), bar(height = 0.5f), bar(height = 1f))

            // Act
            val actual = geometry.touchedBar(x = geometry.barCenter(2), bars = bars)

            // Assert
            assertThat(actual).isEqualTo(2)
        }

        @Test
        fun `GIVEN one drawn bar among empty ones WHEN touched THEN any nearest bar is returned`() {
            // Arrange
            val geometry = EarnBarsGeometry(width = WIDTH, barCount = 3, maxBarWidth = MAX_BAR_WIDTH)
            val bars = listOf(bar(height = 0f), bar(height = 0f), bar(height = 0.3f))

            // Act
            val actual = geometry.touchedBar(x = geometry.barCenter(0), bars = bars)

            // Assert
            assertThat(actual).isEqualTo(0)
        }

        @Test
        fun `GIVEN no bar is drawn WHEN touched THEN nothing is selected`() {
            // Arrange
            val geometry = EarnBarsGeometry(width = WIDTH, barCount = 3, maxBarWidth = MAX_BAR_WIDTH)
            val bars = listOf(bar(height = 0f), bar(height = 0f), bar(height = 0f))

            // Act
            val actual = geometry.touchedBar(x = geometry.barCenter(1), bars = bars)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN chart without width WHEN touched THEN nothing is selected`() {
            // Arrange
            val geometry = EarnBarsGeometry(width = 0f, barCount = 3, maxBarWidth = MAX_BAR_WIDTH)
            val bars = listOf(bar(height = 0.2f), bar(height = 0.5f), bar(height = 1f))

            // Act
            val actual = geometry.touchedBar(x = 0f, bars = bars)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN no bars WHEN touched THEN nothing is selected`() {
            // Arrange
            val geometry = EarnBarsGeometry(width = WIDTH, barCount = 0, maxBarWidth = MAX_BAR_WIDTH)

            // Act
            val actual = geometry.touchedBar(x = 10f, bars = emptyList())

            // Assert
            assertThat(actual).isNull()
        }

        private fun bar(height: Float) = EarnProjectionBarUM(
            height = height,
            balance = TextReference.EMPTY,
            earned = TextReference.EMPTY,
        )
    }

    private companion object {
        const val WIDTH = 100f
        const val MAX_BAR_WIDTH = 6f
    }
}