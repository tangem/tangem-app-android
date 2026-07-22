package com.tangem.feature.rating.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.feature.rating.ui.RatingFeedbackBS
import com.tangem.feature.rating.ui.RatingUM
import com.tangem.feature.swap.domain.SwapFeedbackUseCase
import com.tangem.feature.swap.domain.models.domain.SwapRating
import com.tangem.features.rating.RatingComponent
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class RatingModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val swapFeedbackUseCase: SwapFeedbackUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) : Model() {

    private val params: RatingComponent.Params = paramsContainer.require()

    private val isLoadFinished = MutableStateFlow(value = false)

    val state: StateFlow<RatingUM>
        field = MutableStateFlow(
            RatingUM(
                state = RatingUM.RatingState.Loading,
                feedbackBottomSheet = TangemBottomSheetConfig.Empty,
                onRatingSelected = ::onRatingSelected,
            ),
        )

    init {
        modelScope.launch {
            swapFeedbackUseCase.ensureLoaded(params.txExternalId)
            isLoadFinished.value = true
        }
        subscribeOnRatingUpdates()
    }

    fun onRatingSelected(rating: Int) {
        state.update { current ->
            val ratingState = current.state as? RatingUM.RatingState.Unrated ?: return@update current
            current.copy(
                state = ratingState.copy(selectedRating = rating),
                feedbackBottomSheet = buildFeedbackBottomSheet(),
            )
        }
    }

    private fun subscribeOnRatingUpdates() {
        combine(
            swapFeedbackUseCase.observeRating(params.txExternalId),
            isLoadFinished,
            ::toRatingState,
        )
            .onEach(::applyRatingState)
            .launchIn(modelScope)
    }

    /** Null means the rating is still loading and the current state must be kept */
    private fun toRatingState(entry: SwapRating?, isLoadFinished: Boolean): RatingUM.RatingState? {
        return when (entry) {
            is SwapRating.Rated -> RatingUM.RatingState.AlreadyRated(entry.rating)
            is SwapRating.NotRated -> RatingUM.RatingState.Unrated(selectedRating = null)
            // A failed load is not cached: once it finishes with nothing, fall back to Unrated
            null -> if (isLoadFinished) RatingUM.RatingState.Unrated(selectedRating = null) else null
        }
    }

    private fun applyRatingState(newState: RatingUM.RatingState?) {
        state.update { current ->
            if (newState == null) {
                current
            } else if (newState is RatingUM.RatingState.Unrated && current.state is RatingUM.RatingState.Unrated) {
                current // keep the user's selection
            } else if (newState is RatingUM.RatingState.AlreadyRated) {
                current.copy(
                    state = newState,
                    feedbackBottomSheet = current.feedbackBottomSheet.copy(isShown = false),
                )
            } else {
                current.copy(state = newState)
            }
        }
    }

    private fun onFeedbackChanged(text: String) {
        state.update { current ->
            val bs = current.feedbackBottomSheet
            val content = bs.content as? RatingFeedbackBS ?: return@update current
            current.copy(feedbackBottomSheet = bs.copy(content = content.copy(feedbackText = text)))
        }
    }

    private fun onDismissFeedbackBottomSheet() {
        state.update { current ->
            current.copy(feedbackBottomSheet = current.feedbackBottomSheet.copy(isShown = false))
        }
    }

    private fun onSubmit() {
        val current = state.value
        val ratingState = current.state as? RatingUM.RatingState.Unrated ?: return
        val selectedRating = ratingState.selectedRating ?: return
        val content = current.feedbackBottomSheet.content as? RatingFeedbackBS ?: return

        val submitParams = SwapFeedbackUseCase.SubmitParams(
            txExternalId = params.txExternalId,
            providerName = params.providerName,
            txExternalUrl = params.txExternalUrl,
            userWalletId = params.userWalletId,
            rating = selectedRating,
            feedback = content.feedbackText,
        )
        // The app scope outlives this model, so the POST survives closing the sheet; on error the
        // repository rolls the optimistic rating back and the failure is only logged (not user-critical)
        appCoroutineScope.launch {
            swapFeedbackUseCase.submit(submitParams).onLeft { error ->
                TangemLogger.e("RatingModel: failed to submit swap feedback: $error")
            }
        }
        // AlreadyRated arrives via the rating observation, which also hides the bottom sheet
    }

    private fun buildFeedbackBottomSheet(): TangemBottomSheetConfig {
        return TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = ::onDismissFeedbackBottomSheet,
            content = RatingFeedbackBS(
                feedbackText = "",
                isSubmitting = false,
                onFeedbackChanged = ::onFeedbackChanged,
                onDismiss = ::onDismissFeedbackBottomSheet,
                onSubmit = ::onSubmit,
            ),
        )
    }
}