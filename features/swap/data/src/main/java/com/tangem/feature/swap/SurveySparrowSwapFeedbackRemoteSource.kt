package com.tangem.feature.swap

import arrow.core.Either
import com.tangem.datasource.api.surveysparrow.SurveySparrowApi
import com.tangem.datasource.api.surveysparrow.models.CreateSurveySparrowResponseBody
import com.tangem.datasource.api.surveysparrow.models.SurveySparrowAnswerDto
import com.tangem.datasource.local.config.environment.models.SurveySparrowSwapRatingConfig
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import org.json.JSONObject

internal class SurveySparrowSwapFeedbackRemoteSource(
    private val api: SurveySparrowApi,
    private val config: SurveySparrowSwapRatingConfig,
) : SwapFeedbackRemoteSource {

    override suspend fun getRating(txExternalId: String): Either<Throwable, Int?> {
        return Either.catch {
            val responses = api.getResponses(
                surveyId = config.surveyId,
                variables = JSONObject().put("tx_external_id", txExternalId).toString(),
                limit = 1,
            )
            responses.data
                .firstOrNull()
                ?.answers
                ?.firstOrNull { answer ->
                    when (val id = answer.questionId) {
                        is Number -> id.toLong() == config.ratingQuestionId
                        else -> false
                    }
                }
                ?.answer
                ?.let { v ->
                    when (v) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull()
                        else -> null
                    }
                }
        }
    }

    override suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit> {
        return Either.catch {
            api.createResponse(
                CreateSurveySparrowResponseBody(
                    surveyId = config.surveyId,
                    answers = buildList {
                        add(SurveySparrowAnswerDto(config.ratingQuestionId, params.rating.toString()))
                        if (params.feedback.isNotEmpty()) {
                            add(SurveySparrowAnswerDto(config.feedbackQuestionId, params.feedback))
                        }
                    },
                    variables = buildMap {
                        put("tx_external_id", params.txExternalId)
                        put("provider_name", params.providerName)
                        if (params.txUrl.isNotEmpty()) {
                            put("tx_url", params.txUrl)
                        }
                        put("user_wallet_id", params.userWalletIdHash)
                    },
                ),
            )
        }
    }
}