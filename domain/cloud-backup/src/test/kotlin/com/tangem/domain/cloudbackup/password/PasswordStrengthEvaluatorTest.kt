package com.tangem.domain.cloudbackup.password

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasswordStrengthEvaluatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun evaluate(model: EvaluateModel) {
        // Act
        val actual = PasswordStrengthEvaluator.evaluate(model.password)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        // empty -> WEAK (length not met)
        EvaluateModel(password = "", expected = PasswordStrength.WEAK),
        // < 8 with all classes -> WEAK (length not met)
        EvaluateModel(password = "Aa1!", expected = PasswordStrength.WEAK),
        // exactly 7 chars with all classes -> WEAK (length boundary below 8)
        EvaluateModel(password = "Aa1!bc2", expected = PasswordStrength.WEAK),
        // >= 8 but only one class (lowercase) -> WEAK
        EvaluateModel(password = "abcdefgh", expected = PasswordStrength.WEAK),
        // >= 8 length with only length and one class satisfied -> WEAK
        EvaluateModel(password = "12345678", expected = PasswordStrength.WEAK),
        // >= 8 with exactly two classes (lower + upper) -> MEDIUM
        EvaluateModel(password = "abcdefgH", expected = PasswordStrength.MEDIUM),
        // >= 8 with exactly two classes (lower + digit) -> MEDIUM
        EvaluateModel(password = "abcdefg1", expected = PasswordStrength.MEDIUM),
        // >= 8 with three classes (lower + upper + digit) -> MEDIUM
        EvaluateModel(password = "Abcdefg1", expected = PasswordStrength.MEDIUM),
        // length boundary exactly 8 with all four classes -> STRONG
        EvaluateModel(password = "Abcde1!x", expected = PasswordStrength.STRONG),
        // > 8 with all four classes -> STRONG
        EvaluateModel(password = "Str0ng!Passw0rd", expected = PasswordStrength.STRONG),
    )

    data class EvaluateModel(
        val password: String,
        val expected: PasswordStrength,
    )
}