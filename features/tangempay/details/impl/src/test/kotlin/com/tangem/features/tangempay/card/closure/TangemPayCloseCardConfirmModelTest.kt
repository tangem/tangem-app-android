package com.tangem.features.tangempay.card.closure

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.modal.MessageModalUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.CloseCardConfirmationPopupOpened.CardState
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.CloseCardPopupDismissed.Action
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.FreezeCardClicked.Source
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreUiR

internal class TangemPayCloseCardConfirmModelTest : TangemPayModelTestBase() {

    private val listener: CloseCardConfirmListener = mockk(relaxed = true)
    private val closeCoordinator: TangemPayCardCloseCoordinator = mockk(relaxUnitFun = true)
    private val hostScope = RecordingActionScope()

    @BeforeEach
    fun resetMocks() {
        clearMocks(listener, closeCoordinator, analytics, uiMessageSender)
        hostScope.dismissCount = 0
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Init {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN card WHEN model created THEN popup opening reported with card state`(model: InitModel) = runTest {
            // Act
            createModel(card = model.card, canFreeze = model.canFreeze)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmationPopupOpened(model.expectedCardState))
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN card WHEN model created THEN confirmation shows the card and its actions`(model: InitModel) =
            runTest {
                // Act
                val confirmModel = createModel(card = model.card, canFreeze = model.canFreeze)
                advanceUntilIdle()

                // Assert
                assertThat(confirmModel.state.value.toModalState()).isEqualTo(model.expectedModal)
            }

        private fun provideTestModels() = listOf(
            InitModel(
                card = CARD.copy(name = "Travel"),
                canFreeze = true,
                expectedCardState = CardState.Active,
                expectedModal = ModalState(
                    content = confirmContent(
                        cardTitle = "Travel ••1234",
                        description = R.string.tangempay_close_card_sheet_description_with_freeze_hint,
                    ),
                    buttons = listOf(
                        ButtonState(R.string.tangempay_close_card_sheet_confirm, TangemButton.Variant.Secondary),
                        ButtonState(R.string.tangempay_close_card_sheet_freeze_instead, TangemButton.Variant.Primary),
                    ),
                ),
            ),
            InitModel(
                card = CARD,
                canFreeze = false,
                expectedCardState = CardState.Frozen,
                expectedModal = ModalState(
                    content = confirmContent(
                        cardTitle = "••1234",
                        description = R.string.tangempay_close_card_sheet_description,
                    ),
                    buttons = listOf(
                        ButtonState(R.string.tangempay_close_card_sheet_confirm, TangemButton.Variant.Secondary),
                        ButtonState(R.string.tangempay_close_card_sheet_keep, TangemButton.Variant.Primary),
                    ),
                ),
            ),
        )
    }

    @Nested
    inner class CloseCard {

        @Test
        fun `GIVEN close request pending WHEN close card clicked THEN confirmation reported and close button loading`() =
            runTest {
                // Arrange
                val result = CompletableDeferred<Either<TangemPayApiError, Unit>>()
                coEvery { closeCoordinator.close(any()) } coAnswers { result.await() }
                val model = createModel()
                advanceUntilIdle()

                // Act
                model.clickCloseCard()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardConfirmed()) }
                coVerify(exactly = 1) { closeCoordinator.close(CARD) }
                assertThat(model.state.value.buttons.first().isLoading).isTrue()
                verify(exactly = 0) { listener.onCloseCardConfirmDismissed() }
            }

        @Test
        fun `GIVEN close accepted WHEN close card clicked THEN confirmation dismissed once`() = runTest {
            // Arrange
            coEvery { closeCoordinator.close(any()) } returns Unit.right()
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.clickCloseCard()
            advanceUntilIdle()
            model.onDismiss()

            // Assert
            verify(exactly = 1) { listener.onCloseCardConfirmDismissed() }
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.CloseCardPopupDismissed>()) }
        }

        @Test
        fun `GIVEN last card refused WHEN close card clicked THEN last card toast shown and confirmation dismissed`() =
            runTest {
                // Arrange
                coEvery { closeCoordinator.close(any()) } returns TangemPayApiError.CardCloseLastCard.left()
                val model = createModel()
                advanceUntilIdle()

                // Act
                model.clickCloseCard()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    uiMessageSender.send(
                        SnackbarMessage(
                            message = resourceReference(R.string.tangempay_card_close_last_card_disabled),
                            startIconId = CoreUiR.drawable.ic_alert_circle_24,
                        ),
                    )
                }
                verify(exactly = 1) { listener.onCloseCardConfirmDismissed() }
            }

        @Test
        fun `GIVEN close failed WHEN close card clicked THEN error shown in place and confirmation kept`() = runTest {
            // Arrange
            coEvery { closeCoordinator.close(any()) } returns TangemPayApiError.Unspecified.left()
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.clickCloseCard()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.toModalState()).isEqualTo(
                ModalState(
                    content = TangemModalContentUM.Status(
                        variant = TangemModalStatus.Variant.Error,
                        title = resourceReference(R.string.tangempay_card_close_error_title),
                        description = resourceReference(R.string.tangempay_card_close_error_message),
                        asset = TangemModalContentUM.Status.Asset.Glyph(Icons.ic_error_28),
                    ),
                    buttons = listOf(
                        ButtonState(R.string.common_contact_support, TangemButton.Variant.Secondary),
                        ButtonState(R.string.common_ok, TangemButton.Variant.Primary),
                    ),
                ),
            )
            verify(exactly = 0) { listener.onCloseCardConfirmDismissed() }
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN close error shown WHEN ok clicked THEN error dismissal reported and modal dismissed`() = runTest {
            // Arrange
            val model = createFailedCloseModel()

            // Act
            model.state.value.buttons.last().onClick(hostScope)

            // Assert
            verify(exactly = 1) { closeCoordinator.onCloseErrorDismissed() }
            verify(exactly = 0) { closeCoordinator.contactSupport(any(), any()) }
            assertThat(hostScope.dismissCount).isEqualTo(1)
        }

        @Test
        fun `GIVEN close error shown WHEN contact support clicked THEN support contacted and modal dismissed`() =
            runTest {
                // Arrange
                val model = createFailedCloseModel()

                // Act
                model.state.value.buttons.first().onClick(hostScope)

                // Assert
                verify(exactly = 1) { closeCoordinator.contactSupport(USER_WALLET_ID, CUSTOMER_ID) }
                verify(exactly = 0) { closeCoordinator.onCloseErrorDismissed() }
                assertThat(hostScope.dismissCount).isEqualTo(1)
            }

        @Test
        fun `GIVEN close request pending WHEN dismissed THEN dismissal ignored`() = runTest {
            // Arrange
            val result = CompletableDeferred<Either<TangemPayApiError, Unit>>()
            coEvery { closeCoordinator.close(any()) } coAnswers { result.await() }
            val model = createModel()
            model.clickCloseCard()
            advanceUntilIdle()

            // Act
            model.onDismiss()

            // Assert
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.CloseCardPopupDismissed>()) }
            verify(exactly = 0) { listener.onCloseCardConfirmDismissed() }
        }

        private fun TestScope.createFailedCloseModel(): TangemPayCloseCardConfirmModel {
            coEvery { closeCoordinator.close(any()) } returns TangemPayApiError.Unspecified.left()
            val model = createModel()
            model.clickCloseCard()
            advanceUntilIdle()
            return model
        }
    }

    @Test
    fun `GIVEN card can be frozen WHEN freeze instead clicked THEN freeze requested without dismissal report`() =
        runTest {
            // Arrange
            val model = createModel(canFreeze = true)
            advanceUntilIdle()

            // Act
            model.state.value.buttons.last().onClick(hostScope)
            model.onDismiss()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.FreezeCardClicked(Source.ClosePopup)) }
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.CloseCardPopupDismissed>()) }
            verify(exactly = 1) { listener.onFreezeInsteadOfClose() }
            verify(exactly = 0) { listener.onCloseCardConfirmDismissed() }
        }

    @Test
    fun `GIVEN frozen card WHEN keep card clicked THEN keep card reported once and confirmation dismissed once`() =
        runTest {
            // Arrange
            val model = createModel(canFreeze = false)
            advanceUntilIdle()

            // Act
            model.state.value.buttons.last().onClick(hostScope)
            model.onDismiss()

            // Assert
            verify(exactly = 1) {
                analytics.send(TangemPayAnalyticsEvents.CloseCardPopupDismissed(Action.KeepCard))
            }
            verify(exactly = 0) {
                analytics.send(TangemPayAnalyticsEvents.CloseCardPopupDismissed(Action.CloseButton))
            }
            verify(exactly = 1) { listener.onCloseCardConfirmDismissed() }
            assertThat(hostScope.dismissCount).isEqualTo(1)
        }

    @Test
    fun `GIVEN no action taken WHEN dismissed twice THEN close button dismissal reported once`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        repeat(times = 2) { model.onDismiss() }

        // Assert
        verify(exactly = 1) {
            analytics.send(TangemPayAnalyticsEvents.CloseCardPopupDismissed(Action.CloseButton))
        }
        verify(exactly = 1) { listener.onCloseCardConfirmDismissed() }
    }

    private fun TestScope.createModel(
        card: TangemPayClosingCard = CARD,
        canFreeze: Boolean = true,
    ): TangemPayCloseCardConfirmModel = TangemPayCloseCardConfirmModel(
        paramsContainer = MutableParamsContainer(
            TangemPayCloseCardConfirmComponent.Params(listener = listener, card = card, canFreeze = canFreeze),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        closeCoordinator = closeCoordinator,
        uiMessageSender = uiMessageSender,
        analytics = analytics,
    ).destroyAfterTest()

    private fun TangemPayCloseCardConfirmModel.clickCloseCard() {
        state.value.buttons.first().onClick(hostScope)
    }

    private fun MessageModalUM.toModalState(): ModalState = ModalState(
        content = content,
        buttons = buttons.map { ButtonState(text = it.text, variant = it.variant, isLoading = it.isLoading) },
    )

    private fun confirmContent(cardTitle: String, description: Int) = TangemModalContentUM.Status(
        variant = TangemModalStatus.Variant.Error,
        title = resourceReference(R.string.tangempay_close_card_sheet_title, wrappedList(cardTitle)),
        description = resourceReference(description),
        asset = TangemModalContentUM.Status.Asset.Glyph(Icons.ic_error_28),
    )

    internal data class InitModel(
        val card: TangemPayClosingCard,
        val canFreeze: Boolean,
        val expectedCardState: CardState,
        val expectedModal: ModalState,
    )

    internal data class ModalState(val content: TangemModalContentUM, val buttons: List<ButtonState>)

    internal data class ButtonState(
        val text: TextReference?,
        val variant: TangemButton.Variant,
        val isLoading: Boolean = false,
    ) {
        constructor(text: Int, variant: TangemButton.Variant) : this(resourceReference(text), variant)
    }

    internal class RecordingActionScope : TangemModalActionScope {
        var dismissCount = 0

        override fun dismiss() {
            dismissCount++
        }
    }

    private companion object {
        const val CUSTOMER_ID = "customer_1"
        val USER_WALLET_ID = UserWalletId("123")

        val CARD = TangemPayClosingCard(
            userWalletId = USER_WALLET_ID,
            cardId = "card_1",
            customerId = CUSTOMER_ID,
            name = null,
            lastDigits = "1234",
        )
    }
}