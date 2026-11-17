package com.tangem.features.survey.impl.nativesurvey.repository

import com.squareup.moshi.JsonEncodingException
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException

internal sealed interface SurveyRequestFailure {

    data object Network : SurveyRequestFailure

    data class Http(val code: Int) : SurveyRequestFailure

    data object Parse : SurveyRequestFailure

    data object Unsupported : SurveyRequestFailure
}

/**
 * Malformed JSON and an empty body are [IOException]s too, so they are matched first: a broken body is
 * [SurveyRequestFailure.Parse], not [SurveyRequestFailure.Network].
 */
internal fun Throwable.toSurveyRequestFailure(): SurveyRequestFailure = when (this) {
    is HttpException -> SurveyRequestFailure.Http(code())
    is JsonEncodingException, is EOFException -> SurveyRequestFailure.Parse
    is IOException -> SurveyRequestFailure.Network
    else -> SurveyRequestFailure.Parse
}