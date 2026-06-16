package com.tangem.feature.rating.model

import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.feature.rating.ui.RatingFeedbackBS
import com.tangem.feature.rating.ui.RatingUM
import com.tangem.features.rating.RatingComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class RatingModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
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
        loadRating()
    }

    fun onRatingSelected(rating: Int) {
        state.update { current ->
            val ratingState = current.state as? RatingUM.RatingState.Unrated ?: return@update current
            current.copy(
                state = ratingState.copy(selectedRating = rating),
                feedbackBottomSheet = buildFeedbackBottomSheet(feedbackText = "", isSubmitting = false),
            )
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

        state.update {
            current.copy(
                feedbackBottomSheet = current.feedbackBottomSheet.copy(
                    content = content.copy(isSubmitting = true),
                ),
            )
        }
        modelScope.launch {
            try {
                params.onSubmitRating(selectedRating, content.feedbackText)
                state.update { um ->
                    um.copy(
                        state = RatingUM.RatingState.AlreadyRated(selectedRating),
                        feedbackBottomSheet = um.feedbackBottomSheet.copy(isShown = false),
                    )
                }
            } catch (e: Exception) {
                TangemLogger.e("RatingModel: onSubmitRating failed", e)
                uiMessageSender.send(SnackbarMessage(message = resourceReference(R.string.common_something_went_wrong)))
                state.update { um ->
                    val bsContent = um.feedbackBottomSheet.content as? RatingFeedbackBS ?: return@update um
                    um.copy(
                        feedbackBottomSheet = um.feedbackBottomSheet.copy(
                            content = bsContent.copy(isSubmitting = false),
                        ),
                    )
                }
            }
        }
    }

    private fun buildFeedbackBottomSheet(feedbackText: String, isSubmitting: Boolean): TangemBottomSheetConfig {
        return TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = ::onDismissFeedbackBottomSheet,
            content = RatingFeedbackBS(
                feedbackText = feedbackText,
                isSubmitting = isSubmitting,
                onFeedbackChanged = ::onFeedbackChanged,
                onDismiss = ::onDismissFeedbackBottomSheet,
                onSubmit = ::onSubmit,
            ),
        )
    }

    private fun loadRating() = modelScope.launch {
        val existingRating = try {
            params.onLoadRating()
        } catch (e: Exception) {
            TangemLogger.e("RatingModel: onLoadRating failed", e)
            null
        }
        state.update { current ->
            current.copy(
                state = if (existingRating != null) {
                    RatingUM.RatingState.AlreadyRated(existingRating)
                } else {
                    RatingUM.RatingState.Unrated(selectedRating = null)
                },
            )
        }
    }
}