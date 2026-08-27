package com.tangem.feature.rating.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.rating.ui.RatingFeedbackBS
import com.tangem.feature.rating.ui.RatingUM
import com.tangem.feature.swap.domain.SwapFeedbackUseCase
import com.tangem.feature.swap.domain.models.domain.SwapRating
import com.tangem.features.rating.RatingComponent
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

internal class RatingModelTest {

    private val swapFeedbackUseCase: SwapFeedbackUseCase = mockk()

    /** Emulates the repository cache for [TX_EXTERNAL_ID]; null = not loaded / no entry */
    private val cacheFlow = MutableStateFlow<SwapRating?>(null)

    private val appCoroutineScope = object : AppCoroutineScope {
        override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
    }

    @BeforeEach
    fun reset() {
        clearMocks(swapFeedbackUseCase)
        cacheFlow.value = null
    }

    private fun buildModel(
        onEnsureLoaded: suspend () -> Unit = { cacheFlow.value = SwapRating.NotRated },
        onSubmit: suspend (SwapFeedbackUseCase.SubmitParams) -> Unit = {
            cacheFlow.value = SwapRating.Rated(it.rating)
        },
    ): RatingModel {
        every { swapFeedbackUseCase.observeRating(TX_EXTERNAL_ID) } returns cacheFlow
        coEvery { swapFeedbackUseCase.ensureLoaded(TX_EXTERNAL_ID) } coAnswers { onEnsureLoaded() }
        coEvery { swapFeedbackUseCase.submit(any()) } coAnswers {
            onSubmit(firstArg())
            Unit.right()
        }
        return RatingModel(
            dispatchers = TestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(params()),
            swapFeedbackUseCase = swapFeedbackUseCase,
            appCoroutineScope = appCoroutineScope,
        )
    }

    private fun params() = RatingComponent.Params(
        txExternalId = TX_EXTERNAL_ID,
        providerName = PROVIDER_NAME,
        txExternalUrl = TX_EXTERNAL_URL,
        userWalletId = UserWalletId(USER_WALLET_ID_HEX),
    )

    private val RatingModel.ratingState get() = state.value.state
    private val RatingModel.feedbackContent get() = state.value.feedbackBottomSheet.content as? RatingFeedbackBS

    @Test
    fun `GIVEN load in flight WHEN model created THEN state is Loading until load completes`() = runTest {
        // Arrange
        val deferred = CompletableDeferred<Unit>()

        // Act
        val model = buildModel(onEnsureLoaded = { deferred.await() })

        // Assert
        assertThat(model.ratingState).isInstanceOf(RatingUM.RatingState.Loading::class.java)
        deferred.complete(Unit)
        assertThat(model.ratingState).isInstanceOf(RatingUM.RatingState.Unrated::class.java)
    }

    @Test
    fun `GIVEN no rating stored WHEN model created THEN state is Unrated with no selection`() = runTest {
        // Act
        val model = buildModel()

        // Assert
        val unrated = model.ratingState as RatingUM.RatingState.Unrated
        assertThat(unrated.selectedRating).isNull()
        assertThat(model.state.value.feedbackBottomSheet.isShown).isFalse()
    }

    @Test
    fun `GIVEN load failed and nothing cached WHEN load finishes THEN state is Unrated`() = runTest {
        // Act — ensureLoaded completes without writing to the cache (load error is not cached)
        val model = buildModel(onEnsureLoaded = {})

        // Assert
        assertThat(model.ratingState).isEqualTo(RatingUM.RatingState.Unrated(selectedRating = null))
    }

    @Test
    fun `GIVEN rating stored WHEN model created THEN state is AlreadyRated`() = runTest {
        // Act
        val model = buildModel(onEnsureLoaded = { cacheFlow.value = SwapRating.Rated(rating = 4) })

        // Assert
        assertThat(model.ratingState).isEqualTo(RatingUM.RatingState.AlreadyRated(rating = 4))
    }

    @Test
    fun `GIVEN Unrated WHEN onRatingSelected THEN selection updated and feedback bottom sheet shown`() = runTest {
        // Arrange
        val model = buildModel()

        // Act
        model.onRatingSelected(3)

        // Assert
        val unrated = model.ratingState as RatingUM.RatingState.Unrated
        assertThat(unrated.selectedRating).isEqualTo(3)
        assertThat(model.state.value.feedbackBottomSheet.isShown).isTrue()
    }

    @Test
    fun `GIVEN AlreadyRated WHEN onRatingSelected THEN state unchanged`() = runTest {
        // Arrange
        val model = buildModel(onEnsureLoaded = { cacheFlow.value = SwapRating.Rated(rating = 4) })

        // Act
        model.onRatingSelected(3)

        // Assert
        assertThat(model.ratingState).isEqualTo(RatingUM.RatingState.AlreadyRated(rating = 4))
        assertThat(model.state.value.feedbackBottomSheet.isShown).isFalse()
    }

    @Test
    fun `GIVEN feedback bottom sheet shown WHEN onFeedbackChanged THEN feedbackText updated`() = runTest {
        // Arrange
        val model = buildModel()
        model.onRatingSelected(4)

        // Act
        model.feedbackContent!!.onFeedbackChanged("Great service!")

        // Assert
        assertThat(model.feedbackContent!!.feedbackText).isEqualTo("Great service!")
    }

    @Test
    fun `GIVEN rating selected WHEN onSubmit THEN use case called with deal data`() = runTest {
        // Arrange
        val model = buildModel()
        model.onRatingSelected(5)
        model.feedbackContent!!.onFeedbackChanged("Excellent!")

        // Act
        model.feedbackContent!!.onSubmit()

        // Assert
        coVerify(exactly = 1) {
            swapFeedbackUseCase.submit(
                SwapFeedbackUseCase.SubmitParams(
                    txExternalId = TX_EXTERNAL_ID,
                    providerName = PROVIDER_NAME,
                    txExternalUrl = TX_EXTERNAL_URL,
                    userWalletId = UserWalletId(USER_WALLET_ID_HEX),
                    rating = 5,
                    feedback = "Excellent!",
                ),
            )
        }
    }

    @Test
    fun `GIVEN rating selected WHEN onSubmit THEN state is AlreadyRated and bottom sheet hidden`() = runTest {
        // Arrange
        val model = buildModel()
        model.onRatingSelected(4)

        // Act
        model.feedbackContent!!.onSubmit()

        // Assert
        assertThat(model.ratingState).isEqualTo(RatingUM.RatingState.AlreadyRated(rating = 4))
        assertThat(model.state.value.feedbackBottomSheet.isShown).isFalse()
    }

    @Test
    fun `GIVEN submitted rating WHEN repository rolls the entry back THEN state returns to Unrated`() = runTest {
        // Arrange
        val model = buildModel()
        model.onRatingSelected(4)
        model.feedbackContent!!.onSubmit()

        // Act — POST failed, the repository removed the optimistic entry
        cacheFlow.value = null

        // Assert
        assertThat(model.ratingState).isEqualTo(RatingUM.RatingState.Unrated(selectedRating = null))
    }

    @Test
    fun `GIVEN POST in flight WHEN model destroyed THEN POST completes anyway`() = runTest {
        // Arrange
        val deferred = CompletableDeferred<Unit>()
        var delivered = false
        val model = buildModel(
            onSubmit = {
                deferred.await()
                delivered = true
            },
        )
        model.onRatingSelected(5)

        // Act — the sheet is closed while the POST is still running
        model.feedbackContent!!.onSubmit()
        model.onDestroy()
        deferred.complete(Unit)

        // Assert
        assertThat(delivered).isTrue()
    }

    @Test
    fun `GIVEN no rating selected WHEN onSubmit THEN use case not called`() = runTest {
        // Arrange
        buildModel()

        // Assert
        coVerify(exactly = 0) { swapFeedbackUseCase.submit(any()) }
    }

    private companion object {
        const val TX_EXTERNAL_ID = "tx-external-id"
        const val PROVIDER_NAME = "ChangeNow"
        const val TX_EXTERNAL_URL = "https://provider.example/tx"
        const val USER_WALLET_ID_HEX = "0011223344556677"
    }
}