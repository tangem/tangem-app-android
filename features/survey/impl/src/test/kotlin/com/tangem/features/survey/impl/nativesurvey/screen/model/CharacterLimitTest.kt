package com.tangem.features.survey.impl.nativesurvey.screen.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CharacterLimitTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a text WHEN cut to a length THEN no character is split`(model: LimitModel) {
        // Act
        val actual = model.text.takeWholeCharacters(model.maxLength)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        LimitModel(case = "shorter", text = "abc", maxLength = 5, expected = "abc"),
        LimitModel(case = "exact", text = "abcde", maxLength = 5, expected = "abcde"),
        LimitModel(case = "longer", text = "abcdef", maxLength = 3, expected = "abc"),
        LimitModel(case = "nothing allowed", text = "abc", maxLength = 0, expected = ""),
        LimitModel(case = "emoji on the edge", text = "ab👍", maxLength = 3, expected = "ab"),
        LimitModel(case = "emoji fits exactly", text = "a👍b", maxLength = 3, expected = "a👍"),
        LimitModel(case = "accent on the edge", text = "e\u0301e\u0301", maxLength = 3, expected = "e\u0301"),
    )

    data class LimitModel(val case: String, val text: String, val maxLength: Int, val expected: String) {
        override fun toString() = case
    }
}