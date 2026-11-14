package com.tangem.features.survey.impl.nativesurvey.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyAnswer
import com.tangem.features.survey.impl.nativesurvey.model.SurveyEntry
import com.tangem.features.survey.impl.nativesurvey.model.SurveySource
import com.tangem.features.survey.impl.nativesurvey.repository.SurveyRequestFailure

internal sealed class NativeSurveyAnalyticsEvent(
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(category = "Survey", event = event, params = params) {

    class SurveySubmitted(
        entry: SurveyEntry,
        surveyType: NativeSurvey.Type,
        sentAnswers: List<SurveyAnswer>,
    ) : NativeSurveyAnalyticsEvent(
        event = "Survey Submitted",
        params = baseParams(entry, surveyType) + sentAnswers.toParams(),
    )

    class SurveyError(
        entry: SurveyEntry,
        surveyType: NativeSurvey.Type?,
        request: Request,
        failure: SurveyRequestFailure,
        isRequestSent: Boolean,
    ) : NativeSurveyAnalyticsEvent(
        event = "Survey Error",
        params = baseParams(entry, surveyType) + mapOf(
            "Request" to request.value,
            AnalyticsParam.ERROR_CODE to failure.toErrorCode(),
            "Request Sent" to isRequestSent.toYesNo(),
        ),
    )

    class SurveyOpened(
        entry: SurveyEntry,
        surveyType: NativeSurvey.Type?,
        isFromCache: Boolean,
        isAlreadyPassed: Boolean,
    ) : NativeSurveyAnalyticsEvent(
        event = "Survey Opened",
        params = baseParams(entry, surveyType) + mapOf(
            "From Cache" to isFromCache.toYesNo(),
            "Already Passed" to isAlreadyPassed.toYesNo(),
        ),
    )

    class SurveyClosed(
        entry: SurveyEntry,
        surveyType: NativeSurvey.Type?,
        closeMethod: CloseMethod,
        isScoreSelected: Boolean,
    ) : NativeSurveyAnalyticsEvent(
        event = "Survey Closed",
        params = baseParams(entry, surveyType) + mapOf(
            "Close Method" to closeMethod.value,
            "Score Selected" to isScoreSelected.toYesNo(),
        ),
    )

    enum class CloseMethod(val value: String) {
        CROSS("X"),
        SYSTEM_BACK("System Back"),
        LOCK("Lock"),
        DEEPLINK("Deeplink"),
        ERROR("Error"),
    }

    enum class Request(val value: String) {
        LOAD("Load"),
        SUBMIT("Submit"),
    }
}

private fun baseParams(entry: SurveyEntry, surveyType: NativeSurvey.Type?) = mapOf(
    "Survey Id" to entry.surveyId.toString(),
    "Survey Type" to surveyType?.toValue().orEmpty(),
    AnalyticsParam.SOURCE to entry.source.toValue(),
    "Lifecycle Stage" to entry.stage,
)

private fun List<SurveyAnswer>.toParams(): Map<String, String> {
    val score = filterIsInstance<SurveyAnswer.Score>().firstOrNull()
    val isCommentFilled = any { it is SurveyAnswer.Text }
    return buildMap {
        score?.let { put("Score", it.value.toString()) }
        put("Comment Filled", isCommentFilled.toYesNo())
    }
}

private fun NativeSurvey.Type.toValue() = when (this) {
    NativeSurvey.Type.NPS -> "NPS"
}

private fun SurveySource.toValue() = when (this) {
    SurveySource.SHEET -> "Sheet"
    SurveySource.PUSH -> "Push"
    SurveySource.BANNER -> "Banner"
    SurveySource.LINK -> "Link"
}

private fun SurveyRequestFailure.toErrorCode() = when (this) {
    SurveyRequestFailure.Network -> "Network"
    is SurveyRequestFailure.Http -> code.toString()
    SurveyRequestFailure.Parse -> "Parse"
    SurveyRequestFailure.Unsupported -> "Unsupported"
}

private fun Boolean.toYesNo() = if (this) "Yes" else "No"