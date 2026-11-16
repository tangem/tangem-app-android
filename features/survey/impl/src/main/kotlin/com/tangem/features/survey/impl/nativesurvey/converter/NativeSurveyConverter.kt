package com.tangem.features.survey.impl.nativesurvey.converter

import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyQuestion
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionDto
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionsDto
import java.math.BigDecimal

/** A check the vendor gives no field for passes: its schema leaves most of them out. */
internal object NativeSurveyConverter {

    /** Not `NPSScore`: the vendor's NPS surveys refuse a response without a contact. */
    private const val SCORE_TYPE = "OpinionScale"
    private const val TEXT_TYPE = "TextInput"
    private const val SCALE_START = 0
    private const val SCALE_STEP = 10

    fun convert(surveyId: Long, language: String, dto: SurveySparrowQuestionsDto): NativeSurvey? {
        if (dto.questions.any { it.hasDisplayLogic == true }) return null

        return NativeSurvey(
            surveyId = surveyId,
            type = NativeSurvey.Type.NPS,
            language = language,
            questions = convertNps(dto.questions.inDisplayOrder()) ?: return null,
        )
    }

    private fun convertNps(questions: List<SurveySparrowQuestionDto>): List<SurveyQuestion>? {
        val (score, comment) = questions.takeIf { it.size == 2 } ?: return null
        if (!score.isZeroToTenScale() || !comment.isOptionalText()) return null

        return listOf(
            SurveyQuestion.Nps(id = score.id, text = score.nonBlankText() ?: return null),
            SurveyQuestion.Text(id = comment.id, text = comment.nonBlankText() ?: return null),
        )
    }

    /** Questions in separate sections share one `position`, so the section's position orders them first. */
    private fun List<SurveySparrowQuestionDto>.inDisplayOrder() = sortedWith(
        compareBy<SurveySparrowQuestionDto, BigDecimal?>(nullsLast()) {
            it.section?.position?.toBigDecimalOrNull()
        }.thenBy(nullsLast()) { it.position?.toBigDecimalOrNull() },
    )

    private fun SurveySparrowQuestionDto.isZeroToTenScale(): Boolean {
        val scale = properties?.data
        return type == SCORE_TYPE && scale?.start == SCALE_START && scale.step == SCALE_STEP
    }

    private fun SurveySparrowQuestionDto.isOptionalText(): Boolean = type == TEXT_TYPE && isRequired != true

    private fun SurveySparrowQuestionDto.nonBlankText(): String? = text?.takeIf(String::isNotBlank)
}