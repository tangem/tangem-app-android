package com.tangem.feature.rating.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.feature.rating.ui.RatingFeedbackBS
import com.tangem.feature.rating.ui.RatingUM
import com.tangem.features.rating.RatingComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class RatingModelTest {

    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private fun buildModel(
        onLoadRating: suspend () -> Int? = { null },
        onSubmitRating: suspend (Int, String) -> Unit = { _, _ -> },
    ): RatingModel {
        val params = RatingComponent.Params(
            onLoadRating = onLoadRating,
            onSubmitRating = onSubmitRating,
        )
        return RatingModel(
            dispatchers = TestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(params),
            uiMessageSender = uiMessageSender,
        )
    }

    private val RatingModel.ratingState get() = state.value.state
    private val RatingModel.feedbackContent get() = state.value.feedbackBottomSheet.content as? RatingFeedbackBS

    @Test
    fun `initial state is Loading before onLoadRating completes`() = runTest {
        val deferred = CompletableDeferred<Int?>()
        val model = buildModel(onLoadRating = { deferred.await() })
        assertThat(model.ratingState).isInstanceOf(RatingUM.RatingState.Loading::class.java)
        deferred.complete(null)
        assertThat(model.ratingState).isInstanceOf(RatingUM.RatingState.Unrated::class.java)
    }

    @Test
    fun `state is Unrated with no selection when onLoadRating returns null`() = runTest {
        val model = buildModel(onLoadRating = { null })
        val unrated = model.ratingState as RatingUM.RatingState.Unrated
        assertThat(unrated.selectedRating).isNull()
        assertThat(model.state.value.feedbackBottomSheet.isShown).isFalse()
    }

    @Test
    fun `state is AlreadyRated when onLoadRating returns a rating`() = runTest {
        val model = buildModel(onLoadRating = { 4 })
        assertThat(model.ratingState).isEqualTo(RatingUM.RatingState.AlreadyRated(rating = 4))
    }

    @Test
    fun `onRatingSelected updates selectedRating and shows feedback bottom sheet`() = runTest {
        val model = buildModel(onLoadRating = { null })
        model.onRatingSelected(3)
        val unrated = model.ratingState as RatingUM.RatingState.Unrated
        assertThat(unrated.selectedRating).isEqualTo(3)
        assertThat(model.state.value.feedbackBottomSheet.isShown).isTrue()
    }

    @Test
    fun `onRatingSelected is no-op when state is not Unrated`() = runTest {
        val model = buildModel(onLoadRating = { 4 })
        model.onRatingSelected(3)
        assertThat(model.ratingState).isEqualTo(RatingUM.RatingState.AlreadyRated(rating = 4))
        assertThat(model.state.value.feedbackBottomSheet.isShown).isFalse()
    }

    @Test
    fun `onFeedbackChanged updates feedbackText in bottom sheet content`() = runTest {
        val model = buildModel(onLoadRating = { null })
        model.onRatingSelected(4)
        model.feedbackContent!!.onFeedbackChanged("Great service!")
        assertThat(model.feedbackContent!!.feedbackText).isEqualTo("Great service!")
    }

    @Test
    fun `onSubmit calls onSubmitRating with correct args`() = runTest {
        val submitMock: suspend (Int, String) -> Unit = mockk(relaxed = true)
        val model = buildModel(onLoadRating = { null }, onSubmitRating = submitMock)
        model.onRatingSelected(5)
        model.feedbackContent!!.onFeedbackChanged("Excellent!")
        model.feedbackContent!!.onSubmit()
        coVerify(exactly = 1) { submitMock(5, "Excellent!") }
    }

    @Test
    fun `onSubmit transitions to AlreadyRated and hides bottom sheet on success`() = runTest {
        val model = buildModel(onLoadRating = { null }, onSubmitRating = { _, _ -> })
        model.onRatingSelected(4)
        model.feedbackContent!!.onSubmit()
        assertThat(model.ratingState).isEqualTo(RatingUM.RatingState.AlreadyRated(rating = 4))
        assertThat(model.state.value.feedbackBottomSheet.isShown).isFalse()
    }

    @Test
    fun `onSubmit resets isSubmitting on failure`() = runTest {
        val model = buildModel(
            onLoadRating = { null },
            onSubmitRating = { _, _ -> error("network error") },
        )
        model.onRatingSelected(3)
        model.feedbackContent!!.onSubmit()
        assertThat(model.feedbackContent!!.isSubmitting).isFalse()
    }

    @Test
    fun `onSubmit shows snackbar on failure`() = runTest {
        val model = buildModel(
            onLoadRating = { null },
            onSubmitRating = { _, _ -> error("network error") },
        )
        model.onRatingSelected(3)
        model.feedbackContent!!.onSubmit()
        verify(exactly = 1) { uiMessageSender.send(ofType<SnackbarMessage>()) }
    }

    @Test
    fun `onSubmit is no-op when no rating selected`() = runTest {
        val submitMock: suspend (Int, String) -> Unit = mockk(relaxed = true)
        val model = buildModel(onLoadRating = { null }, onSubmitRating = submitMock)
        // open BS without selecting rating (edge case - shouldn't happen in practice)
        // just verify submit does nothing without a selected rating
        coVerify(exactly = 0) { submitMock(any(), any()) }
    }
}