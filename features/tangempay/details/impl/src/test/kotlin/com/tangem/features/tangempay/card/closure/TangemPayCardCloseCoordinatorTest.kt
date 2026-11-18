package com.tangem.features.tangempay.card.closure

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.ModalMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.modal.MessageModalUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.CloseCardResult.Result
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.usecase.CloseTangemPayCardUseCase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreUiR

internal class TangemPayCardCloseCoordinatorTest {

    private val closeTangemPayCardUseCase: CloseTangemPayCardUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private val coordinator = TangemPayCardCloseCoordinator(
        closeTangemPayCardUseCase = closeTangemPayCardUseCase,
        sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
        analytics = analytics,
        uiMessageSender = uiMessageSender,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(closeTangemPayCardUseCase, sendFeedbackEmailUseCase, analytics, uiMessageSender)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Close {

        @Test
        fun `GIVEN close accepted WHEN close THEN use case called for the card and no result reported yet`() = runTest {
            // Arrange
            coEvery { closeTangemPayCardUseCase(any(), any(), any()) } returns Unit.right()

            // Act
            val actual = coordinator.close(CARD)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) { closeTangemPayCardUseCase(USER_WALLET_ID, CARD_ID, any()) }
            verify(exactly = 0) { analytics.send(any()) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN close refused WHEN close THEN error returned and result reported`(model: CloseRefusedModel) =
            runTest {
                // Arrange
                coEvery { closeTangemPayCardUseCase(any(), any(), any()) } returns model.error.left()

                // Act
                val actual = coordinator.close(CARD)

                // Assert
                assertThat(actual).isEqualTo(model.error.left())
                verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardResult(model.expectedResult)) }
            }

        private fun provideTestModels() = listOf(
            CloseRefusedModel(error = TangemPayApiError.CardCloseLastCard, expectedResult = Result.LastCard),
            CloseRefusedModel(error = TangemPayApiError.Unspecified, expectedResult = Result.Error),
            CloseRefusedModel(error = TangemPayApiError.ServerUnavailable, expectedResult = Result.Error),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OrderEnded {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN close order completed WHEN order ends THEN success reported, card emitted and toast shown`(
            model: ClosedToastModel,
        ) = runTest {
            // Arrange
            val closedCards = getEmittedValues(coordinator.closedCards)
            val onOrderEnded = closeAndCaptureOrderEnded(model.card)

            // Act
            onOrderEnded(true)

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardResult(Result.Success)) }
            assertThat(closedCards).containsExactly(TangemPayClosedCard(USER_WALLET_ID, CARD_ID))
            verify(exactly = 1) {
                uiMessageSender.send(
                    SnackbarMessage(message = model.expectedMessage, startIconId = CoreUiR.drawable.ic_success_20),
                )
            }
        }

        private fun provideTestModels() = listOf(
            ClosedToastModel(
                card = CARD.copy(name = "Travel"),
                expectedMessage = resourceReference(R.string.tangempay_card_closed_toast_named, wrappedList("Travel")),
            ),
            ClosedToastModel(
                card = CARD.copy(name = null),
                expectedMessage = resourceReference(R.string.tangempay_card_closed_toast, wrappedList(LAST_DIGITS)),
            ),
        )

        @Test
        fun `GIVEN close order failed WHEN order ends THEN error reported, error modal shown and no card emitted`() =
            runTest {
                // Arrange
                val closedCards = getEmittedValues(coordinator.closedCards)
                val onOrderEnded = closeAndCaptureOrderEnded(CARD)

                // Act
                onOrderEnded(false)

                // Assert
                verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardResult(Result.Error)) }
                assertThat(sentErrorModal().content).isEqualTo(CLOSE_ERROR_CONTENT)
                assertThat(closedCards).isEmpty()
            }

        @Test
        fun `GIVEN close error modal WHEN ok clicked THEN error dismissal reported and modal dismissed`() = runTest {
            // Arrange
            val onOrderEnded = closeAndCaptureOrderEnded(CARD)
            onOrderEnded(false)
            val dismissScope = RecordingActionScope()

            // Act
            sentErrorModal().buttons.last().onClick(dismissScope)

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardErrorDismissed()) }
            assertThat(dismissScope.dismissCount).isEqualTo(1)
        }

        @Test
        fun `GIVEN close error modal WHEN contact support clicked THEN support email opened and modal dismissed`() =
            runTest {
                // Arrange
                val onOrderEnded = closeAndCaptureOrderEnded(CARD)
                onOrderEnded(false)
                val dismissScope = RecordingActionScope()

                // Act
                sentErrorModal().buttons.first().onClick(dismissScope)

                // Assert
                coVerify(exactly = 1) { sendFeedbackEmailUseCase(FEATURE_IS_BETA_EMAIL) }
                assertThat(dismissScope.dismissCount).isEqualTo(1)
            }

        private suspend fun closeAndCaptureOrderEnded(card: TangemPayClosingCard): suspend (Boolean) -> Unit {
            val onOrderEnded = slot<suspend (Boolean) -> Unit>()
            coEvery { closeTangemPayCardUseCase(any(), any(), capture(onOrderEnded)) } returns Unit.right()
            coordinator.close(card)
            return onOrderEnded.captured
        }

        private fun sentErrorModal(): MessageModalUM {
            val message = slot<ModalMessage>()
            verify(exactly = 1) { uiMessageSender.send(capture(message)) }
            return message.captured.messageModalUM
        }
    }

    @Test
    fun `GIVEN close error shown WHEN onCloseErrorDismissed THEN error dismissal reported`() {
        // Act
        coordinator.onCloseErrorDismissed()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardErrorDismissed()) }
    }

    @Nested
    inner class ContactSupport {

        @Test
        fun `GIVEN customer id WHEN contactSupport THEN support click reported and feedback email sent`() {
            // Act
            coordinator.contactSupport(userWalletId = USER_WALLET_ID, customerId = CUSTOMER_ID)

            // Assert
            verify(exactly = 1) { analytics.send(Basic.ButtonSupport(AnalyticsParam.ScreensSources.TangemPay)) }
            coVerify(exactly = 1) { sendFeedbackEmailUseCase(FEATURE_IS_BETA_EMAIL) }
        }

        @Test
        fun `GIVEN no customer id WHEN contactSupport THEN support click reported and no email sent`() {
            // Act
            coordinator.contactSupport(userWalletId = USER_WALLET_ID, customerId = null)

            // Assert
            verify(exactly = 1) { analytics.send(Basic.ButtonSupport(AnalyticsParam.ScreensSources.TangemPay)) }
            coVerify(exactly = 0) { sendFeedbackEmailUseCase(any()) }
        }
    }

    internal data class CloseRefusedModel(val error: TangemPayApiError, val expectedResult: Result)

    internal data class ClosedToastModel(val card: TangemPayClosingCard, val expectedMessage: TextReference)

    private class RecordingActionScope : TangemModalActionScope {
        var dismissCount = 0
            private set

        override fun dismiss() {
            dismissCount++
        }
    }

    private companion object {
        const val CARD_ID = "card_1"
        const val CUSTOMER_ID = "customer_1"
        const val LAST_DIGITS = "1234"
        val USER_WALLET_ID = UserWalletId("123")

        val CARD = TangemPayClosingCard(
            userWalletId = USER_WALLET_ID,
            cardId = CARD_ID,
            customerId = CUSTOMER_ID,
            name = null,
            lastDigits = LAST_DIGITS,
        )

        val FEATURE_IS_BETA_EMAIL = FeedbackEmailType.TangemPay.FeatureIsBeta(
            walletMetaInfo = WalletMetaInfo(userWalletId = USER_WALLET_ID),
            customerId = CUSTOMER_ID,
        )

        val CLOSE_ERROR_CONTENT = TangemModalContentUM.Status(
            variant = TangemModalStatus.Variant.Error,
            title = resourceReference(R.string.tangempay_card_close_error_title),
            description = resourceReference(R.string.tangempay_card_close_error_message),
            asset = TangemModalContentUM.Status.Asset.Glyph(Icons.ic_error_28),
        )
    }
}