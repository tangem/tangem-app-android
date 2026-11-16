package com.tangem.features.polymarket.impl.common.ui.chart

import com.google.common.truth.Truth.assertThat
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.MutableChartValues
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class ProbabilityChartTimeScaleTest {

    @ParameterizedTest
    @ValueSource(longs = [60_000, 360_000, 1_440_000, 4_320_000])
    fun `GIVEN uniformly spaced epoch milliseconds WHEN Vico updates its time scale THEN every period is accepted`(
        spacingMillis: Long,
    ) {
        // Arrange
        val timestamps = List(61) { 1_793_613_600_123L + it * spacingMillis }
        val layer = LineCartesianLayerModel.build { series(timestamps, List(61) { 0.5 }) }
        val model = CartesianChartModel(layer)
        val values = MutableChartValues()

        // Act
        values.update(probabilityChartXStep(model), model)

        // Assert
        assertThat(values.xStep).isEqualTo(1.0)
        assertThat(values.model).isSameInstanceAs(model)
        assertThat(layer.series.single().map { it.x }).containsExactlyElementsIn(timestamps.map { it.toDouble() }).inOrder()
    }
}