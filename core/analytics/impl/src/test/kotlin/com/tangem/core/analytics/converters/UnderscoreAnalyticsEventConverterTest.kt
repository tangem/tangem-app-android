package com.tangem.core.analytics.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class UnderscoreAnalyticsEventConverterTest {

    private val converter = UnderscoreAnalyticsEventConverter()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertEventName {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN event name WHEN convertEventName THEN returns underscored name`(model: ConvertModel) {
            // Act
            val actual = converter.convertEventName(model.input)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ConvertModel(input = "[Basic] Balance Loaded", expected = "Basic_Balance_Loaded"),
            ConvertModel(input = "already_underscored", expected = "already_underscored"),
            ConvertModel(input = "Token - Swap / Send", expected = "Token_Swap_Send"),
            ConvertModel(input = "!!!", expected = ""),
            ConvertModel(input = "a".repeat(41), expected = "a".repeat(40)),
        )
    }

    @Nested
    inner class ConvertEventParams {

        @Test
        fun `GIVEN params WHEN convertEventParams THEN converts keys and values`() {
            // Arrange
            val params = mapOf("Error Code" to "Not enough [balance]")

            // Act
            val actual = converter.convertEventParams(params)

            // Assert
            assertThat(actual).isEqualTo(mapOf("Error_Code" to "Not_enough_balance"))
        }

        @Test
        fun `GIVEN long key and value WHEN convertEventParams THEN key is cut to 40 and value to 100 chars`() {
            // Arrange
            val params = mapOf("k".repeat(41) to "v".repeat(101))

            // Act
            val actual = converter.convertEventParams(params)

            // Assert
            assertThat(actual).isEqualTo(mapOf("k".repeat(40) to "v".repeat(100)))
        }
    }

    data class ConvertModel(val input: String, val expected: String)
}