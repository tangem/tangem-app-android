package com.tangem.domain.cloudbackup.password

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class PasswordStrengthEvaluatorTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Evaluate {

        @ParameterizedTest
        @ProvideTestModels
        fun evaluate(model: EvaluateModel) {
            // Act
            val actual = PasswordStrengthEvaluator.evaluate(model.password)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // empty -> WEAK
            EvaluateModel(password = "", expected = PasswordStrength.WEAK),
            // 4 chars with all classes -> WEAK (length below 6)
            EvaluateModel(password = "Aa1!", expected = PasswordStrength.WEAK),
            // 5 chars with all classes -> WEAK (length below 6)
            EvaluateModel(password = "Aa1!b", expected = PasswordStrength.WEAK),
            // 6 chars with one class -> WEAK
            EvaluateModel(password = "abcdef", expected = PasswordStrength.WEAK),
            // 6 chars with two classes (lower + upper) -> MEDIUM
            EvaluateModel(password = "abcdeF", expected = PasswordStrength.MEDIUM),
            // 7 chars with all classes -> MEDIUM (>= 6, but length below 8 so not STRONG)
            EvaluateModel(password = "Aa1!bc2", expected = PasswordStrength.MEDIUM),
            // >= 8 but only one class (lowercase) -> WEAK
            EvaluateModel(password = "abcdefgh", expected = PasswordStrength.WEAK),
            // >= 8 but only one class (digit) -> WEAK
            EvaluateModel(password = "12345678", expected = PasswordStrength.WEAK),
            // >= 8 with two classes (lower + upper) -> MEDIUM
            EvaluateModel(password = "abcdefgH", expected = PasswordStrength.MEDIUM),
            // length exactly 8 with all four classes -> STRONG
            EvaluateModel(password = "Abcde1!x", expected = PasswordStrength.STRONG),
            // > 8 with all four classes -> STRONG
            EvaluateModel(password = "Str0ng!Passw0rd", expected = PasswordStrength.STRONG),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Hint {

        @ParameterizedTest
        @ProvideTestModels
        fun hint(model: HintModel) {
            // Act
            val actual = PasswordStrengthEvaluator.hint(model.password)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // 0-3 chars -> USE_ALL_CRITERIA
            HintModel(password = "", expected = PasswordStrengthHint.USE_ALL_CRITERIA),
            HintModel(password = "Aa1", expected = PasswordStrengthHint.USE_ALL_CRITERIA),
            // 4-6 chars -> KEEP_GOING
            HintModel(password = "Aa1!", expected = PasswordStrengthHint.KEEP_GOING),
            HintModel(password = "Aa1!bc", expected = PasswordStrengthHint.KEEP_GOING),
            // 7+ chars, missing special -> ADD_SYMBOL
            HintModel(password = "Abcde12", expected = PasswordStrengthHint.ADD_SYMBOL),
            // 7+ chars, has special but missing digit -> ADD_NUMBER
            HintModel(password = "Abcde!x", expected = PasswordStrengthHint.ADD_NUMBER),
            // 7+ chars, missing uppercase -> ADD_UPPERCASE
            HintModel(password = "abcde1!", expected = PasswordStrengthHint.ADD_UPPERCASE),
            // 7+ chars, missing lowercase -> ADD_LOWERCASE
            HintModel(password = "ABCDE1!", expected = PasswordStrengthHint.ADD_LOWERCASE),
            // 7 chars, all classes present but length below 8 -> ALMOST_LONG
            HintModel(password = "Aa1!bc2", expected = PasswordStrengthHint.ALMOST_LONG),
            // >= 8 with all classes -> STRONG
            HintModel(password = "Abcde1!x", expected = PasswordStrengthHint.STRONG),
        )
    }

    data class EvaluateModel(
        val password: String,
        val expected: PasswordStrength,
    )

    data class HintModel(
        val password: String,
        val expected: PasswordStrengthHint,
    )
}