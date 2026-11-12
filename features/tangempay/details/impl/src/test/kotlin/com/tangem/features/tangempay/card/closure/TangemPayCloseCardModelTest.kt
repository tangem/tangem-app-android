package com.tangem.features.tangempay.card.closure

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.usecase.CloseTangemPayCardUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TangemPayCloseCardModelTest : TangemPayModelTestBase() {

    private val cardId = "test_card_id"
    private val userWalletId = UserWalletId("123")

    private val listener: CloseCardListener = mockk(relaxed = true)
    private val closeCardUseCase: CloseTangemPayCardUseCase = mockk()

    private val params = TangemPayCloseCardComponent.Params(
        listener = listener,
        userWalletId = userWalletId,
        cardId = cardId,
    )

    private fun createModel() = TangemPayCloseCardModel(
        paramsContainer = MutableParamsContainer(params),
        dispatchers = TestingCoroutineDispatcherProvider(),
        closeTangemPayCardUseCase = closeCardUseCase,
        uiMessageSender = uiMessageSender,
        analytics = analytics,
    )

    @Test
    fun `GIVEN model WHEN created THEN CloseCardConfirmationPopupOpened is sent`() {
        // Act
        createModel()

        // Assert
        verify(exactly = 1) {
            analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmationPopupOpened())
        }
    }

    @Test
    fun `GIVEN not closing WHEN onDismiss THEN listener notified and use case not called`() {
        // Arrange
        val model = createModel()

        // Act
        model.state.value.onDismissRequest()

        // Assert
        verify(exactly = 1) { listener.onDismissCloseCard() }
        coVerify(exactly = 0) { closeCardUseCase(any(), any()) }
    }

    @Nested
    inner class OnConfirm {

        @Test
        fun `GIVEN close succeeds WHEN onCloseClick THEN use case invoked and dialog dismissed`() {
            // Arrange
            coEvery { closeCardUseCase(userWalletId, cardId) } returns Unit.right()
            val model = createModel()

            // Act
            model.state.value.onCloseClick()

            // Assert
            coVerify(exactly = 1) { closeCardUseCase(userWalletId, cardId) }
            verify(exactly = 1) { listener.onDismissCloseCard() }
        }

        @Test
        fun `GIVEN close succeeds WHEN onCloseClick THEN CloseCardConfirmed is sent`() {
            // Arrange
            coEvery { closeCardUseCase(userWalletId, cardId) } returns Unit.right()
            val model = createModel()

            // Act
            model.state.value.onCloseClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmed()) }
        }

        @Test
        fun `GIVEN close fails WHEN onCloseClick THEN snackbar shown and dialog dismissed`() {
            // Arrange
            coEvery { closeCardUseCase(userWalletId, cardId) } returns TangemPayApiError.Unspecified.left()
            val model = createModel()

            // Act
            model.state.value.onCloseClick()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
            verify(exactly = 1) { listener.onDismissCloseCard() }
        }

        @Test
        fun `GIVEN close fails WHEN onCloseClick THEN progress is reset`() {
            // Arrange
            coEvery { closeCardUseCase(userWalletId, cardId) } returns TangemPayApiError.Unspecified.left()
            val model = createModel()

            // Act
            model.state.value.onCloseClick()

            // Assert
            assertThat(model.state.value.isClosingInProgress).isFalse()
        }
    }
}