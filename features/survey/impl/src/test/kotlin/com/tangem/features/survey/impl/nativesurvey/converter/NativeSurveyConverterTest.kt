package com.tangem.features.survey.impl.nativesurvey.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SURVEY_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.commentQuestion
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.npsSurvey
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.questionsDto
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.scoreQuestion
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionDto
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionsDto
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NativeSurveyConverterTest {

    @Test
    fun `GIVEN a classic form with an opinion scale and a text input WHEN convert THEN returns the NPS survey`() {
        // Act
        val actual = NativeSurveyConverter.convert(surveyId = SURVEY_ID, language = "de", dto = questionsDto())

        // Assert
        assertThat(actual).isEqualTo(npsSurvey(language = "de"))
    }

    @Test
    fun `GIVEN questions listed out of order WHEN convert THEN they are ordered by their sections`() {
        // Arrange
        val dto = questionsDto(questions = listOf(commentQuestion(), scoreQuestion()))

        // Act
        val actual = NativeSurveyConverter.convert(surveyId = SURVEY_ID, language = "en", dto = dto)

        // Assert
        assertThat(actual).isEqualTo(npsSurvey())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a survey the app cannot draw WHEN convert THEN returns null`(model: UnsupportedModel) {
        // Act
        val actual = NativeSurveyConverter.convert(surveyId = SURVEY_ID, language = "en", dto = model.dto)

        // Assert
        assertThat(actual).isNull()
    }

    data class UnsupportedModel(val case: String, val dto: SurveySparrowQuestionsDto) {
        override fun toString() = case
    }

    private fun provideTestModels() = listOf(
        UnsupportedModel("display logic", survey(score = scoreQuestion(hasDisplayLogic = true))),
        UnsupportedModel("one question", questionsDto(questions = listOf(scoreQuestion()))),
        UnsupportedModel(
            "three questions",
            questionsDto(listOf(scoreQuestion(), commentQuestion(), commentQuestion(sectionPosition = "2"))),
        ),
        UnsupportedModel(
            "comment first",
            survey(score = scoreQuestion(sectionPosition = "1"), comment = commentQuestion(sectionPosition = "0")),
        ),
        UnsupportedModel("other score type", survey(score = scoreQuestion(type = "MultiChoice"))),
        UnsupportedModel("NPS survey type", survey(score = scoreQuestion(type = "NPSScore"))),
        UnsupportedModel("scale from one", survey(score = scoreQuestion(start = 1))),
        UnsupportedModel("five steps", survey(score = scoreQuestion(step = 5))),
        UnsupportedModel("no scale fields", survey(score = scoreQuestion(start = null, step = null))),
        UnsupportedModel("required comment", survey(comment = commentQuestion(isRequired = true))),
        UnsupportedModel("other comment type", survey(comment = commentQuestion(type = "Email"))),
        UnsupportedModel("no score text", survey(score = scoreQuestion(text = null))),
        UnsupportedModel("blank comment text", survey(comment = commentQuestion(text = " "))),
    )

    private fun survey(
        score: SurveySparrowQuestionDto = scoreQuestion(),
        comment: SurveySparrowQuestionDto = commentQuestion(),
    ) = questionsDto(questions = listOf(score, comment))
}