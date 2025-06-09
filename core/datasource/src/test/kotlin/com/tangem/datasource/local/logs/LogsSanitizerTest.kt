package com.tangem.datasource.local.logs

import com.google.common.truth.Truth
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LogsSanitizerTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun sanitize(model: TestModel) {
        // Act
        val actual = LogsSanitizer.sanitize(model.input)

        // Assert
        val expected = model.expected
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        TestModel(input = "dead-beef-cafe-babe", expected = HIDDEN_TEXT),
        TestModel(input = "[dead-beef-cafe-babe]", expected = "[$HIDDEN_TEXT]"),
        TestModel(input = "0xdead-beef-cafe-babe", expected = HIDDEN_TEXT),
        TestModel(input = "0XaA-Bb-Cc-Dd-Ee-Ff", expected = HIDDEN_TEXT),
        TestModel(input = "A1B2C3D4E5F6", expected = HIDDEN_TEXT),
        TestModel(input = "deadbeefcafebabe", expected = HIDDEN_TEXT),
        TestModel(input = "deadbeefcafebabe", expected = HIDDEN_TEXT),
        TestModel(
            input = "Values: 12345678 and ABCDEF12 and cafe9876",
            expected = "Values: $HIDDEN_TEXT and $HIDDEN_TEXT and $HIDDEN_TEXT",
        ),
        TestModel(
            input = "Chunks: DEADBEEF CAFEBABE F00DBABE",
            expected = "Chunks: $HIDDEN_TEXT $HIDDEN_TEXT $HIDDEN_TEXT",
        ),
        TestModel(
            input = "Normal log message without secrets",
            expected = "Normal log message without secrets",
        ),
    )

    data class TestModel(val input: String, val expected: String)

    private companion object {

        const val HIDDEN_TEXT = "******"
    }
}