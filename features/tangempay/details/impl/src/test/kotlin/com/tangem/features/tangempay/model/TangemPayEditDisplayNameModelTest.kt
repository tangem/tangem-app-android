package com.tangem.features.tangempay.model

import androidx.compose.ui.text.input.TextFieldValue
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.UpdateTangemPayCardNameUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.TangemPayEditDisplayNameComponent
import com.tangem.features.tangempay.model.controller.TangemPayCardDetailsController
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TangemPayEditDisplayNameModelTest {

    private val cardId = "card_1"
    private val userWalletId = UserWalletId("123")

    private val router: Router = mockk(relaxed = true)
    private val updateCardNameUseCase: UpdateTangemPayCardNameUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val featureToggles: TangemPayFeatureToggles = mockk(relaxed = true)
    private val cardDetailsControllerFactory: TangemPayCardDetailsController.Factory = mockk(relaxed = true)

    init {
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
        featureToggles = featureToggles,
        cardDetailsControllerFactory = cardDetailsControllerFactory,
    )

    @Nested
    inner class OnValueChanged {

        // [REDACTED_TASK_KEY]: invalid-but-present input must NOT disable the button, otherwise the
        // user can never press Done to see the "Invalid characters" alert.
        @Test
        fun `GIVEN emoji input WHEN onValueChanged THEN button stays enabled`() {
            val model = createModel()

            model.uiState.value.onValueChanged(TextFieldValue("Card 😀"))

            assertThat(model.uiState.value.isDoneEnabled).isTrue()
        }

        @Test
        fun `GIVEN special characters input WHEN onValueChanged THEN button stays enabled`() {
            val model = createModel()

            model.uiState.value.onValueChanged(TextFieldValue("Card #1!"))

            assertThat(model.uiState.value.isDoneEnabled).isTrue()
        }

        @Test
        fun `GIVEN valid input WHEN onValueChanged THEN button enabled`() {
            val model = createModel()

            model.uiState.value.onValueChanged(TextFieldValue("My Card"))

            assertThat(model.uiState.value.isDoneEnabled).isTrue()
        }

        @Test
        fun `GIVEN blank input WHEN onValueChanged THEN button disabled`() {
            val model = createModel()

            model.uiState.value.onValueChanged(TextFieldValue("   "))

            assertThat(model.uiState.value.isDoneEnabled).isFalse()
        }

        @Test
        fun `GIVEN input longer than max length WHEN onValueChanged THEN change is ignored`() {
            val model = createModel()
            val maxLengthText = "a".repeat(CardDisplayName.MAX_LENGTH)
            model.uiState.value.onValueChanged(TextFieldValue(maxLengthText))

            model.uiState.value.onValueChanged(TextFieldValue(maxLengthText + "b"))

            assertThat(model.uiState.value.editingValue.text).isEqualTo(maxLengthText)
        }
    }

    @Nested
    inner class OnDoneClick {

        // [REDACTED_TASK_KEY]: pressing Done on an invalid name surfaces the "Invalid characters" alert
        // and does not persist the name.
        @Test
        fun `GIVEN invalid name WHEN onDoneClick THEN error dialog shown and name not updated`() {
            val model = createModel()
            model.uiState.value.onValueChanged(TextFieldValue("Card 😀"))

            model.uiState.value.onDoneClick()

            verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            coVerify(exactly = 0) { updateCardNameUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN valid changed name WHEN onDoneClick THEN name updated`() {
            coEvery { updateCardNameUseCase(any(), any(), any()) } returns Unit.right()
            val model = createModel()
            model.uiState.value.onValueChanged(TextFieldValue("New Name"))

            model.uiState.value.onDoneClick()

            coVerify(exactly = 1) {
                updateCardNameUseCase(cardId, userWalletId, CardDisplayName("New Name").getOrNull()!!)
            }
            verify(exactly = 0) { uiMessageSender.send(any<DialogMessage>()) }
        }

        @Test
        fun `GIVEN unchanged name WHEN onDoneClick THEN screen closed without update`() {
            val model = createModel(displayName = CardDisplayName("My Card").getOrNull())

            model.uiState.value.onDoneClick()

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
        state = TangemPayCardState.Active,
    )
}