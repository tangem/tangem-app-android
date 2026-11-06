package com.tangem.core.ui.ds2.shape

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

internal class SmoothCornerShapeTest {

    @Test
    fun `GIVEN zero smoothing WHEN smoothCornerParams THEN corner is a plain quarter circle`() {
        // Act
        val actual = smoothCornerParams(radius = 20f, smoothing = 0f, budget = 100f)

        // Assert
        assertThat(actual.p).isWithin(TOLERANCE).of(20f)
        assertThat(actual.arcMeasure).isWithin(TOLERANCE).of(90f)
        listOf(actual.a, actual.b, actual.c, actual.d).forEach { assertThat(it).isWithin(TOLERANCE).of(0f) }
    }

    @Test
    fun `GIVEN side too short for smoothing WHEN smoothCornerParams THEN smoothing is reduced to fit`() {
        // Act
        val actual = smoothCornerParams(radius = 20f, smoothing = 0.6f, budget = 28f)

        // Assert
        assertThat(actual.p).isWithin(TOLERANCE).of(28f)
        assertThat(actual.arcMeasure).isWithin(TOLERANCE).of(54f)
    }

    @Test
    fun `GIVEN smoothing WHEN smoothCornerParams THEN transition curve ends where the arc starts`() {
        // Arrange
        val radius = 20f
        val smoothing = 0.6f
        val alpha = Math.toRadians(45.0 * smoothing).toFloat()

        // Act
        val actual = smoothCornerParams(radius = radius, smoothing = smoothing, budget = 100f)

        // Assert
        val curveEndX = -actual.p + actual.a + actual.b + actual.c
        val arcStartX = -radius + radius * sin(alpha)
        val arcStartY = radius - radius * cos(alpha)
        assertThat(curveEndX).isWithin(TOLERANCE).of(arcStartX)
        assertThat(actual.d).isWithin(TOLERANCE).of(arcStartY)
    }

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}