package com.tangem.features.survey.impl.nativesurvey.screen.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.survey.NativeSurveyComponent
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.COMMENT_QUESTION_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.COMMENT_TEXT
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SCORE_QUESTION_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SCORE_TEXT
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SURVEY_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.npsSurvey
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent.CloseMethod
import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyAnswer
import com.tangem.features.survey.impl.nativesurvey.model.SurveyEntry
import com.tangem.features.survey.impl.nativesurvey.model.SurveySource
import com.tangem.features.survey.impl.nativesurvey.repository.LoadedNativeSurvey
import com.tangem.features.survey.impl.nativesurvey.repository.NativeSurveyRepository
import com.tangem.features.survey.impl.nativesurvey.repository.SurveySubmitError
import com.tangem.features.survey.impl.nativesurvey.screen.ui.state.NativeSurveyUM
import com.tangem.features.survey.impl.nativesurvey.screen.ui.state.NativeSurveyUM.BodyUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NativeSurveyModelTest {

    private val repository: NativeSurveyRepository = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val uiMessageSender: UiMessageSender = mockk()
    private val router: Router = mockk(relaxed = true)
    private val userWallet: UserWallet.Cold = mockk()

    private val events = mutableListOf<AnalyticsEvent>()
    private val messages = mutableListOf<UiMessage>()
    private val models = mutableListOf<NativeSurveyModel>()

    @BeforeEach
    fun setUp() {
        clearMocks(repository, getUserWalletUseCase, analyticsEventHandler, uiMessageSender, router, userWallet)
        events.clear()
        messages.clear()
        every { getUserWalletUseCase(USER_WALLET_ID) } returns userWallet.right()
        coEvery { repository.load(entry()) } returns LoadedNativeSurvey(npsSurvey(), isFromCache = true)
        justRun { analyticsEventHandler.send(capture(events)) }
        justRun { uiMessageSender.send(capture(messages)) }
    }

    @AfterEach
    fun tearDown() {
        models.forEach(NativeSurveyModel::onDestroy)
        models.clear()
    }

    @Test
    fun `GIVEN a survey WHEN the screen opens THEN the form has nothing chosen and Opened is sent once`() = runTest {
        // Act
        val model = createModel()

        // Assert
        val form = model.form()
        val body = form.body as BodyUM.Score
        assertThat(body.question).isEqualTo(SCORE_TEXT)
        assertThat(body.selectedScore).isNull()
        assertThat(form.comment).isNull()
        assertThat(form.isSubmitEnabled).isFalse()
        assertThat(events).containsExactly(opened())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a source WHEN a fresh survey opens THEN Opened carries the source and the fresh load`(
        model: SourceModel,
    ) = runTest {
        // Arrange
        coEvery { repository.load(any()) } returns LoadedNativeSurvey(npsSurvey(), isFromCache = false)

        // Act
        createModel(source = model.source)

        // Assert
        assertThat(events).containsExactly(
            NativeSurveyAnalyticsEvent.SurveyOpened(
                entry = entry(source = model.expected),
                surveyType = NativeSurvey.Type.NPS,
                isFromCache = false,
                isAlreadyPassed = false,
            ),
        )
    }

    @Test
    fun `GIVEN the wallet is gone WHEN the screen opens THEN it closes without loading or events`() = runTest {
        // Arrange
        every { getUserWalletUseCase(USER_WALLET_ID) } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        createModel()

        // Assert
        verify(exactly = 1) { router.pop() }
        coVerify(exactly = 0) { repository.load(any()) }
        assertThat(events).isEmpty()
    }

    @Test
    fun `GIVEN the survey is loading WHEN closed THEN it closes without events`() = runTest {
        // Arrange
        val loaded = CompletableDeferred<LoadedNativeSurvey?>()
        coEvery { repository.load(entry()) } coAnswers { loaded.await() }
        val model = createModel()

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(NativeSurveyUM.Loading::class.java)
        verify(exactly = 1) { router.pop() }
        assertThat(events).isEmpty()
    }

    @Test
    fun `GIVEN the survey cannot be loaded WHEN the screen opens THEN it closes without events`() = runTest {
        // Arrange
        coEvery { repository.load(entry()) } returns null

        // Act
        createModel()

        // Assert
        verify(exactly = 1) { router.pop() }
        assertThat(events).isEmpty()
    }

    @Test
    fun `GIVEN an answered survey WHEN the screen opens THEN it shows Done without loading`() = runTest {
        // Act
        val model = createModel(isAlreadyAnswered = true)

        // Assert
        assertThat(model.uiState.value).isInstanceOf(NativeSurveyUM.Done::class.java)
        coVerify(exactly = 0) { repository.load(any()) }
        assertThat(events).containsExactly(openedAnswered())
    }

    @ParameterizedTest
    @ValueSource(ints = [6, 9])
    fun `GIVEN a comment WHEN a score is chosen again THEN it is selected and the comment stays`(score: Int) =
        runTest {
            // Arrange
            val model = createModel()
            model.form().scoreClick()(9)
            model.form().comment?.onValueChange?.invoke("Add more networks")

            // Act
            model.form().scoreClick()(score)

            // Assert
            val form = model.form()
            assertThat((form.body as BodyUM.Score).selectedScore).isEqualTo(score)
            assertThat(form.comment?.question).isEqualTo(COMMENT_TEXT)
            assertThat(form.comment?.value).isEqualTo("Add more networks")
            assertThat(form.isSubmitEnabled).isTrue()
        }

    @Test
    fun `GIVEN a long comment WHEN typed THEN it is cut to the limit`() = runTest {
        // Arrange
        val model = createModel()
        model.form().scoreClick()(9)

        // Act
        model.form().comment?.onValueChange?.invoke("a".repeat(1001))

        // Assert
        assertThat(model.form().comment?.value).hasLength(1000)
    }

    @Test
    fun `GIVEN chosen answers WHEN sent THEN the vendor gets them and Done is shown`() = runTest {
        // Arrange
        val model = createModel()
        model.form().scoreClick()(9)
        model.form().comment?.onValueChange?.invoke("Add more networks")
        coEvery { repository.submit(any(), any(), any(), any()) } returns Unit.right()

        // Act
        model.form().onSubmitClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(NativeSurveyUM.Done::class.java)
        coVerify(exactly = 1) {
            repository.submit(
                entry = entry(),
                survey = npsSurvey(),
                responseUuid = any(),
                answers = listOf(
                    SurveyAnswer.Score(SCORE_QUESTION_ID, 9),
                    SurveyAnswer.Text(COMMENT_QUESTION_ID, "Add more networks"),
                ),
            )
        }
    }

    @Test
    fun `GIVEN a failure to retry WHEN sent twice THEN the answers stay and both sends share one response id`() =
        runTest {
            // Arrange
            val model = createModel()
            model.form().scoreClick()(9)
            model.form().comment?.onValueChange?.invoke("Add more networks")
            val answered = model.form()
            val responseIds = mutableListOf<String>()
            coEvery { repository.submit(any(), any(), capture(responseIds), any()) } returns
                SurveySubmitError.RETRY.left()

            // Act
            model.form().onSubmitClick()
            advanceUntilIdle()
            (messages.single() as DialogMessage).onDismissRequest()
            model.form().onSubmitClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.form()).isEqualTo(answered)
            assertThat(responseIds).hasSize(2)
            assertThat(responseIds.distinct()).hasSize(1)
            verify(exactly = 0) { router.pop() }
        }

    @Test
    fun `GIVEN a failure that closes the survey WHEN the alert is dismissed THEN the screen closes with Error`() =
        runTest {
            // Arrange
            val model = createModel()
            model.form().scoreClick()(9)
            coEvery { repository.submit(any(), any(), any(), any()) } returns SurveySubmitError.CLOSE.left()
            model.form().onSubmitClick()
            advanceUntilIdle()

            // Act
            (messages.single() as DialogMessage).onDismissRequest()

            // Assert
            verify(exactly = 1) { router.pop() }
            assertThat(events).containsExactly(
                opened(),
                closed(CloseMethod.ERROR, isScoreSelected = true),
            ).inOrder()
        }

    @Test
    fun `GIVEN the screen is gone WHEN the closing alert is dismissed THEN nothing is closed or sent`() = runTest {
        // Arrange
        val model = createModel()
        model.form().scoreClick()(9)
        coEvery { repository.submit(any(), any(), any(), any()) } returns SurveySubmitError.CLOSE.left()
        model.form().onSubmitClick()
        advanceUntilIdle()
        model.onDestroy()

        // Act
        (messages.single() as DialogMessage).onDismissRequest()

        // Assert
        verify(exactly = 0) { router.pop() }
        assertThat(events).containsExactly(opened())
    }

    @Test
    fun `GIVEN the answer is being sent WHEN closed, rescored or sent again THEN nothing happens`() = runTest {
        // Arrange
        val model = createModel()
        model.form().scoreClick()(9)
        val accepted = CompletableDeferred<Unit>()
        coEvery { repository.submit(any(), any(), any(), any()) } coAnswers { accepted.await().right() }
        model.form().onSubmitClick()
        advanceUntilIdle()

        // Act
        model.form().onCloseClick()
        model.onSystemBack()
        model.form().scoreClick()(3)
        model.form().onSubmitClick()
        advanceUntilIdle()

        // Assert
        assertThat((model.form().body as BodyUM.Score).selectedScore).isEqualTo(9)
        assertThat(model.form().isSubmitting).isTrue()
        verify(exactly = 0) { router.pop() }
        coVerify(exactly = 1) { repository.submit(any(), any(), any(), any()) }
        assertThat(events).containsExactly(opened())
        accepted.complete(Unit)
    }

    @ParameterizedTest
    @EnumSource(value = CloseMethod::class, names = ["CROSS", "SYSTEM_BACK"])
    fun `GIVEN the form WHEN closed twice the same way THEN it closes once with that method`(method: CloseMethod) =
        runTest {
            // Arrange
            val model = createModel()
            model.form().scoreClick()(4)

            // Act
            repeat(2) {
                when (method) {
                    CloseMethod.CROSS -> model.form().onCloseClick()
                    else -> model.onSystemBack()
                }
            }

            // Assert
            verify(exactly = 1) { router.pop() }
            assertThat(events).containsExactly(opened(), closed(method, isScoreSelected = true)).inOrder()
        }

    @Test
    fun `GIVEN the form WHEN closed by the cross and then by back THEN it closes once with Closed`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.form().onCloseClick()
        model.onSystemBack()

        // Assert
        verify(exactly = 1) { router.pop() }
        assertThat(events).containsExactly(opened(), closed(CloseMethod.CROSS, isScoreSelected = false)).inOrder()
    }

    @Test
    fun `GIVEN Done WHEN closed twice THEN the screen closes once without Closed`() = runTest {
        // Arrange
        val model = createModel(isAlreadyAnswered = true)

        // Act
        model.onSystemBack()
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 1) { router.pop() }
        assertThat(events).containsExactly(openedAnswered())
    }

    private fun TestScope.createModel(
        isAlreadyAnswered: Boolean = false,
        source: AppRoute.NativeSurvey.Source = AppRoute.NativeSurvey.Source.LINK,
    ): NativeSurveyModel {
        val model = NativeSurveyModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                NativeSurveyComponent.Params(
                    surveyId = SURVEY_ID,
                    source = source,
                    stage = STAGE,
                    repeatAfterDays = REPEAT_AFTER_DAYS,
                    userWalletId = USER_WALLET_ID,
                    isAlreadyAnswered = isAlreadyAnswered,
                ),
            ),
            router = router,
            repository = repository,
            getUserWalletUseCase = getUserWalletUseCase,
            analyticsEventHandler = analyticsEventHandler,
            uiMessageSender = uiMessageSender,
        )
        advanceUntilIdle()
        models += model
        return model
    }

    private fun opened() = NativeSurveyAnalyticsEvent.SurveyOpened(
        entry = entry(),
        surveyType = NativeSurvey.Type.NPS,
        isFromCache = true,
        isAlreadyPassed = false,
    )

    private fun openedAnswered() = NativeSurveyAnalyticsEvent.SurveyOpened(
        entry = entry(),
        surveyType = null,
        isFromCache = true,
        isAlreadyPassed = true,
    )

    private fun closed(method: CloseMethod, isScoreSelected: Boolean) = NativeSurveyAnalyticsEvent.SurveyClosed(
        entry = entry(),
        surveyType = NativeSurvey.Type.NPS,
        closeMethod = method,
        isScoreSelected = isScoreSelected,
    )

    private fun NativeSurveyModel.form() = uiState.value as NativeSurveyUM.Form

    private fun NativeSurveyUM.Form.scoreClick() = (body as BodyUM.Score).onScoreClick

    private fun entry(source: SurveySource = SurveySource.LINK) = SurveyEntry(
        surveyId = SURVEY_ID,
        source = source,
        stage = STAGE,
        repeatAfterDays = REPEAT_AFTER_DAYS,
        userWallet = userWallet,
    )

    private fun provideTestModels() = listOf(
        SourceModel(source = AppRoute.NativeSurvey.Source.SHEET, expected = SurveySource.SHEET),
        SourceModel(source = AppRoute.NativeSurvey.Source.PUSH, expected = SurveySource.PUSH),
        SourceModel(source = AppRoute.NativeSurvey.Source.BANNER, expected = SurveySource.BANNER),
        SourceModel(source = AppRoute.NativeSurvey.Source.LINK, expected = SurveySource.LINK),
    )

    data class SourceModel(val source: AppRoute.NativeSurvey.Source, val expected: SurveySource)

    private companion object {
        val USER_WALLET_ID = UserWalletId("0A1B2C3D4E5F")
        const val STAGE = "funded_30d"
        const val REPEAT_AFTER_DAYS = 180
    }
}