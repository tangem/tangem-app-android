package com.tangem.features.jointaccount.common.displayname

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DisplayNameValidatorTest {

    private val validator = DisplayNameValidator()

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
        // real names carry hyphens, apostrophes and diacritics — precomposed and decomposed (e + U+0301)
        IsValidModel(name = "Anna-Maria O'Neil", expected = true),
        IsValidModel(name = "O’Connor José", expected = true),
        IsValidModel(name = "José", expected = true),
        // any other special characters are allowed too
        IsValidModel(name = "Ivan!", expected = true),
        IsValidModel(name = "ivan_zolo@mail $100", expected = true),
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
    )

    internal data class IsValidModel(
        val name: String,
        val expected: Boolean,
    )
}