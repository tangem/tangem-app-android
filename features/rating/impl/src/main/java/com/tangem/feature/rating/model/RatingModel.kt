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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
            subscribeOnRatingUpdates()
        }
    }

    fun onRatingSelected(rating: Int) {
        state.update { current ->
            val ratingState = current.state as? RatingUM.RatingState.Unrated ?: return@update current
            current.copy(
                state = ratingState.copy(selectedRating = rating),
                feedbackBottomSheet = buildFeedbackBottomSheet(rating),
            )
        }
    }

    private fun subscribeOnRatingUpdates() {
        swapFeedbackUseCase.observeRating(params.txExternalId)
            .map(::toRatingState)
            .onEach(::applyRatingState)
            .launchIn(modelScope)
    }

    // Subscription starts after ensureLoaded completes, so a null here means the load finished with
    // nothing cached (a failed load is not cached) → fall back to Unrated
    private fun toRatingState(entry: SwapRating?): RatingUM.RatingState {
        return when (entry) {
            is SwapRating.Rated -> RatingUM.RatingState.AlreadyRated(entry.rating)
            is SwapRating.NotRated -> RatingUM.RatingState.Unrated(selectedRating = null)
            null -> RatingUM.RatingState.Unrated(selectedRating = null)
        }
    }

    private fun applyRatingState(newState: RatingUM.RatingState) {
        state.update { current ->
            if (newState is RatingUM.RatingState.Unrated && current.state is RatingUM.RatingState.Unrated) {
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

    private fun buildFeedbackBottomSheet(rating: Int): TangemBottomSheetConfig {
        return TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = ::onDismissFeedbackBottomSheet,
            content = RatingFeedbackBS(
                selectedRating = rating,
                feedbackText = "",
                isSubmitting = false,
                onFeedbackChanged = ::onFeedbackChanged,
                onDismiss = ::onDismissFeedbackBottomSheet,
                onSubmit = ::onSubmit,
            ),
        )
    }
}