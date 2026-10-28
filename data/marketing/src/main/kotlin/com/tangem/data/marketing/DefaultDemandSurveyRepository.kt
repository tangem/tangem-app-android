package com.tangem.data.marketing

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.survey.models.SurveyAnswerDto
import com.tangem.datasource.api.survey.models.SurveyCompletionBody
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.marketing.DemandSurveyRepository
import com.tangem.domain.marketing.models.SurveyAnswer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class DefaultDemandSurveyRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : DemandSurveyRepository {

    /** Only completion is cached: it is final, while `false` or a failure must be re-requested next session */
    private val completedWalletIds = MutableStateFlow(emptySet<UserWalletId>())

    override suspend fun submit(userWalletId: UserWalletId, answers: List<SurveyAnswer>): Either<Throwable, Unit> {
        val body = SurveyCompletionBody(
            surveyId = DEMAND_SURVEY_ID,
            answers = answers.map { SurveyAnswerDto(question = it.questionKey, answer = it.answerKey) },
        )
        return tangemTechApi.submitSurveyCompletion(walletId = userWalletId.stringValue, body = body)
            .toEither()
            .map { markCompleted(userWalletId) }
            .onLeft { TangemLogger.e("Demand survey submit failed", it) }
    }

    override suspend fun isCompleted(userWalletId: UserWalletId): Either<Throwable, Boolean> {
        return tangemTechApi.getSurveyCompletionStatus(walletId = userWalletId.stringValue, surveyId = DEMAND_SURVEY_ID)
            .toEither()
            .map { it.isCompleted }
            .onRight { isCompleted -> if (isCompleted) markCompleted(userWalletId) }
            .onLeft { TangemLogger.e("Demand survey status request failed", it) }
    }

    override fun observeCompleted(userWalletId: UserWalletId): Flow<Boolean> = flow {
        if (userWalletId !in completedWalletIds.value) isCompleted(userWalletId)
        emitAll(completedWalletIds.map { userWalletId in it })
    }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)

    private fun markCompleted(userWalletId: UserWalletId) {
        completedWalletIds.update { it + userWalletId }
    }

    private fun <T : Any> ApiResponse<T>.toEither(): Either<Throwable, T> = when (this) {
        is ApiResponse.Success -> Either.Right(data)
        is ApiResponse.Error -> Either.Left(cause)
    }

    private companion object {
        const val DEMAND_SURVEY_ID = "demand"
    }
}