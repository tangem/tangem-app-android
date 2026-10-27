package com.tangem.data.marketing

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.survey.models.SurveyAnswerDto
import com.tangem.datasource.api.survey.models.SurveyCompletionBody
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.marketing.DemandSurveyRepository
import com.tangem.domain.marketing.models.SurveyAnswer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.logging.TangemLogger

internal class DefaultDemandSurveyRepository(
    private val tangemTechApi: TangemTechApi,
) : DemandSurveyRepository {

    override suspend fun submit(userWalletId: UserWalletId, answers: List<SurveyAnswer>): Either<Throwable, Unit> {
        val body = SurveyCompletionBody(
            surveyId = DEMAND_SURVEY_ID,
            answers = answers.map { SurveyAnswerDto(question = it.questionKey, answer = it.answerKey) },
        )
        return tangemTechApi.submitSurveyCompletion(walletId = userWalletId.stringValue, body = body)
            .toEither()
            .map { }
            .onLeft { TangemLogger.e("Demand survey submit failed", it) }
    }

    override suspend fun isCompleted(userWalletId: UserWalletId): Either<Throwable, Boolean> {
        return tangemTechApi.getSurveyCompletionStatus(walletId = userWalletId.stringValue, surveyId = DEMAND_SURVEY_ID)
            .toEither()
            .map { it.isCompleted }
            .onLeft { TangemLogger.e("Demand survey status request failed", it) }
    }

    private fun <T : Any> ApiResponse<T>.toEither(): Either<Throwable, T> = when (this) {
        is ApiResponse.Success -> Either.Right(data)
        is ApiResponse.Error -> Either.Left(cause)
    }

    private companion object {
        const val DEMAND_SURVEY_ID = "demand"
    }
}