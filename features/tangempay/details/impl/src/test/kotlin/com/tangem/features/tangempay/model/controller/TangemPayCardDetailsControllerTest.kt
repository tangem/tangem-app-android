package com.tangem.features.tangempay.model.controller

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.error.UniversalError
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.entity.CardDataType
import com.tangem.features.tangempay.model.listener.CardDetailsEvent
import com.tangem.features.tangempay.model.listener.DefaultCardDetailsEventListener
import com.tangem.utils.StringsSigns
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private const val SHOW_DETAILS_TIME = 30_000L

internal class TangemPayCardDetailsControllerTest {

    private val userWalletId = UserWalletId("123")
    private val cardId = "card_1"
    private val otherCardId = "card_2"

    private val repository: TangemPayCardDetailsRepository = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val supplier: PaymentAccountStatusSupplier = mockk()
    private val supplierFlow = MutableSharedFlow<AccountStatus.Payment>(replay = 1)
    private val eventListener = DefaultCardDetailsEventListener()

    private val details = TangemPayCardDetails(
        pan = "4242424242424242",
        cvv = "123",
        expirationYear = "2030",
        expirationMonth = "9",
    )

    init {
        every { supplier.invoke(userWalletId) } returns supplierFlow
        every { repository.cardFrozenState(any()) } returns emptyFlow()
    }

    @Test
    fun `GIVEN card WHEN created THEN initial state is hidden with masked digits`() = runTest {
        val controller = createController(scope = backgroundScope, card = card(lastDigits = "9999"))
        runCurrent()

        val state = controller.uiState.value
        assertThat(state.isHidden).isTrue()
        assertThat(state.numberShort).isEqualTo("${StringsSigns.ASTERISK}9999")
    }

    @Test
    fun `GIVEN Show for this card WHEN event received THEN details revealed and analytics sent`() = runTest {
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()

        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.ViewCardDetailsClicked()) }
        coVerify(exactly = 1) { repository.revealCardDetails(userWalletId, cardId) }
        val state = controller.uiState.value
        assertThat(state.isHidden).isFalse()
        assertThat(state.cvv).isEqualTo("123")
        assertThat(state.expiry).isEqualTo("09/30")
    }

    @Test
    fun `GIVEN reveal fails WHEN event received THEN snackbar shown and state hidden`() = runTest {
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns ERROR.left()
        val controller = createController(scope = backgroundScope)
        runCurrent()

        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
        assertThat(controller.uiState.value.isHidden).isTrue()
    }

    @Test
    fun `GIVEN revealed WHEN Show for another card THEN this card hides`() = runTest {
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        eventListener.send(CardDetailsEvent.Show(otherCardId))
        runCurrent()

        assertThat(controller.uiState.value.isHidden).isTrue()
    }

    @Test
    fun `GIVEN revealed WHEN Hide for another card THEN this card stays revealed`() = runTest {
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        eventListener.send(CardDetailsEvent.Hide(otherCardId))
        runCurrent()

        assertThat(controller.uiState.value.isHidden).isFalse()
    }

    @Test
    fun `GIVEN revealed WHEN HideAll THEN this card hides`() = runTest {
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        eventListener.send(CardDetailsEvent.HideAll)
        runCurrent()

        assertThat(controller.uiState.value.isHidden).isTrue()
    }

    @Test
    fun `GIVEN revealed WHEN show timer elapses THEN details auto-hide`() = runTest {
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        advanceTimeBy(SHOW_DETAILS_TIME + 1)
        runCurrent()

        assertThat(controller.uiState.value.isHidden).isTrue()
    }

    @Test
    fun `GIVEN supplier emits actual loaded WHEN card present THEN frozen state and digits updated`() = runTest {
        val updatedCard = card(lastDigits = "5678", frozenState = TangemPayCardFrozenState.Frozen)
        val loaded = mockk<PaymentAccountStatusValue.Loaded> {
            every { source } returns StatusSource.ACTUAL
            every { cards } returns listOf(updatedCard)
        }
        val payment = mockk<AccountStatus.Payment> {
            every { value } returns loaded
        }
        val controller = createController(scope = backgroundScope)
        runCurrent()

        supplierFlow.emit(payment)
        runCurrent()

        val state = controller.uiState.value
        assertThat(state.numberShort).isEqualTo("${StringsSigns.ASTERISK}5678")
        assertThat(state.cardFrozenState).isEqualTo(TangemPayCardFrozenState.Frozen)
        assertThat(state.isActionsAvailable).isTrue()
    }

    @Test
    fun `GIVEN copy cvv WHEN onCopy invoked THEN clipboard set as sensitive and analytics sent`() = runTest {
        val controller = createController(scope = backgroundScope)
        runCurrent()

        controller.uiState.value.onCopy("1 2 3", CardDataType.CVV)

        verify(exactly = 1) { clipboardManager.setText(text = "123", isSensitive = true) }
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CopyCardCVVClicked()) }
    }

    @Test
    fun `GIVEN disposed WHEN Show received THEN reveal not triggered`() = runTest {
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()

        controller.dispose()
        eventListener.send(CardDetailsEvent.Show(cardId))
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.revealCardDetails(any(), any()) }
    }

    private fun TestScope.createController(
        scope: CoroutineScope,
        card: TangemPayCard = card(),
        config: TangemPayCardDetailsController.Config = TangemPayCardDetailsController.Config(
            isEditingNameEnabled = true,
            shouldShowCardDetailsButtonOnCard = false,
        ),
        onEditNameClick: () -> Unit = {},
    ): TangemPayCardDetailsController = TangemPayCardDetailsController(
        scope = scope,
        initialCard = card,
        userWalletId = userWalletId,
        config = config,
        onEditNameClick = onEditNameClick,
        cardDetailsRepository = repository,
        clipboardManager = clipboardManager,
        uiMessageSender = uiMessageSender,
        cardDetailsEventListener = eventListener,
        analytics = analytics,
        paymentAccountStatusSupplier = supplier,
    )

    private fun card(
        id: String = cardId,
        lastDigits: String = "1234",
        displayName: CardDisplayName? = null,
        frozenState: TangemPayCardFrozenState = TangemPayCardFrozenState.Unfrozen,
        state: TangemPayCardState = TangemPayCardState.Active,
    ): TangemPayCard = TangemPayCard(
        id = id,
        productInstanceId = "product_$id",
        cardStatus = TangemPayCard.Status.ACTIVE,
        hasPinCode = true,
        displayName = displayName,
        limit = null,
        frozenState = frozenState,
        lastDigits = lastDigits,
        state = state,
    )

    private companion object {
        val ERROR = object : UniversalError {
            override val errorCode: Int = 0
        }
    }
}