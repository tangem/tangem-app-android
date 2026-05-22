package com.tangem.feature.swap

import arrow.core.Either
import com.tangem.datasource.api.surveysparrow.SurveySparrowApi
import com.tangem.datasource.api.surveysparrow.models.CreateSurveySparrowResponseBody
import com.tangem.datasource.api.surveysparrow.models.SurveySparrowAnswerDto
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.models.domain.ExistingRating
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import org.json.JSONObject

internal class DefaultSwapFeedbackRepository(
    private val api: SurveySparrowApi,
    private val surveyId: Long,
    private val ratingQuestionId: Long,
    private val feedbackQuestionId: Long,
) : SwapFeedbackRepository {

    override suspend fun getRating(txExternalId: String): Either<Throwable, ExistingRating?> {
        return Either.catch {
            val responses = api.getResponses(
                surveyId = surveyId,
                variables = JSONObject().put("tx_external_id", txExternalId).toString(),
                limit = 1,
            )
            val ratingAnswer = responses.data
                .firstOrNull()
                ?.answers
                ?.firstOrNull { answer ->
                    when (val id = answer.questionId) {
                        is Number -> id.toLong() == ratingQuestionId
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

            if (ratingAnswer != null) ExistingRating(ratingAnswer) else null
        }
    }

    override suspend fun submitFeedback(params: SwapFeedbackParams): Either<Throwable, Unit> {
        return Either.catch {
            api.createResponse(
                CreateSurveySparrowResponseBody(
                    surveyId = surveyId,
                    answers = buildList {
                        add(SurveySparrowAnswerDto(ratingQuestionId, params.rating.toString()))
                        if (params.feedback.isNotEmpty()) {
                            add(SurveySparrowAnswerDto(feedbackQuestionId, params.feedback))
                        }
                    },
                    variables = mapOf(
                        "tx_external_id" to params.txExternalId,
                        "provider_name" to params.providerName,
                        "tx_url" to params.txUrl,
                        "user_wallet_id" to params.userWalletIdHash,
                    ),
                ),
            )
        }
    }
}