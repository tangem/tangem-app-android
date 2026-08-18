package com.tangem.features.jointaccount.common.displayname

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class DisplayNameValidatorTest {

    private val validator = DisplayNameValidator()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsValid {

        @ParameterizedTest
        @ProvideTestModels
        fun isValid(model: IsValidModel) {
            // Act
            val actual = validator.isValid(model.name)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // letters and spaces
            IsValidModel(name = "Ivan Zolo", expected = true),
            // letters of any script
            IsValidModel(name = "Иван 李小龙", expected = true),
            // digits
            IsValidModel(name = "Agent 007", expected = true),
            // emoji alone is a valid name
            IsValidModel(name = "😀", expected = true),
            // values are not normalised: surrounding spaces are kept and stay valid
            IsValidModel(name = " Ivan ", expected = true),
            // exactly the maximum length
            IsValidModel(name = "a".repeat(n = 25), expected = true),
            // one character over the maximum
            IsValidModel(name = "a".repeat(n = 26), expected = false),
            // the name is required
            IsValidModel(name = "", expected = false),
            // spaces are allowed characters, but a blank-only name is not a name
            IsValidModel(name = "   ", expected = false),
            // a forbidden character invalidates the whole name
            IsValidModel(name = "Ivan!", expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HasForbiddenChars {

        @ParameterizedTest
        @ProvideTestModels
        fun hasForbiddenChars(model: HasForbiddenCharsModel) {
            // Act
            val actual = validator.hasForbiddenChars(model.name)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // letters, digits and spaces are allowed
            HasForbiddenCharsModel(name = "Ivan Zolo 2", expected = false),
            // an empty value has nothing forbidden in it — the "required" rule lives in isValid
            HasForbiddenCharsModel(name = "", expected = false),
            // plain pictograph emoji
            HasForbiddenCharsModel(name = "🚀", expected = false),
            // ZWJ sequence: pictographs joined into a single family glyph
            HasForbiddenCharsModel(name = "👨‍👩‍👧", expected = false),
            // Fitzpatrick skin-tone modifier
            HasForbiddenCharsModel(name = "👍🏽", expected = false),
            // keycap sequence: digit + variation selector + combining keycap
            HasForbiddenCharsModel(name = "1️⃣", expected = false),
            // punctuation
            HasForbiddenCharsModel(name = "Ivan!", expected = true),
            HasForbiddenCharsModel(name = "Ivan-Zolo", expected = true),
            HasForbiddenCharsModel(name = "ivan_zolo", expected = true),
            HasForbiddenCharsModel(name = "ivan.zolo", expected = true),
            // symbols outside the emoji categories
            HasForbiddenCharsModel(name = "name@mail", expected = true),
            HasForbiddenCharsModel(name = "$100", expected = true),
            // whitespace other than a plain space
            HasForbiddenCharsModel(name = "Ivan\nZolo", expected = true),
        )
    }

    internal data class IsValidModel(
        val name: String,
        val expected: Boolean,
    )

    internal data class HasForbiddenCharsModel(
        val name: String,
        val expected: Boolean,
    )
}