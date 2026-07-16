package com.tangem.features.tangempay.closure

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.CloseTangemPayCardUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TangemPayCloseCardModelTest {

    private val cardId = "test_card_id"
    private val userWalletId = UserWalletId("123")

    private val listener: CloseCardListener = mockk(relaxed = true)
    private val closeCardUseCase: CloseTangemPayCardUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

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
        createModel()

        verify(exactly = 1) {
            analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmationPopupOpened())
        }
    }

    @Test
    fun `GIVEN not closing WHEN onDismiss THEN listener notified and use case not called`() {
        val model = createModel()

        model.state.value.onDismissRequest()

        verify(exactly = 1) { listener.onDismissCloseCard() }
        coVerify(exactly = 0) { closeCardUseCase(any(), any()) }
    }

    @Nested
    inner class OnConfirm {

        @Test
        fun `GIVEN close succeeds WHEN onCloseClick THEN use case invoked and dialog dismissed`() {
            coEvery { closeCardUseCase(userWalletId, cardId) } returns Unit.right()
            val model = createModel()

            model.state.value.onCloseClick()

            coVerify(exactly = 1) { closeCardUseCase(userWalletId, cardId) }
            verify(exactly = 1) { listener.onDismissCloseCard() }
        }

        @Test
        fun `GIVEN close succeeds WHEN onCloseClick THEN CloseCardConfirmed is sent`() {
            coEvery { closeCardUseCase(userWalletId, cardId) } returns Unit.right()
            val model = createModel()

            model.state.value.onCloseClick()

            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmed()) }
        }

        @Test
        fun `GIVEN close fails WHEN onCloseClick THEN snackbar shown and dialog dismissed`() {
            coEvery { closeCardUseCase(userWalletId, cardId) } returns VisaApiError.Unspecified.left()
            val model = createModel()

            model.state.value.onCloseClick()

            verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
            verify(exactly = 1) { listener.onDismissCloseCard() }
        }

        @Test
        fun `GIVEN close fails WHEN onCloseClick THEN progress is reset`() {
            coEvery { closeCardUseCase(userWalletId, cardId) } returns VisaApiError.Unspecified.left()
            val model = createModel()

            model.state.value.onCloseClick()

            assertThat(model.state.value.isClosingInProgress).isFalse()
        }
    }
}