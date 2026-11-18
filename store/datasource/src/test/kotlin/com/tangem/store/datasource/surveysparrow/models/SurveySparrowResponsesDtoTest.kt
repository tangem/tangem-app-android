package com.tangem.store.datasource.surveysparrow.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

internal class SurveySparrowResponsesDtoTest {

    @Test
    fun `GIVEN answers of mixed types WHEN parsed without reflection THEN numbers and strings are kept`() {
        // Arrange
        val json = """
            {"data": [{"answers": [
                {"question_id": 42, "answer": 5},
                {"question_id": "submittedTime", "answer": "2026-11-09T10:00:00Z"}
            ]}]}
        """.trimIndent()

        // Act
        val actual = Moshi.Builder().build().adapter(SurveySparrowResponsesDto::class.java).fromJson(json)

        // Assert
        val expected = SurveySparrowResponsesDto(
            data = listOf(
                SurveySparrowResponseDto(
                    answers = listOf(
                        SurveySparrowGetAnswerDto(questionId = 42.0, answer = 5.0),
                        SurveySparrowGetAnswerDto(questionId = "submittedTime", answer = "2026-11-09T10:00:00Z"),
                    ),
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }
}