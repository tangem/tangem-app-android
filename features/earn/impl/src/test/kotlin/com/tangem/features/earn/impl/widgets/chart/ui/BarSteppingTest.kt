package com.tangem.features.earn.impl.widgets.chart.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BarSteppingTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a selection and a step WHEN steppedBar THEN the neighbouring bar is chosen within the chart`(
        model: SteppingModel,
    ) {
        // Act
        val actual = steppedBar(selected = model.selected, barCount = model.barCount, step = model.step)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        SteppingModel(name = "nothing selected, previous", selected = null, barCount = 5, step = -1, expected = 4),
        SteppingModel(name = "nothing selected, next", selected = null, barCount = 5, step = 1, expected = 4),
        SteppingModel(name = "previous moves to an older bar", selected = 2, barCount = 5, step = -1, expected = 1),
        SteppingModel(name = "next moves to a newer bar", selected = 2, barCount = 5, step = 1, expected = 3),
        SteppingModel(name = "previous stops at the oldest bar", selected = 0, barCount = 5, step = -1, expected = 0),
        SteppingModel(name = "next stops at the newest bar", selected = 4, barCount = 5, step = 1, expected = 4),
        SteppingModel(name = "a single bar", selected = null, barCount = 1, step = -1, expected = 0),
    )

    data class SteppingModel(
        val name: String,
        val selected: Int?,
        val barCount: Int,
        val step: Int,
        val expected: Int,
    ) {
        override fun toString(): String = name
    }
}