package com.tangem.features.tangempay.card.details

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.error.UniversalError
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.TangemPayCard
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.tangempay.TangemPayCardState
import com.tangem.domain.models.tangempay.TangemPayCardType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.model.TangemPayCardDetails
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.card.view.CardDataType
import com.tangem.features.tangempay.details.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

private const val SHOW_DETAILS_TIME = 40_000L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCardDetailsControllerTest {

    private val userWalletId = UserWalletId("123")
    private val cardId = "card_1"
    private val otherCardId = "card_2"

    private val repository: TangemPayCardDetailsRepository = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val supplier: PaymentAccountStatusSupplier = mockk()
    private lateinit var supplierFlow: MutableSharedFlow<AccountStatus.Payment>
    private lateinit var eventListener: DefaultCardDetailsEventListener
    private val cardAuthenticator: TangemPayCardAuthenticator = mockk()

    private val details = TangemPayCardDetails(
        pan = "4242424242424242",
        cvv = "123",
        expirationYear = "2030",
        expirationMonth = "9",
    )

    @BeforeEach
    fun setUp() {
        clearMocks(repository, clipboardManager, uiMessageSender, analytics, supplier, cardAuthenticator)
        supplierFlow = MutableSharedFlow(replay = 1)
        eventListener = DefaultCardDetailsEventListener()
        every { supplier.invoke(userWalletId) } returns supplierFlow
        every { repository.cardFrozenState(any()) } returns emptyFlow()
    }

    @Test
    fun `GIVEN user authenticated WHEN reveal requested THEN details revealed`() = runTest {
        // Arrange
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        coEvery { cardAuthenticator.authenticate(any()) } returns true
        val controller = createController(scope = backgroundScope)
        runCurrent()

        // Act
        controller.requestReveal()
        runCurrent()

        // Assert
        coVerify(exactly = 1) {
            cardAuthenticator.authenticate(title = TextReference.Res(R.string.tangempay_card_details_title))
        }
        coVerify(exactly = 1) { repository.revealCardDetails(userWalletId, cardId) }
        assertThat(controller.uiState.value.isHidden).isFalse()
    }

    @Test
    fun `GIVEN user not authenticated WHEN reveal requested THEN details stay hidden`() = runTest {
        // Arrange
        coEvery { cardAuthenticator.authenticate(any()) } returns false
        val controller = createController(scope = backgroundScope)
        runCurrent()

        // Act
        controller.requestReveal()
        runCurrent()

        // Assert
        coVerify(exactly = 0) { repository.revealCardDetails(any(), any()) }
        assertThat(controller.uiState.value.isHidden).isTrue()
    }

    @Test
    fun `GIVEN authentication in progress WHEN reveal requested again THEN prompt shown once`() = runTest {
        // Arrange
        coEvery { cardAuthenticator.authenticate(any()) } coAnswers { awaitCancellation() }
        val controller = createController(scope = backgroundScope)
        runCurrent()
        controller.requestReveal()
        runCurrent()

        // Act
        controller.requestReveal()
        runCurrent()

        // Assert
        coVerify(exactly = 1) { cardAuthenticator.authenticate(any()) }
    }

    @Test
    fun `GIVEN card WHEN created THEN initial state is hidden with masked digits`() = runTest {
        // Act
        val controller = createController(scope = backgroundScope, card = card(lastDigits = "9999"))
        runCurrent()

        // Assert
        val state = controller.uiState.value
        assertThat(state.isHidden).isTrue()
        assertThat(state.numberShort).isEqualTo("${StringsSigns.ASTERISK}9999")
    }

    @Test
    fun `GIVEN Show for this card WHEN event received THEN details revealed and analytics sent`() = runTest {
        // Arrange
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()

        // Act
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.ViewCardDetailsClicked()) }
        coVerify(exactly = 1) { repository.revealCardDetails(userWalletId, cardId) }
        val state = controller.uiState.value
        assertThat(state.isHidden).isFalse()
        assertThat(state.cvv).isEqualTo("123")
        assertThat(state.expiry).isEqualTo("09/30")
    }

    @Test
    fun `GIVEN reveal fails WHEN event received THEN snackbar shown and state hidden`() = runTest {
        // Arrange
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns ERROR.left()
        val controller = createController(scope = backgroundScope)
        runCurrent()

        // Act
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
        assertThat(controller.uiState.value.isHidden).isTrue()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN revealed WHEN visibility event received THEN hidden state follows the event`(
        case: VisibilityEventModel,
    ) = runTest {
        // Arrange
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        // Act
        eventListener.send(case.event)
        runCurrent()

        // Assert
        assertThat(controller.uiState.value.isHidden).isEqualTo(case.isHidden)
    }

    @ParameterizedTest
    @MethodSource("showTimerCases")
    fun `GIVEN revealed WHEN time passes THEN details auto-hide only once the show timeout elapses`(
        case: ShowTimerModel,
    ) = runTest {
        // Arrange
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        // Act
        advanceTimeBy(case.elapsedMillis)
        runCurrent()

        // Assert
        assertThat(controller.uiState.value.isHidden).isEqualTo(case.isHidden)
    }

    @Test
    fun `GIVEN supplier emits actual loaded WHEN card present THEN frozen state and digits updated`() = runTest {
        // Arrange
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

        // Act
        supplierFlow.emit(payment)
        runCurrent()

        // Assert
        val state = controller.uiState.value
        assertThat(state.numberShort).isEqualTo("${StringsSigns.ASTERISK}5678")
        assertThat(state.cardFrozenState).isEqualTo(TangemPayCardFrozenState.Frozen)
        assertThat(state.isActionsAvailable).isTrue()
    }

    @ParameterizedTest
    @MethodSource("copyCases")
    fun `GIVEN revealed WHEN onCopy invoked THEN value copied as sensitive and analytics sent`(case: CopyModel) =
        runTest {
            // Arrange
            val controller = revealedController()

            // Act
            controller.uiState.value.onCopy(case.value, case.type)

            // Assert
            verify(exactly = 1) { clipboardManager.setText(text = case.expectedClipboardText, isSensitive = true) }
            verify(exactly = 1) { analytics.send(case.expectedEvent) }
        }

    @Test
    fun `GIVEN details hidden WHEN onCopy invoked THEN nothing copied`() = runTest {
        // Arrange
        val controller = createController(scope = backgroundScope)
        runCurrent()

        // Act
        controller.uiState.value.onCopy("1 2 3", CardDataType.CVV)

        // Assert
        verify(exactly = 0) { clipboardManager.setText(any(), any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("cardholderNameCases")
    fun `GIVEN card emboss name WHEN created THEN cardholder name mirrors it`(case: CardholderNameModel) = runTest {
        // Act
        val controller = createController(scope = backgroundScope, card = card(embossName = case.embossName))
        runCurrent()

        // Assert
        assertThat(controller.uiState.value.cardholderName).isEqualTo(case.expectedCardholderName)
    }

    @Test
    fun `GIVEN Show already sent WHEN controller created with isolated listener THEN details stay hidden`() =
        runTest {
            // Arrange
            coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
            eventListener.send(CardDetailsEvent.Show(cardId))

            // Act
            val controller = createController(
                scope = backgroundScope,
                listener = CardDetailsEventListener.default(),
            )
            advanceUntilIdle()

            // Assert
            assertThat(controller.uiState.value.isHidden).isTrue()
            coVerify(exactly = 0) { repository.revealCardDetails(any(), any()) }
        }

    @Test
    fun `GIVEN isolated listener WHEN reveal requested THEN blocks on shared listener stay hidden`() = runTest {
        // Arrange
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val sharedController = createController(scope = backgroundScope)
        val isolatedListener = CardDetailsEventListener.default()
        val isolatedController = createController(scope = backgroundScope, listener = isolatedListener)
        runCurrent()

        // Act
        isolatedListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()

        // Assert
        assertThat(isolatedController.uiState.value.isHidden).isFalse()
        assertThat(sharedController.uiState.value.isHidden).isTrue()
    }

    @Test
    fun `GIVEN disposed WHEN Show received THEN reveal not triggered`() = runTest {
        // Arrange
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()
        controller.dispose()

        // Act
        eventListener.send(CardDetailsEvent.Show(cardId))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { repository.revealCardDetails(any(), any()) }
    }

    private fun TestScope.revealedController(): TangemPayCardDetailsController {
        coEvery { repository.revealCardDetails(userWalletId, cardId) } returns details.right()
        val controller = createController(scope = backgroundScope)
        runCurrent()
        eventListener.send(CardDetailsEvent.Show(cardId))
        runCurrent()
        return controller
    }

    private fun TestScope.createController(
        scope: CoroutineScope,
        card: TangemPayCard = card(),
        config: TangemPayCardDetailsController.Config = TangemPayCardDetailsController.Config(
            isEditingNameEnabled = true,
            shouldShowCardDetailsButtonOnCard = false,
        ),
        listener: CardDetailsEventListener = eventListener,
        onEditNameClick: () -> Unit = {},
    ): TangemPayCardDetailsController = TangemPayCardDetailsController(
        scope = scope,
        card = card,
        userWalletId = userWalletId,
        config = config,
        cardDetailsEventListener = listener,
        onEditNameClick = onEditNameClick,
        cardDetailsRepository = repository,
        clipboardManager = clipboardManager,
        uiMessageSender = uiMessageSender,
        analytics = analytics,
        paymentAccountStatusSupplier = supplier,
        cardAuthenticator = cardAuthenticator,
    )

    private fun card(
        id: String = cardId,
        lastDigits: String = "1234",
        displayName: CardDisplayName? = null,
        frozenState: TangemPayCardFrozenState = TangemPayCardFrozenState.Unfrozen,
        state: TangemPayCardState = TangemPayCardState.Active,
        embossName: String? = null,
    ): TangemPayCard = TangemPayCard(
        id = id,
        productInstanceId = "product_$id",
        cardStatus = TangemPayCard.Status.ACTIVE,
        hasPinCode = true,
        displayName = displayName,
        limit = null,
        frozenState = frozenState,
        lastDigits = lastDigits,
        images = emptyList(),
        state = state,
        embossName = embossName,
        cardType = TangemPayCardType.VIRTUAL,
    )

    private fun provideTestModels() = listOf(
        VisibilityEventModel(event = CardDetailsEvent.Show(otherCardId), isHidden = true),
        VisibilityEventModel(event = CardDetailsEvent.Hide(otherCardId), isHidden = false),
        VisibilityEventModel(event = CardDetailsEvent.HideAll, isHidden = true),
    )

    private fun showTimerCases() = listOf(
        ShowTimerModel(elapsedMillis = SHOW_DETAILS_TIME + 1, isHidden = true),
        ShowTimerModel(elapsedMillis = SHOW_DETAILS_TIME - 1, isHidden = false),
    )

    private fun copyCases() = listOf(
        CopyModel(
            value = "1 2 3",
            type = CardDataType.CVV,
            expectedClipboardText = "123",
            expectedEvent = TangemPayAnalyticsEvents.CopyCardCVVClicked(),
        ),
        CopyModel(
            value = "JOHNNY SILVERHAND",
            type = CardDataType.CardholderName,
            expectedClipboardText = "JOHNNY SILVERHAND",
            expectedEvent = TangemPayAnalyticsEvents.CopyCardholderNameClicked(),
        ),
    )

    private fun cardholderNameCases() = listOf(
        CardholderNameModel(embossName = "JOHNNY SILVERHAND", expectedCardholderName = "JOHNNY SILVERHAND"),
        CardholderNameModel(embossName = null, expectedCardholderName = null),
    )

    internal data class VisibilityEventModel(val event: CardDetailsEvent, val isHidden: Boolean)

    internal data class ShowTimerModel(val elapsedMillis: Long, val isHidden: Boolean)

    internal data class CopyModel(
        val value: String,
        val type: CardDataType,
        val expectedClipboardText: String,
        val expectedEvent: TangemPayAnalyticsEvents,
    )

    internal data class CardholderNameModel(val embossName: String?, val expectedCardholderName: String?)

    private companion object {
        val ERROR = object : UniversalError {
            override val errorCode: Int = 0
        }
    }
}