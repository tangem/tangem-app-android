package com.tangem.features.survey.impl.nativesurvey.model

import com.tangem.domain.models.wallet.UserWallet

/** @property language the language the texts came in: the base one when the asked language has no translation */
internal data class NativeSurvey(
    val surveyId: Long,
    val type: Type,
    val language: String,
    val questions: List<SurveyQuestion>,
) {

    enum class Type { NPS }
}

internal sealed interface SurveyQuestion {
    val id: Long
    val text: String

    data class Nps(override val id: Long, override val text: String) : SurveyQuestion

    data class Text(override val id: Long, override val text: String) : SurveyQuestion
}

internal sealed interface SurveyAnswer {
    val questionId: Long

    data class Score(override val questionId: Long, val value: Int) : SurveyAnswer

    data class Text(override val questionId: Long, val value: String) : SurveyAnswer
}

/**
 * @property stage the lifecycle stage label of the entry point, empty when it has none
 * @property repeatAfterDays how long an answer closes the survey on this device; null closes it for good
 * @property userWallet the wallet active when the survey was opened, the one the answer is attributed to
 */
internal data class SurveyEntry(
    val surveyId: Long,
    val source: SurveySource,
    val stage: String,
    val repeatAfterDays: Int?,
    val userWallet: UserWallet,
)

internal enum class SurveySource { SHEET, PUSH, BANNER, LINK }