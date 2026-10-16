package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

private const val TOLERANCE = 0.001f

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConfirmOrderTest {

    @ParameterizedTest
    @MethodSource("providePackWidthModels")
    fun `GIVEN the space above the price rows WHEN packWidth THEN the pack keeps its design width unless it does not fit`(
        model: PackWidthModel,
    ) {
        // Act
        val actual = packWidth(maxWidth = model.maxWidth.dp, maxHeight = model.maxHeight.dp)

        // Assert
        assertThat(actual.value).isWithin(TOLERANCE).of(model.expected)
    }

    internal data class PackWidthModel(val maxWidth: Float, val maxHeight: Float, val expected: Float)

    private fun providePackWidthModels() = listOf(
        PackWidthModel(maxWidth = 402f, maxHeight = 500f, expected = 230f),
        // 332 + 16 + 44: the pack just fits with both minimal gaps
        PackWidthModel(maxWidth = 402f, maxHeight = 392f, expected = 230f),
        PackWidthModel(maxWidth = 402f, maxHeight = 196f, expected = 136f * 230f / 332f),
        PackWidthModel(maxWidth = 402f, maxHeight = 20f, expected = 0f),
    )
}