package com.tangem.features.tangempay.card.name

import androidx.compose.ui.text.input.TextFieldValue
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.tangempay.TangemPayCard
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.tangempay.TangemPayCardState
import com.tangem.domain.models.tangempay.TangemPayCardType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.usecase.UpdateTangemPayCardNameUseCase
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.features.tangempay.card.details.CardDetailsEventListener
import com.tangem.features.tangempay.card.details.TangemPayCardDetailsController
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TangemPayEditDisplayNameModelTest : TangemPayModelTestBase() {

    private val cardId = "card_1"
    private val userWalletId = UserWalletId("123")

    private val updateCardNameUseCase: UpdateTangemPayCardNameUseCase = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val cardDetailsEventListener: CardDetailsEventListener = mockk(relaxed = true)
    private val cardDetailsControllerFactory: TangemPayCardDetailsController.Factory = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(
            updateCardNameUseCase,
            paymentAccountStatusSupplier,
            cardDetailsEventListener,
            cardDetailsControllerFactory,
            analytics,
            router,
            uiMessageSender,
        )
        every { paymentAccountStatusSupplier.invoke(any<UserWalletId>()) } returns emptyFlow()
    }

    private fun createModel(displayName: CardDisplayName? = null) = TangemPayEditDisplayNameModel(
        paramsContainer = MutableParamsContainer(
            TangemPayEditDisplayNameComponent.Params(
                card = card(displayName = displayName),
                userWalletId = userWalletId,
            ),
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
        router = router,
        updateCardNameUseCase = updateCardNameUseCase,
        uiMessageSender = uiMessageSender,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        cardDetailsEventListener = cardDetailsEventListener,
        cardDetailsControllerFactory = cardDetailsControllerFactory,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnValueChanged {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN input WHEN onValueChanged THEN done button enabled unless the input is blank`(
            case: DoneButtonModel,
        ) {
            // Arrange
            val model = createModel()

            // Act
            model.uiState.value.onValueChanged(TextFieldValue(case.input))

            // Assert
            assertThat(model.uiState.value.isDoneEnabled).isEqualTo(case.isDoneEnabled)
        }

        @Test
        fun `GIVEN input longer than max length WHEN onValueChanged THEN change is ignored`() {
            // Arrange
            val model = createModel()
            val maxLengthText = "a".repeat(CardDisplayName.MAX_LENGTH)
            model.uiState.value.onValueChanged(TextFieldValue(maxLengthText))

            // Act
            model.uiState.value.onValueChanged(TextFieldValue(maxLengthText + "b"))

            // Assert
            assertThat(model.uiState.value.editingValue.text).isEqualTo(maxLengthText)
        }

        private fun provideTestModels() = listOf(
            // [REDACTED_TASK_KEY]: invalid-but-present input must NOT disable the button, otherwise the
            // user can never press Done to see the "Invalid characters" alert.
            DoneButtonModel(input = "Card 😀", isDoneEnabled = true),
            DoneButtonModel(input = "Card #1!", isDoneEnabled = true),
            DoneButtonModel(input = "My Card", isDoneEnabled = true),
            DoneButtonModel(input = "   ", isDoneEnabled = false),
        )
    }

    @Nested
    inner class OnDoneClick {

        // [REDACTED_TASK_KEY]: pressing Done on an invalid name surfaces the "Invalid characters" alert
        // and does not persist the name.
        @Test
        fun `GIVEN invalid name WHEN onDoneClick THEN error dialog shown and name not updated`() {
            // Arrange
            val model = createModel()
            model.uiState.value.onValueChanged(TextFieldValue("Card 😀"))

            // Act
            model.uiState.value.onDoneClick()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            coVerify(exactly = 0) { updateCardNameUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN valid changed name WHEN onDoneClick THEN name updated`() {
            // Arrange
            coEvery { updateCardNameUseCase(any(), any(), any()) } returns Unit.right()
            val model = createModel()
            model.uiState.value.onValueChanged(TextFieldValue("New Name"))

            // Act
            model.uiState.value.onDoneClick()

            // Assert
            coVerify(exactly = 1) {
                updateCardNameUseCase(cardId, userWalletId, CardDisplayName("New Name").getOrNull()!!)
            }
            verify(exactly = 0) { uiMessageSender.send(any<DialogMessage>()) }
        }

        @Test
        fun `GIVEN unchanged name WHEN onDoneClick THEN screen closed without update`() {
            // Arrange
            val model = createModel(displayName = CardDisplayName("My Card").getOrNull())

            // Act
            model.uiState.value.onDoneClick()

            // Assert
            verify(exactly = 1) { router.pop() }
            coVerify(exactly = 0) { updateCardNameUseCase(any(), any(), any()) }
        }
    }

    private fun card(displayName: CardDisplayName? = null) = TangemPayCard(
        id = cardId,
        productInstanceId = "product",
        cardStatus = TangemPayCard.Status.ACTIVE,
        hasPinCode = true,
        displayName = displayName,
        limit = null,
        frozenState = TangemPayCardFrozenState.Unfrozen,
        lastDigits = "1234",
        images = emptyList(),
        state = TangemPayCardState.Active,
        embossName = null,
        cardType = TangemPayCardType.VIRTUAL,
    )

    internal data class DoneButtonModel(val input: String, val isDoneEnabled: Boolean)
}