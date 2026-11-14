package com.tangem.features.survey.impl.nativesurvey.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.domain.models.wallet.hashedStringValue
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent.Request
import com.tangem.features.survey.impl.nativesurvey.converter.NativeSurveyConverter
import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyAnswer
import com.tangem.features.survey.impl.nativesurvey.model.SurveyEntry
import com.tangem.features.survey.impl.nativesurvey.store.NativeSurveyDefinitionStore
import com.tangem.features.survey.impl.nativesurvey.store.NativeSurveyStateStore
import com.tangem.features.survey.impl.nativesurvey.store.SavedSurveyDefinition
import com.tangem.store.datasource.config.StoreEnvironmentConfig
import com.tangem.store.datasource.surveysparrow.SurveySparrowApi
import com.tangem.store.datasource.surveysparrow.models.CreateSurveySparrowResponseBody
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowAnswerDto
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionsDto
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.time.CurrentTimeProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The vendor caps calls per account, shared with the swap rating, so every survey is read once per device and
 * language, and a survey the vendor refused is not asked again until the next launch.
 */
@Suppress("LongParameterList")
@Singleton
internal class DefaultNativeSurveyRepository @Inject constructor(
    private val api: SurveySparrowApi,
    private val storeEnvironmentConfig: StoreEnvironmentConfig,
    private val definitionStore: NativeSurveyDefinitionStore,
    private val stateStore: NativeSurveyStateStore,
    private val languageProvider: SurveyLanguageProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appInfoProvider: AppInfoProvider,
    private val currentTimeProvider: CurrentTimeProvider,
    private val appScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) : NativeSurveyRepository {

    private val refusals = ConcurrentHashMap<Long, SurveyRequestFailure>()

    /** One load at a time, so a second caller finds the questions the first one saved instead of asking again. */
    private val loadMutex = Mutex()

    override val isAvailable: Boolean
        get() = !storeEnvironmentConfig.surveySparrowToken.isNullOrBlank()

    override suspend fun load(entry: SurveyEntry): LoadedNativeSurvey? {
        val askedLanguage = languageProvider.current()
        // The application scope keeps questions loaded for a closed screen, so they are saved for the next time
        return appScope.async(dispatchers.io) {
            loadMutex.withLock { loadOnce(entry = entry, askedLanguage = askedLanguage) }
        }.await()
    }

    private suspend fun loadOnce(entry: SurveyEntry, askedLanguage: String): LoadedNativeSurvey? {
        val refusal = refusals[entry.surveyId]
        if (refusal == null) {
            savedSurvey(entry.surveyId, askedLanguage)?.let { return LoadedNativeSurvey(it, isFromCache = true) }
        }

        return (refusal?.left() ?: fetch(surveyId = entry.surveyId, askedLanguage = askedLanguage))
            .onLeft { failure ->
                sendError(
                    entry = entry,
                    surveyType = null,
                    request = Request.LOAD,
                    failure = failure,
                    isRequestSent = refusal == null,
                )
            }
            .getOrNull()
            ?.let { LoadedNativeSurvey(it, isFromCache = false) }
    }

    override suspend fun submit(
        entry: SurveyEntry,
        survey: NativeSurvey,
        responseUuid: String,
        answers: List<SurveyAnswer>,
    ): Either<SurveySubmitError, Unit> {
        return appScope.async(dispatchers.io) {
            send(entry = entry, survey = survey, responseUuid = responseUuid, answers = answers)
        }.await()
    }

    override suspend fun isAnswered(entry: SurveyEntry): Boolean {
        return stateStore.deviceRecord(entry.surveyId)?.isAnswerActive(currentTimeProvider.nowMillis()) == true
    }

    private suspend fun savedSurvey(surveyId: Long, askedLanguage: String): NativeSurvey? {
        val saved = definitionStore.get(surveyId, askedLanguage) ?: return null
        return NativeSurveyConverter.convert(surveyId = surveyId, language = saved.language, dto = saved.questions)
    }

    private suspend fun fetch(surveyId: Long, askedLanguage: String): Either<SurveyRequestFailure, NativeSurvey> =
        either {
            val (questions, language) = requestQuestions(surveyId = surveyId, askedLanguage = askedLanguage).bind()
            val survey = NativeSurveyConverter.convert(surveyId = surveyId, language = language, dto = questions)
                ?: raise(SurveyRequestFailure.Unsupported)

            definitionStore.save(
                surveyId = surveyId,
                askedLanguage = askedLanguage,
                definition = SavedSurveyDefinition(language = language, questions = questions),
            )
            survey
        }.onLeft { failure ->
            TangemLogger.e("Native survey: failed to load $surveyId: $failure")
            if (failure != SurveyRequestFailure.Network) refusals[surveyId] = failure
        }

    private suspend fun requestQuestions(
        surveyId: Long,
        askedLanguage: String,
    ): Either<SurveyRequestFailure, Pair<SurveySparrowQuestionsDto, String>> {
        val languageLabel = askedLanguage.takeIf { it != SurveyLanguageProvider.BASE_LANGUAGE }
        val asked = getQuestions(surveyId, languageLabel).map { it to askedLanguage }

        // The vendor answers 404 instead of the base language when the survey has no translation for the asked one
        val isTranslationMissing = languageLabel != null &&
            asked.leftOrNull() == SurveyRequestFailure.Http(HTTP_NOT_FOUND)

        return if (isTranslationMissing) {
            getQuestions(surveyId, languageLabel = null).map { it to SurveyLanguageProvider.BASE_LANGUAGE }
        } else {
            asked
        }
    }

    private suspend fun getQuestions(
        surveyId: Long,
        languageLabel: String?,
    ): Either<SurveyRequestFailure, SurveySparrowQuestionsDto> {
        return Either.catch {
            api.getQuestions(surveyId = surveyId, limit = QUESTIONS_LIMIT, languageLabel = languageLabel)
        }.mapLeft { it.toLoggedFailure() }
    }

    private suspend fun send(
        entry: SurveyEntry,
        survey: NativeSurvey,
        responseUuid: String,
        answers: List<SurveyAnswer>,
    ): Either<SurveySubmitError, Unit> {
        val sentAnswers = answers.mapNotNull { it.trimmedOrNull() }
        val refusal = refusals[entry.surveyId]
        val result = refusal?.left() ?: Either.catch {
            api.createResponse(
                CreateSurveySparrowResponseBody(
                    surveyId = survey.surveyId,
                    answers = sentAnswers.map { it.toDto() },
                    variables = variables(entry = entry, survey = survey, responseUuid = responseUuid),
                ),
            )
        }.mapLeft { it.toLoggedFailure() }

        return result
            .onRight {
                stateStore.markAnswered(
                    surveyId = entry.surveyId,
                    answeredAtMillis = currentTimeProvider.nowMillis(),
                    repeatAfterDays = entry.repeatAfterDays,
                )
                analyticsEventHandler.send(
                    NativeSurveyAnalyticsEvent.SurveySubmitted(entry, survey.type, sentAnswers = sentAnswers),
                )
            }
            .mapLeft { failure ->
                sendError(
                    entry = entry,
                    surveyType = survey.type,
                    request = Request.SUBMIT,
                    failure = failure,
                    isRequestSent = refusal == null,
                )
                when {
                    refusal == null -> onSubmitFailed(surveyId = entry.surveyId, failure = failure)
                    refusal == SurveyRequestFailure.Http(HTTP_TOO_MANY_REQUESTS) -> SurveySubmitError.RETRY
                    else -> SurveySubmitError.CLOSE
                }
            }
    }

    private suspend fun onSubmitFailed(surveyId: Long, failure: SurveyRequestFailure): SurveySubmitError {
        TangemLogger.e("Native survey: failed to submit $surveyId: $failure")
        val code = (failure as? SurveyRequestFailure.Http)?.code
        return when {
            code == null || code >= HTTP_INTERNAL_ERROR -> SurveySubmitError.RETRY
            code == HTTP_TOO_MANY_REQUESTS -> {
                refusals[surveyId] = failure
                SurveySubmitError.RETRY
            }
            code in SETUP_ERROR_CODES -> {
                refusals[surveyId] = failure
                SurveySubmitError.CLOSE
            }
            else -> {
                // A rejected answer means the saved questions no longer match the survey
                definitionStore.remove(surveyId)
                refusals[surveyId] = failure
                SurveySubmitError.CLOSE
            }
        }
    }

    private fun variables(entry: SurveyEntry, survey: NativeSurvey, responseUuid: String) = mapOf(
        "user_wallet_id" to entry.userWallet.walletId.hashedStringValue,
        "response_uuid" to responseUuid,
        "app_version" to appInfoProvider.appVersion,
        "platform" to appInfoProvider.platform.lowercase(),
        "source" to entry.source.name.lowercase(),
        "stage" to entry.stage,
        "wallet_type" to WalletType.from(entry.userWallet)?.name?.lowercase().orEmpty(),
        "language" to survey.language,
    )

    private fun Throwable.toLoggedFailure(): SurveyRequestFailure = toSurveyRequestFailure().also { failure ->
        // Parse also stands for an unexpected error, so its cause is kept in the log
        if (failure == SurveyRequestFailure.Parse) TangemLogger.e("Native survey: request failed", this)
    }

    private fun SurveyAnswer.trimmedOrNull(): SurveyAnswer? = when (this) {
        is SurveyAnswer.Score -> this
        is SurveyAnswer.Text -> value.trim().takeIf(String::isNotEmpty)?.let { copy(value = it) }
    }

    private fun SurveyAnswer.toDto() = when (this) {
        is SurveyAnswer.Score -> SurveySparrowAnswerDto(questionId = questionId, answer = value.toString())
        is SurveyAnswer.Text -> SurveySparrowAnswerDto(questionId = questionId, answer = value)
    }

    private fun sendError(
        entry: SurveyEntry,
        surveyType: NativeSurvey.Type?,
        request: Request,
        failure: SurveyRequestFailure,
        isRequestSent: Boolean,
    ) {
        analyticsEventHandler.send(
            NativeSurveyAnalyticsEvent.SurveyError(
                entry = entry,
                surveyType = surveyType,
                request = request,
                failure = failure,
                isRequestSent = isRequestSent,
            ),
        )
    }

    private companion object {
        const val QUESTIONS_LIMIT = 100
        const val HTTP_TOO_MANY_REQUESTS = 429
        val SETUP_ERROR_CODES = setOf(HTTP_UNAUTHORIZED, HTTP_FORBIDDEN, HTTP_NOT_FOUND)
    }
}