package com.tangem.features.survey.impl.nativesurvey.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.hashedStringValue
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.COMMENT_QUESTION_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SCORE_QUESTION_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SURVEY_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.npsSurvey
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.questionsDto
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.scoreQuestion
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent.Request
import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyAnswer
import com.tangem.features.survey.impl.nativesurvey.model.SurveyEntry
import com.tangem.features.survey.impl.nativesurvey.model.SurveySource
import com.tangem.features.survey.impl.nativesurvey.store.NativeSurveyDefinitionStore
import com.tangem.features.survey.impl.nativesurvey.store.NativeSurveyStateStore
import com.tangem.features.survey.impl.nativesurvey.store.SavedSurveyDefinition
import com.tangem.features.survey.impl.nativesurvey.store.SurveyDeviceRecord
import com.tangem.store.datasource.config.StoreEnvironmentConfig
import com.tangem.store.datasource.surveysparrow.SurveySparrowApi
import com.tangem.store.datasource.surveysparrow.models.CreateSurveySparrowResponseBody
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowAnswerDto
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionsDto
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import retrofit2.HttpException
import retrofit2.Response
import java.io.EOFException
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.days

internal class DefaultNativeSurveyRepositoryTest {

    private val api: SurveySparrowApi = mockk()
    private val definitionStore: NativeSurveyDefinitionStore = mockk()
    private val stateStore: NativeSurveyStateStore = mockk()
    private val languageProvider: SurveyLanguageProvider = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val appInfoProvider: AppInfoProvider = mockk()
    private val userWallet: UserWallet.Cold = mockk()

    /** Recreated per test: a refused survey stays blocked in the instance for a launch. */
    private lateinit var repository: DefaultNativeSurveyRepository

    @BeforeEach
    fun setUp() {
        clearMocks(
            api,
            definitionStore,
            stateStore,
            languageProvider,
            analyticsEventHandler,
            appInfoProvider,
            userWallet,
        )
        every { languageProvider.current() } returns "de"
        coEvery { definitionStore.get(any(), any()) } returns null
        coJustRun { definitionStore.save(any(), any(), any()) }
        coJustRun { definitionStore.remove(any()) }
        coJustRun { stateStore.markAnswered(any(), any(), any()) }
        justRun { analyticsEventHandler.send(any()) }
        every { appInfoProvider.appVersion } returns APP_VERSION
        every { appInfoProvider.platform } returns "Android"
        every { userWallet.walletId } returns UserWalletId(WALLET_ID)

        repository = createRepository(token = "token")
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsAvailable {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a SurveySparrow token WHEN isAvailable read THEN it is on only with a token`(model: TokenModel) {
            // Arrange
            val repository = createRepository(token = model.token)

            // Act
            val actual = repository.isAvailable

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            TokenModel(token = "token", expected = true),
            TokenModel(token = null, expected = false),
            TokenModel(token = "", expected = false),
            TokenModel(token = "  ", expected = false),
        )
    }

    data class TokenModel(val token: String?, val expected: Boolean)

    private fun createRepository(token: String?): DefaultNativeSurveyRepository {
        return DefaultNativeSurveyRepository(
            api = api,
            storeEnvironmentConfig = storeEnvironmentConfig(token),
            definitionStore = definitionStore,
            stateStore = stateStore,
            languageProvider = languageProvider,
            analyticsEventHandler = analyticsEventHandler,
            appInfoProvider = appInfoProvider,
            currentTimeProvider = { NOW },
            appScope = TestAppCoroutineScope(),
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Load {

        @Test
        fun `GIVEN a saved definition WHEN load THEN returns it from cache without a request`() = runTest {
            // Arrange
            coEvery { definitionStore.get(SURVEY_ID, "de") } returns SavedSurveyDefinition("en", questionsDto())

            // Act
            val actual = repository.load(entry())

            // Assert
            assertThat(actual).isEqualTo(LoadedNativeSurvey(npsSurvey(language = "en"), isFromCache = true))
            coVerify(exactly = 0) { api.getQuestions(any(), any(), any()) }
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        }

        @Test
        fun `GIVEN nothing saved WHEN load THEN requests the asked language and saves the questions`() = runTest {
            // Arrange
            coEvery { api.getQuestions(SURVEY_ID, 100, "de") } returns questionsDto()

            // Act
            val actual = repository.load(entry())

            // Assert
            assertThat(actual).isEqualTo(LoadedNativeSurvey(npsSurvey(language = "de"), isFromCache = false))
            coVerify(exactly = 1) {
                definitionStore.save(SURVEY_ID, "de", SavedSurveyDefinition(language = "de", questionsDto()))
            }
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        }

        @Test
        fun `GIVEN the base language WHEN load THEN requests without a language label`() = runTest {
            // Arrange
            every { languageProvider.current() } returns "en"
            coEvery { api.getQuestions(SURVEY_ID, 100, null) } returns questionsDto()

            // Act
            val actual = repository.load(entry())

            // Assert
            assertThat(actual).isEqualTo(LoadedNativeSurvey(npsSurvey(language = "en"), isFromCache = false))
        }

        @Test
        fun `GIVEN no survey in the base language WHEN load THEN makes no fallback request`() = runTest {
            // Arrange
            every { languageProvider.current() } returns "en"
            coEvery { api.getQuestions(SURVEY_ID, 100, null) } throws httpError(code = 404)

            // Act
            val actual = repository.load(entry())

            // Assert
            assertThat(actual).isNull()
            coVerify(exactly = 1) { api.getQuestions(any(), any(), any()) }
        }

        @Test
        fun `GIVEN no translation for the asked language WHEN load THEN falls back to the base language`() = runTest {
            // Arrange
            coEvery { api.getQuestions(SURVEY_ID, 100, "de") } throws httpError(code = 404)
            coEvery { api.getQuestions(SURVEY_ID, 100, null) } returns questionsDto()

            // Act
            val actual = repository.load(entry())

            // Assert
            assertThat(actual).isEqualTo(LoadedNativeSurvey(npsSurvey(language = "en"), isFromCache = false))
            coVerify(exactly = 1) {
                definitionStore.save(SURVEY_ID, "de", SavedSurveyDefinition(language = "en", questionsDto()))
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a failed load WHEN load twice THEN only a network failure lets the survey be asked again`(
            model: LoadFailureModel,
        ) = runTest {
            // Arrange
            val events = mutableListOf<AnalyticsEvent>()
            justRun { analyticsEventHandler.send(capture(events)) }
            coEvery { api.getQuestions(any(), any(), any()) } answers { model.response.getOrThrow() }

            // Act
            val actual = List(size = 2) { repository.load(entry()) }

            // Assert
            assertThat(actual).containsExactly(null, null)
            coVerify(exactly = if (model.isBlocked) 1 else 2) { api.getQuestions(any(), any(), any()) }
            coVerify(exactly = 0) { definitionStore.save(any(), any(), any()) }
            assertThat(events).containsExactly(
                loadError(model.failure, isRequestSent = true),
                loadError(model.failure, isRequestSent = !model.isBlocked),
            ).inOrder()
        }

        private fun provideTestModels() = listOf(
            LoadFailureModel(Result.failure(UnknownHostException()), SurveyRequestFailure.Network, isBlocked = false),
            LoadFailureModel(Result.failure(EOFException()), SurveyRequestFailure.Parse, isBlocked = true),
            LoadFailureModel(Result.failure(httpError(400)), SurveyRequestFailure.Http(400), isBlocked = true),
            LoadFailureModel(Result.failure(httpError(429)), SurveyRequestFailure.Http(429), isBlocked = true),
            LoadFailureModel(Result.failure(httpError(503)), SurveyRequestFailure.Http(503), isBlocked = true),
            LoadFailureModel(
                response = Result.success(questionsDto(questions = listOf(scoreQuestion()))),
                failure = SurveyRequestFailure.Unsupported,
                isBlocked = true,
            ),
        )

        @Test
        fun `GIVEN an answer refused for a saved survey WHEN load THEN the survey is not shown again`() = runTest {
            // Arrange
            coEvery { definitionStore.get(SURVEY_ID, "de") } returns SavedSurveyDefinition("de", questionsDto())
            coEvery { api.createResponse(any()) } throws httpError(code = 401)
            repository.submit(entry(), npsSurvey(), RESPONSE_UUID, answers())

            // Act
            val actual = repository.load(entry())

            // Assert
            assertThat(actual).isNull()
            verify(exactly = 1) {
                analyticsEventHandler.send(loadError(SurveyRequestFailure.Http(401), isRequestSent = false))
            }
        }

        @Test
        fun `GIVEN two loads at once WHEN the request is in flight THEN the second one reads what the first saved`() =
            runTest {
                // Arrange
                val response = CompletableDeferred<SurveySparrowQuestionsDto>()
                coEvery { api.getQuestions(any(), any(), any()) } coAnswers { response.await() }
                var saved: SavedSurveyDefinition? = null
                coEvery { definitionStore.save(SURVEY_ID, "de", any()) } answers { saved = thirdArg() }
                coEvery { definitionStore.get(SURVEY_ID, "de") } answers { saved }

                // Act
                val first = async { repository.load(entry()) }
                val second = async { repository.load(entry()) }
                runCurrent()
                response.complete(questionsDto())

                // Assert
                assertThat(listOf(first.await(), second.await())).containsExactly(
                    LoadedNativeSurvey(npsSurvey(language = "de"), isFromCache = false),
                    LoadedNativeSurvey(npsSurvey(language = "de"), isFromCache = true),
                )
                coVerify(exactly = 1) { api.getQuestions(any(), any(), any()) }
            }

        @Test
        fun `GIVEN the caller is gone WHEN the questions arrive THEN they are still saved`() = runTest {
            // Arrange
            val response = CompletableDeferred<SurveySparrowQuestionsDto>()
            coEvery { api.getQuestions(any(), any(), any()) } coAnswers { response.await() }
            val caller = launch { repository.load(entry()) }
            runCurrent()

            // Act
            caller.cancelAndJoin()
            response.complete(questionsDto())

            // Assert
            coVerify(exactly = 1) {
                definitionStore.save(SURVEY_ID, "de", SavedSurveyDefinition(language = "de", questionsDto()))
            }
        }
    }

    data class LoadFailureModel(
        val response: Result<SurveySparrowQuestionsDto>,
        val failure: SurveyRequestFailure,
        val isBlocked: Boolean,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Submit {

        @Test
        fun `GIVEN a score and a comment WHEN submit THEN sends them with the response variables`() = runTest {
            // Arrange
            val body = slot<CreateSurveySparrowResponseBody>()
            coJustRun { api.createResponse(capture(body)) }

            // Act
            val actual = repository.submit(
                entry = entry(),
                survey = npsSurvey(language = "en"),
                responseUuid = RESPONSE_UUID,
                answers = answers(comment = "  Add more networks\n"),
            )

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            assertThat(body.captured).isEqualTo(
                CreateSurveySparrowResponseBody(
                    surveyId = SURVEY_ID,
                    answers = listOf(
                        SurveySparrowAnswerDto(questionId = SCORE_QUESTION_ID, answer = "9"),
                        SurveySparrowAnswerDto(questionId = COMMENT_QUESTION_ID, answer = "Add more networks"),
                    ),
                    variables = mapOf(
                        "user_wallet_id" to UserWalletId(WALLET_ID).hashedStringValue,
                        "response_uuid" to RESPONSE_UUID,
                        "app_version" to APP_VERSION,
                        "platform" to "android",
                        "source" to "sheet",
                        "stage" to "funded_30d",
                        "wallet_type" to "cold",
                        "language" to "en",
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN a blank comment WHEN submit THEN only the score is sent and the answer is recorded`() = runTest {
            // Arrange
            val body = slot<CreateSurveySparrowResponseBody>()
            coJustRun { api.createResponse(capture(body)) }

            // Act
            repository.submit(entry(), npsSurvey(), RESPONSE_UUID, answers(comment = "  "))

            // Assert
            val sent = listOf(SurveyAnswer.Score(questionId = SCORE_QUESTION_ID, value = 9))
            assertThat(body.captured.answers).containsExactly(SurveySparrowAnswerDto(SCORE_QUESTION_ID, "9"))
            coVerify(exactly = 1) { stateStore.markAnswered(SURVEY_ID, NOW, REPEAT_AFTER_DAYS) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    NativeSurveyAnalyticsEvent.SurveySubmitted(entry(), NativeSurvey.Type.NPS, sentAnswers = sent),
                )
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a failed submit WHEN submit twice THEN the failure decides retry, block and saved questions`(
            model: SubmitFailureModel,
        ) = runTest {
            // Arrange
            val events = mutableListOf<AnalyticsEvent>()
            justRun { analyticsEventHandler.send(capture(events)) }
            coEvery { api.createResponse(any()) } throws model.error

            // Act
            val actual = List(size = 2) { repository.submit(entry(), npsSurvey(), RESPONSE_UUID, answers()) }

            // Assert
            assertThat(actual).containsExactly(model.expected.left(), model.expected.left())
            coVerify(exactly = if (model.isBlocked) 1 else 2) { api.createResponse(any()) }
            coVerify(exactly = if (model.isDefinitionDropped) 1 else 0) { definitionStore.remove(SURVEY_ID) }
            coVerify(exactly = 0) { stateStore.markAnswered(any(), any(), any()) }
            assertThat(events).containsExactly(
                submitError(model.failure, isRequestSent = true),
                submitError(model.failure, isRequestSent = !model.isBlocked),
            ).inOrder()
        }

        private fun provideTestModels() = listOf(
            SubmitFailureModel(UnknownHostException(), SurveyRequestFailure.Network, SurveySubmitError.RETRY),
            SubmitFailureModel(EOFException(), SurveyRequestFailure.Parse, SurveySubmitError.RETRY),
            SubmitFailureModel(httpError(503), SurveyRequestFailure.Http(503), SurveySubmitError.RETRY),
            SubmitFailureModel(
                error = httpError(429),
                failure = SurveyRequestFailure.Http(429),
                expected = SurveySubmitError.RETRY,
                isBlocked = true,
            ),
            SubmitFailureModel(
                error = httpError(400),
                failure = SurveyRequestFailure.Http(400),
                expected = SurveySubmitError.CLOSE,
                isBlocked = true,
                isDefinitionDropped = true,
            ),
        ) + listOf(401, 403, 404).map { code ->
            SubmitFailureModel(
                error = httpError(code),
                failure = SurveyRequestFailure.Http(code),
                expected = SurveySubmitError.CLOSE,
                isBlocked = true,
            )
        }

        @Test
        fun `GIVEN the caller is gone WHEN the vendor accepts the answer THEN it is still recorded`() = runTest {
            // Arrange
            val accepted = CompletableDeferred<Unit>()
            coEvery { api.createResponse(any()) } coAnswers { accepted.await() }
            val caller = launch { repository.submit(entry(), npsSurvey(), RESPONSE_UUID, answers()) }
            runCurrent()

            // Act
            caller.cancelAndJoin()
            accepted.complete(Unit)

            // Assert
            coVerify(exactly = 1) { stateStore.markAnswered(SURVEY_ID, NOW, REPEAT_AFTER_DAYS) }
        }
    }

    data class SubmitFailureModel(
        val error: Throwable,
        val failure: SurveyRequestFailure,
        val expected: SurveySubmitError,
        val isBlocked: Boolean = false,
        val isDefinitionDropped: Boolean = false,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsAnswered {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an answer record WHEN isAnswered THEN it counts only within its term`(model: IsAnsweredModel) =
            runTest {
                // Arrange
                coEvery { stateStore.deviceRecord(SURVEY_ID) } returns model.record

                // Act
                val actual = repository.isAnswered(entry())

                // Assert
                assertThat(actual).isEqualTo(model.expected)
            }

        private fun provideTestModels() = listOf(
            IsAnsweredModel(record = SurveyDeviceRecord(NOW - 1, REPEAT_AFTER_DAYS), expected = true),
            IsAnsweredModel(
                record = SurveyDeviceRecord(NOW - REPEAT_AFTER_DAYS.days.inWholeMilliseconds, REPEAT_AFTER_DAYS),
                expected = false,
            ),
            IsAnsweredModel(record = null, expected = false),
        )
    }

    data class IsAnsweredModel(val record: SurveyDeviceRecord?, val expected: Boolean)

    private fun entry() = SurveyEntry(
        surveyId = SURVEY_ID,
        source = SurveySource.SHEET,
        stage = "funded_30d",
        repeatAfterDays = REPEAT_AFTER_DAYS,
        userWallet = userWallet,
    )

    private fun answers(comment: String = "") = listOf(
        SurveyAnswer.Score(questionId = SCORE_QUESTION_ID, value = 9),
        SurveyAnswer.Text(questionId = COMMENT_QUESTION_ID, value = comment),
    )

    private fun loadError(failure: SurveyRequestFailure, isRequestSent: Boolean) =
        NativeSurveyAnalyticsEvent.SurveyError(entry(), null, Request.LOAD, failure, isRequestSent)

    private fun submitError(failure: SurveyRequestFailure, isRequestSent: Boolean) =
        NativeSurveyAnalyticsEvent.SurveyError(entry(), NativeSurvey.Type.NPS, Request.SUBMIT, failure, isRequestSent)

    private fun storeEnvironmentConfig(token: String?): StoreEnvironmentConfig = mockk {
        every { surveySparrowToken } returns token
    }

    private fun httpError(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody(null)))

    private companion object {
        const val WALLET_ID = "0A1B2C3D4E5F"
        const val RESPONSE_UUID = "6f1c2e0a-5b7d-4c21-9a3e-0d8b6a1f4e27"
        const val APP_VERSION = "6.9.0"
        const val REPEAT_AFTER_DAYS = 180
        const val NOW = 1_794_000_000_000L
    }
}