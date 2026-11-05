package com.tangem.features.polymarket.impl.common.ui.chart

import com.google.common.truth.Truth.assertThat
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerDrawingModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerDrawingModel.PointInfo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ProbabilityDrawingModelInterpolatorTest {

    @Test
    fun `GIVEN appended point WHEN animating THEN tail continues from previous endpoint`() = runTest {
        // Arrange
        val interpolator = ProbabilityDrawingModelInterpolator()
        val old = model(points = mapOf(0.0 to 0.25f, 1.0 to 0.5f))
        val new = model(points = mapOf(0.0 to 0.5f, 1.0 to 0.75f, 2.0 to 1f))

        // Act
        interpolator.setModels(old, new)
        val frames = listOf(0f, 0.5f, 1f).map { interpolator.transform(it) }

        // Assert
        assertThat(frames).containsExactly(
            model(points = mapOf(0.0 to 0.25f, 1.0 to 0.5f, 2.0 to 0.5f)),
            model(points = mapOf(0.0 to 0.375f, 1.0 to 0.625f, 2.0 to 0.75f)),
            new,
        ).inOrder()
    }

    @Test
    fun `GIVEN several series WHEN appending a batch THEN each tail starts from its own endpoint`() = runTest {
        // Arrange
        val interpolator = ProbabilityDrawingModelInterpolator()
        val old = LineCartesianLayerDrawingModel(
            pointInfo = listOf(mapOf(0.0 to PointInfo(0.75f)), mapOf(0.0 to PointInfo(0.25f))),
            zeroY = 1f,
        )
        val new = LineCartesianLayerDrawingModel(
            pointInfo = listOf(
                mapOf(0.0 to PointInfo(0.75f), 1.0 to PointInfo(0.5f), 2.0 to PointInfo(0.25f)),
                mapOf(0.0 to PointInfo(0.25f), 1.0 to PointInfo(0.5f), 2.0 to PointInfo(0.75f)),
            ),
            zeroY = 1f,
        )

        // Act
        interpolator.setModels(old, new)
        val firstFrame = interpolator.transform(0f)

        // Assert
        assertThat(firstFrame).isEqualTo(
            LineCartesianLayerDrawingModel(
                pointInfo = listOf(
                    mapOf(0.0 to PointInfo(0.75f), 1.0 to PointInfo(0.75f), 2.0 to PointInfo(0.75f)),
                    mapOf(0.0 to PointInfo(0.25f), 1.0 to PointInfo(0.25f), 2.0 to PointInfo(0.25f)),
                ),
                zeroY = 1f,
            ),
        )
    }

    @Test
    fun `GIVEN unfinished transition WHEN another update arrives THEN animation continues from visible frame`() = runTest {
        // Arrange
        val interpolator = ProbabilityDrawingModelInterpolator()
        interpolator.setModels(model(points = mapOf(0.0 to 0.25f)), model(points = mapOf(0.0 to 0.75f)))
        val visible = interpolator.transform(0.5f)

        // Act
        interpolator.setModels(visible, model(points = mapOf(0.0 to 1f, 1.0 to 0.75f)))
        val firstFrame = interpolator.transform(0f)

        // Assert
        assertThat(firstFrame).isEqualTo(model(points = mapOf(0.0 to 0.5f, 1.0 to 0.5f)))
    }

    @Test
    fun `GIVEN history backfill WHEN animating THEN earlier point does not extend the latest endpoint backwards`() = runTest {
        // Arrange
        val interpolator = ProbabilityDrawingModelInterpolator()
        val new = model(points = mapOf(0.0 to 0.25f, 1.0 to 0.75f))

        // Act
        interpolator.setModels(model(points = mapOf(1.0 to 0.75f)), new)
        val firstFrame = interpolator.transform(0f)

        // Assert
        assertThat(firstFrame).isEqualTo(new)
    }

    @Test
    fun `GIVEN first data WHEN initial animation is skipped THEN all values are immediately final`() = runTest {
        // Arrange
        val interpolator = ProbabilityDrawingModelInterpolator()
        val new = model()

        // Act
        interpolator.setModels(null, new)
        val frame = interpolator.transform(1f)

        // Assert
        assertThat(frame).isEqualTo(new)
    }

    private fun model(points: Map<Double, Float> = mapOf(0.0 to 0.5f)): LineCartesianLayerDrawingModel =
        LineCartesianLayerDrawingModel(pointInfo = listOf(points.mapValues { PointInfo(it.value) }), zeroY = 1f)
}