package com.tangem.features.storiesv2.impl.content.remote

import com.google.common.truth.Truth.assertThat
import com.tangem.features.storiesv2.impl.content.StoryV2Haptic
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StoryV2HapticClusterizerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN haptic marks WHEN cluster THEN marks closer than the gap play as one burst`(model: ClusterModel) {
        // Act
        val actual = StoryV2HapticClusterizer.cluster(model.marksMs)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ClusterModel(marksMs = emptyList(), expected = emptyList()),
        ClusterModel(marksMs = listOf(4_700L), expected = listOf(tick(atMs = 4_700L))),
        ClusterModel(
            marksMs = listOf(1_450L, 1_560L),
            expected = listOf(tick(atMs = 1_450L, pulses = 2, pulseIntervalMs = 110L)),
        ),
        ClusterModel(marksMs = listOf(600L, 1_050L), expected = listOf(tick(atMs = 600L), tick(atMs = 1_050L))),
        ClusterModel(marksMs = listOf(0L, 150L), expected = listOf(tick(atMs = 0L), tick(atMs = 150L))),
        ClusterModel(
            marksMs = listOf(0L, 149L),
            expected = listOf(tick(atMs = 0L, pulses = 2, pulseIntervalMs = 149L)),
        ),
        ClusterModel(
            marksMs = listOf(0L, 140L, 280L, 420L),
            expected = listOf(tick(atMs = 0L, pulses = 4, pulseIntervalMs = 140L)),
        ),
        ClusterModel(
            marksMs = List(size = 12) { index -> 350L + index * 30L },
            expected = listOf(tick(atMs = 350L, pulses = 12, pulseIntervalMs = 30L)),
        ),
        ClusterModel(marksMs = listOf(1_050L, 600L, 1_050L, -20L), expected = listOf(tick(600L), tick(1_050L))),
        ClusterModel(
            marksMs = listOf(250L, 280L, 310L, 340L, 370L, 400L, 430L, 460L, 1_500L),
            expected = listOf(tick(atMs = 250L, pulses = 8, pulseIntervalMs = 30L), tick(atMs = 1_500L)),
        ),
        ClusterModel(
            marksMs = listOf(0L, 50L, 101L),
            expected = listOf(tick(atMs = 0L, pulses = 3, pulseIntervalMs = 51L)),
        ),
    )

    internal data class ClusterModel(val marksMs: List<Long>, val expected: List<StoryV2Haptic>)
}