package com.tangem.feature.swap.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class ExpressSlippageConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FromApi {

        @ParameterizedTest
        @ProvideTestModels
        fun fromApi(model: FromApiModel) {
            // Act
            val actual = ExpressSlippageConverter.fromApi(model.value)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FromApiModel(value = BigDecimal("50"), expected = BigDecimal("0.50")),
            FromApiModel(value = BigDecimal("150"), expected = BigDecimal("1.50")),
            FromApiModel(value = null, expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToApi {

        @ParameterizedTest
        @ProvideTestModels
        fun toApi(model: ToApiModel) {
            // Act
            val actual = ExpressSlippageConverter.toApi(model.percent)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ToApiModel(percent = BigDecimal("0.5"), expected = "50"),
            ToApiModel(percent = BigDecimal("0.15"), expected = "15"),
            ToApiModel(percent = BigDecimal("1"), expected = "100"),
            ToApiModel(percent = null, expected = null),
        )
    }

    data class FromApiModel(val value: BigDecimal?, val expected: BigDecimal?)

    data class ToApiModel(val percent: BigDecimal?, val expected: String?)
}