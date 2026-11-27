package com.tangem.features.survey.impl.nativesurvey.screen.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.survey.NativeSurveyComponent
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent.CloseMethod
import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyAnswer
import com.tangem.features.survey.impl.nativesurvey.model.SurveyEntry
import com.tangem.features.survey.impl.nativesurvey.model.SurveyQuestion
import com.tangem.features.survey.impl.nativesurvey.model.toSurveySource
import com.tangem.features.survey.impl.nativesurvey.repository.NativeSurveyRepository
import com.tangem.features.survey.impl.nativesurvey.repository.SurveySubmitError
import com.tangem.features.survey.impl.nativesurvey.screen.ui.state.NativeSurveyUM
import com.tangem.features.survey.impl.nativesurvey.screen.ui.state.NativeSurveyUM.BodyUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class NativeSurveyModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val repository: NativeSurveyRepository,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<NativeSurveyComponent.Params>()

    private val responseUuid = UUID.randomUUID().toString()

    private var entry: SurveyEntry? = null
    private var survey: NativeSurvey? = null
    private var isFinished = false

    val uiState: StateFlow<NativeSurveyUM>
        field = MutableStateFlow<NativeSurveyUM>(NativeSurveyUM.Loading(onCloseClick = { close(CloseMethod.CROSS) }))

    init {
        modelScope.launch { open() }
    }

    fun onSystemBack() {
        close(CloseMethod.SYSTEM_BACK)
    }

    private suspend fun open() {
        val userWallet = getUserWalletUseCase(params.userWalletId).getOrNull() ?: run {
            TangemLogger.e("Native survey: the wallet of survey ${params.surveyId} is gone")
            return finish()
        }
        val entry = SurveyEntry(
            surveyId = params.surveyId,
            source = params.source.toSurveySource(),
            stage = params.stage,
            repeatAfterDays = params.repeatAfterDays,
            userWallet = userWallet,
        )
        val loaded = if (params.isAlreadyAnswered) {
            null
        } else {
            repository.load(entry) ?: return finish()
        }
        this.entry = entry
        survey = loaded?.survey
        uiState.value = loaded?.let { createFormState(it.survey) } ?: createDoneState()
        analyticsEventHandler.send(
            NativeSurveyAnalyticsEvent.SurveyOpened(
                entry = entry,
                surveyType = loaded?.survey?.type,
                // The entry point made the request, if any: the screen itself only reads what it cached
                isFromCache = loaded == null || params.isFromCache && loaded.isFromCache,
                isAlreadyPassed = params.isAlreadyAnswered,
            ),
        )
    }

    private fun createFormState(survey: NativeSurvey): NativeSurveyUM.Form {
        val scoreQuestion = survey.questions.filterIsInstance<SurveyQuestion.Nps>().first()
        return NativeSurveyUM.Form(
            body = BodyUM.Score(question = scoreQuestion.text, selectedScore = null, onScoreClick = ::selectScore),
            comment = null,
            isSubmitting = false,
            onSubmitClick = ::submit,
            onCloseClick = { close(CloseMethod.CROSS) },
        )
    }

    private fun createDoneState() = NativeSurveyUM.Done(onCloseClick = ::finish)

    private fun selectScore(score: Int) {
        val commentQuestion = survey?.questions?.filterIsInstance<SurveyQuestion.Text>()?.firstOrNull()
        updateForm { form ->
            val body = form.body as? BodyUM.Score ?: return@updateForm form
            form.copy(
                body = body.copy(selectedScore = score),
                comment = form.comment ?: commentQuestion?.let {
                    NativeSurveyUM.CommentUM(question = it.text, value = "", onValueChange = ::changeComment)
                },
            )
        }
    }

    private fun changeComment(value: String) {
        updateForm { form ->
            form.copy(comment = form.comment?.copy(value = value.takeWholeCharacters(COMMENT_MAX_LENGTH)))
        }
    }

    private fun submit() {
        val form = uiState.value as? NativeSurveyUM.Form ?: return
        val entry = entry ?: return
        val survey = survey ?: return
        if (form.isSubmitting || !form.isSubmitEnabled) return

        uiState.value = form.copy(isSubmitting = true)
        modelScope.launch {
            val answers = form.toAnswers(survey)
            repository.submit(entry = entry, survey = survey, responseUuid = responseUuid, answers = answers)
                .fold(
                    ifLeft = { error ->
                        uiState.value = form
                        showSubmitError(error)
                    },
                    ifRight = { uiState.value = createDoneState() },
                )
        }
    }

    private fun NativeSurveyUM.Form.toAnswers(survey: NativeSurvey): List<SurveyAnswer> {
        val score = (body as? BodyUM.Score)?.selectedScore
        return survey.questions.mapNotNull { question ->
            when (question) {
                is SurveyQuestion.Nps -> score?.let { SurveyAnswer.Score(question.id, it) }
                is SurveyQuestion.Text -> comment?.let { SurveyAnswer.Text(question.id, it.value) }
            }
        }
    }

    private fun showSubmitError(error: SurveySubmitError) {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
                onDismissRequest = {
                    when (error) {
                        SurveySubmitError.RETRY -> Unit
                        SurveySubmitError.CLOSE -> close(CloseMethod.ERROR)
                    }
                },
            ),
        )
    }

    /** Blocked while sending so the user sees the outcome. Only an abandoned form counts as Closed. */
    private fun close(method: CloseMethod) {
        val state = uiState.value
        if (isFinished || state is NativeSurveyUM.Form && state.isSubmitting) return

        val entry = entry
        if (state is NativeSurveyUM.Form && entry != null) {
            analyticsEventHandler.send(
                NativeSurveyAnalyticsEvent.SurveyClosed(
                    entry = entry,
                    surveyType = survey?.type,
                    closeMethod = method,
                    isScoreSelected = (state.body as? BodyUM.Score)?.selectedScore != null,
                ),
            )
        }
        finish()
    }

    /** Removes this survey wherever it is: a deep link may have opened another screen over it. */
    private fun finish() {
        if (isFinished) return
        isFinished = true
        router.navigate(transformer = { stack -> stack.filterNot { route -> route is AppRoute.NativeSurvey } })
    }

    /** The error alert outlives the screen, and its late dismissal must not close anything once the screen is gone. */
    override fun onDestroy() {
        isFinished = true
        super.onDestroy()
    }

    private fun updateForm(transform: (NativeSurveyUM.Form) -> NativeSurveyUM.Form) {
        uiState.update { state -> if (state is NativeSurveyUM.Form && !state.isSubmitting) transform(state) else state }
    }

    private companion object {
        const val COMMENT_MAX_LENGTH = 1000
    }
}