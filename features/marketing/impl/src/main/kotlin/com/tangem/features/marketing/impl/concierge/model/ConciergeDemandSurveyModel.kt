package com.tangem.features.marketing.impl.concierge.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.marketing.SubmitDemandSurveyUseCase
import com.tangem.domain.marketing.models.SurveyAnswer
import com.tangem.features.marketing.api.concierge.ConciergeDemandSurveyComponent
import com.tangem.features.marketing.impl.concierge.ui.state.ConciergeDemandSurveyUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class ConciergeDemandSurveyModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val submitDemandSurveyUseCase: SubmitDemandSurveyUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<ConciergeDemandSurveyComponent.Params>()

    private var isFinished = false

    /** The survey step stays tappable while it slides out, so a sent survey must not be sent again from it. */
    private var isSent = false

    val stackNavigation = StackNavigation<ConciergeDemandSurveyRoute>()

    val uiState: StateFlow<ConciergeDemandSurveyUM>
        field = MutableStateFlow(createInitialState())

    private fun createInitialState(): ConciergeDemandSurveyUM = ConciergeDemandSurveyUM(
        questions = ConciergeDemandSurveyQuestion.entries.map { question ->
            ConciergeDemandSurveyUM.QuestionUM(
                key = question.key,
                title = question.title,
                hint = question.hint,
                isOptional = question.isOptional,
                options = question.options.map { option ->
                    ConciergeDemandSurveyUM.OptionUM(
                        key = option.key,
                        label = option.label,
                        isSelected = false,
                        onClick = { selectOption(questionKey = question.key, optionKey = option.key) },
                    )
                }.toImmutableList(),
            )
        }.toImmutableList(),
        isSubmitting = false,
        onSubmitClick = ::submit,
        onDismiss = ::onClose,
    )

    /**
     * The close button and the sheet's own dismissal can both ask to close, so only the first one pops. Closing while
     * the answers are being sent would cancel the request before its result is shown, so it waits for the result.
     */
    private fun onClose() {
        if (isFinished || uiState.value.isSubmitting) return
        isFinished = true
        router.pop()
    }

    private fun selectOption(questionKey: String, optionKey: String) {
        if (uiState.value.isSubmitting) return
        uiState.update { state ->
            state.copy(
                questions = state.questions.map { question ->
                    if (question.key != questionKey) return@map question
                    question.copy(
                        options = question.options
                            .map { option -> option.copy(isSelected = option.key == optionKey) }
                            .toImmutableList(),
                    )
                }.toImmutableList(),
            )
        }
    }

    private fun submit() {
        val state = uiState.value
        if (isSent || !state.isSubmitEnabled || state.isSubmitting) return

        uiState.update { it.copy(isSubmitting = true) }
        modelScope.launch {
            submitDemandSurveyUseCase(userWalletId = params.userWalletId, answers = state.toAnswers()).fold(
                ifLeft = { error ->
                    TangemLogger.e("Demand survey submission failed", error)
                    uiState.update { current -> current.copy(isSubmitting = false) }
                    showSubmissionError()
                },
                ifRight = {
                    isSent = true
                    uiState.update { current -> current.copy(isSubmitting = false) }
                    stackNavigation.replaceAll(ConciergeDemandSurveyRoute.Success)
                },
            )
        }
    }

    private fun ConciergeDemandSurveyUM.toAnswers(): List<SurveyAnswer> = questions.mapNotNull { question ->
        question.selectedOptionKey?.let { answerKey ->
            SurveyAnswer(questionKey = question.key, answerKey = answerKey)
        }
    }

    private fun showSubmissionError() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_unknown_error),
            ),
        )
    }
}