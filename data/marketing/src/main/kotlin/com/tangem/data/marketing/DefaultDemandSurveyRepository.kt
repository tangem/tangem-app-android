package com.tangem.data.marketing

import arrow.core.Either
import arrow.core.getOrElse
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

internal class DefaultDemandSurveyRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : DemandSurveyRepository {

    private val submittedWalletIds = MutableStateFlow(emptySet<UserWalletId>())

    override suspend fun submit(userWalletId: UserWalletId, answers: List<SurveyAnswer>): Either<Throwable, Unit> {
        val body = SurveyCompletionBody(
            surveyId = DEMAND_SURVEY_ID,
            answers = answers.map { SurveyAnswerDto(question = it.questionKey, answer = it.answerKey) },
        )
        return tangemTechApi.submitSurveyCompletion(walletId = userWalletId.stringValue, body = body)
            .toEither()
            .map { submittedWalletIds.update { walletIds -> walletIds + userWalletId } }
            .onLeft { TangemLogger.e("Demand survey submit failed", it) }
    }

    override suspend fun isCompleted(userWalletId: UserWalletId): Either<Throwable, Boolean> {
        return tangemTechApi.getSurveyCompletionStatus(walletId = userWalletId.stringValue, surveyId = DEMAND_SURVEY_ID)
            .toEither()
            .map { it.isCompleted }
            .onLeft { TangemLogger.e("Demand survey status request failed", it) }
    }

    override fun observeCompleted(userWalletId: UserWalletId): Flow<Boolean> {
        val isSubmitted = submittedWalletIds.map { userWalletId in it }
        val requestedStatus = flow<Boolean?> { emit(isCompleted(userWalletId).getOrElse { false }) }
            .onStart { emit(null) }

        return combine(isSubmitted, requestedStatus) { submitted, requested -> if (submitted) true else requested }
            .filterNotNull()
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
    }

    private fun <T : Any> ApiResponse<T>.toEither(): Either<Throwable, T> = when (this) {
        is ApiResponse.Success -> Either.Right(data)
        is ApiResponse.Error -> Either.Left(cause)
    }

    private companion object {
        const val DEMAND_SURVEY_ID = "demand"
    }
}