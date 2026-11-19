package com.tangem.features.survey.impl.nativesurvey

import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyQuestion
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionDto
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionDto.Data
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionDto.Properties
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionDto.Section
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionsDto

internal object NativeSurveyTestData {

    const val SURVEY_ID = 283996L
    const val SCORE_QUESTION_ID = 1559648L
    const val COMMENT_QUESTION_ID = 1559649L
    const val SCORE_TEXT = "How likely are you to recommend Tangem?"
    const val COMMENT_TEXT = "What should we improve?"

    fun scoreQuestion(
        type: String = "OpinionScale",
        text: String? = SCORE_TEXT,
        start: Int? = 0,
        step: Int? = 10,
        hasDisplayLogic: Boolean? = false,
        sectionPosition: String? = "0.000000000000000",
    ) = SurveySparrowQuestionDto(
        id = SCORE_QUESTION_ID,
        type = type,
        text = text,
        isRequired = true,
        hasDisplayLogic = hasDisplayLogic,
        position = "0.000000000000",
        section = Section(position = sectionPosition),
        properties = Properties(data = Data(start = start, step = step)),
    )

    fun commentQuestion(
        type: String = "TextInput",
        text: String? = COMMENT_TEXT,
        isRequired: Boolean? = false,
        sectionPosition: String? = "1.000000000000000",
    ) = SurveySparrowQuestionDto(
        id = COMMENT_QUESTION_ID,
        type = type,
        text = text,
        isRequired = isRequired,
        hasDisplayLogic = false,
        position = "0.000000000000",
        section = Section(position = sectionPosition),
        properties = Properties(data = Data()),
    )

    fun questionsDto(
        questions: List<SurveySparrowQuestionDto> = listOf(scoreQuestion(), commentQuestion()),
    ) = SurveySparrowQuestionsDto(questions = questions)

    fun npsSurvey(language: String = "en") =
        NativeSurvey(
            surveyId = SURVEY_ID,
            type = NativeSurvey.Type.NPS,
            language = language,
            questions = listOf(
                SurveyQuestion.Nps(id = SCORE_QUESTION_ID, text = SCORE_TEXT),
                SurveyQuestion.Text(id = COMMENT_QUESTION_ID, text = COMMENT_TEXT),
            ),
        )
}