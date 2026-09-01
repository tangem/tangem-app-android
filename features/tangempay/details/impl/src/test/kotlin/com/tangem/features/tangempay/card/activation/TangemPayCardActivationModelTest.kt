package com.tangem.features.tangempay.card.activation

import androidx.annotation.StringRes
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.EventMessage
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayImage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CardActivationOrder
import com.tangem.domain.pay.usecase.ActivatePlasticCardUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import com.tangem.core.ui.R as CoreUiR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCardActivationModelTest {

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val activatePlasticCard: ActivatePlasticCardUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private var model: TangemPayCardActivationModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(analytics, router, activatePlasticCard, paymentAccountStatusFetcher, uiMessageSender)
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN a fresh screen WHEN opened THEN the description is shown and Continue is disabled`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.state()
        assertThat(state.lastDigits).isEmpty()
        assertThat(state.isContinueEnabled).isFalse()
        assertThat(state.isHintError).isFalse()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `GIVEN a card with an activation image WHEN opened THEN that image is shown`() = runTest {
        // Arrange
        val card = tangemPayCard(
            images = listOf(
                TangemPayImage(type = "MAIN", url = MAIN_IMAGE_URL),
                TangemPayImage(type = "ACTIVATION", url = ACTIVATION_IMAGE_URL),
            ),
        )

        // Act
        val model = createModel(testScope = this, card = card)
        advanceUntilIdle()

        // Assert
        assertThat(model.state().cardImageUrl).isEqualTo(ACTIVATION_IMAGE_URL)
    }

    @Test
    fun `GIVEN a card without an activation image WHEN opened THEN no image is shown`() = runTest {
        // Arrange
        val card = tangemPayCard(images = listOf(TangemPayImage(type = "MAIN", url = MAIN_IMAGE_URL)))

        // Act
        val model = createModel(testScope = this, card = card)
        advanceUntilIdle()

        // Assert
        assertThat(model.state().cardImageUrl).isNull()
    }

    @Test
    fun `GIVEN non-digits and extra characters WHEN typed THEN only four digits are kept`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state().onLastDigitsChange("8a2 5-2 9")

        // Assert
        assertThat(model.state().lastDigits).isEqualTo("8252")
        assertThat(model.state().isContinueEnabled).isTrue()
    }

    @Test
    fun `GIVEN fewer than four digits WHEN Continue clicked THEN nothing is submitted`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state().onLastDigitsChange("825")
        model.state().onContinueClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { activatePlasticCard(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the correct last digits WHEN submitted THEN the flow is closed`() = runTest {
        // Arrange
        coEvery {
            activatePlasticCard(WALLET_ID, CardActivationOrder(PRODUCT_INSTANCE_ID, "8252"), any())
        } returns Unit.right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        submit(model, "8252")
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN a submit in flight WHEN Continue clicked again THEN only one request is sent`() = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns Unit.right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state().onLastDigitsChange("8252")
        model.state().onContinueClick()
        model.state().onContinueClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { activatePlasticCard(any(), any(), any()) }
    }

    @Test
    fun `GIVEN wrong last digits WHEN submitted THEN the error is shown and the input cleared`() = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns
            VisaApiError.CardActivationInvalidCardData.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        submit(model, "1111")
        advanceUntilIdle()

        // Assert
        val state = model.state()
        assertThat(state.isHintError).isTrue()
        assertThat(state.lastDigits).isEmpty()
        assertThat(state.isLoading).isFalse()
        assertThat(state.isContinueEnabled).isFalse()
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN wrong last digits repeatedly WHEN submitted THEN the screen keeps accepting attempts`() = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns
            VisaApiError.CardActivationInvalidCardData.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        repeat(times = 3) {
            submit(model, "1111")
            advanceUntilIdle()
        }

        // Assert
        assertThat(model.state().isHintError).isTrue()
        coVerify(exactly = 3) { activatePlasticCard(any(), any(), any()) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN an error shown WHEN the user types again THEN the error is cleared`() = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns
            VisaApiError.CardActivationInvalidCardData.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        submit(model, "1111")
        advanceUntilIdle()

        // Act
        model.state().onLastDigitsChange("2")

        // Assert
        assertThat(model.state().isHintError).isFalse()
        assertThat(model.state().lastDigits).isEqualTo("2")
    }

    @ParameterizedTest
    @MethodSource("provideRejectionModels")
    fun `GIVEN a rejection the screen cannot fix WHEN submitted THEN its own reason is shown`(
        testModel: RejectionModel,
    ) = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns testModel.error.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        submit(model, "8252")
        advanceUntilIdle()

        // Assert
        assertThat(lastMessageTitle()).isEqualTo(resourceReference(testModel.title))
        assertThat(model.state().isHintError).isFalse()
        assertThat(model.state().isLoading).isFalse()
        coVerify(exactly = testModel.statusRefreshes) { paymentAccountStatusFetcher.invoke(WALLET_ID) }
        verify(exactly = 0) { router.pop() }
    }

    @ParameterizedTest
    @MethodSource("provideRejectionModels")
    fun `GIVEN a rejection sheet WHEN closed THEN the navigation that rejection owes is performed`(
        testModel: RejectionModel,
    ) = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns testModel.error.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        submit(model, "8252")
        advanceUntilIdle()

        // Act
        closeLastMessage()

        // Assert
        verify(exactly = testModel.pops) { router.pop() }
        verify(exactly = testModel.accountPops) {
            router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails)
        }
    }

    internal data class RejectionModel(
        val error: VisaApiError,
        @StringRes val title: Int,
        val statusRefreshes: Int = 0,
        val pops: Int = 1,
        val accountPops: Int = 0,
    )

    private fun provideRejectionModels() = listOf(
        RejectionModel(
            error = VisaApiError.CardActivationCardNotPhysical,
            title = R.string.tangempay_card_activation_error_not_physical,
            pops = 0,
            accountPops = 1,
        ),
        RejectionModel(
            error = VisaApiError.CardActivationCardAlreadyActive,
            title = R.string.tangempay_card_activation_error_already_active,
            statusRefreshes = 1,
        ),
        RejectionModel(
            error = VisaApiError.CardActivationCardNotReadyForActivation,
            title = R.string.tangempay_card_activation_error_not_ready,
        ),
        RejectionModel(
            error = VisaApiError.CardActivationActiveOrderExists,
            title = R.string.tangempay_card_activation_error_in_progress,
            statusRefreshes = 1,
        ),
    )

    @Test
    fun `GIVEN an unexpected failure WHEN submitted THEN a generic sheet is shown and the input stays filled`() =
        runTest {
            // Arrange
            coEvery { activatePlasticCard(any(), any(), any()) } returns VisaApiError.ServerUnavailable.left()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            submit(model, "8252")
            advanceUntilIdle()

            // Assert
            val state = model.state()
            assertThat(state.isLoading).isFalse()
            assertThat(state.isHintError).isFalse()
            assertThat(state.lastDigits).isEqualTo("8252")
            assertThat(lastMessageTitle()).isEqualTo(resourceReference(CoreUiR.string.common_something_went_wrong))
            verify(exactly = 0) { router.pop() }
        }

    @Test
    fun `GIVEN a submit in flight WHEN state observed THEN Continue stays enabled and shows the loader`() = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } coAnswers { awaitCancellation() }
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        submit(model, "8252")
        advanceUntilIdle()

        // Assert
        val state = model.state()
        assertThat(state.isLoading).isTrue()
        assertThat(state.isContinueEnabled).isTrue()
    }

    @Test
    fun `GIVEN a submit in flight WHEN digits typed THEN the input is not editable`() = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } coAnswers { awaitCancellation() }
        val model = createModel(testScope = this)
        advanceUntilIdle()
        submit(model, "8252")
        advanceUntilIdle()

        // Act
        model.state().onLastDigitsChange("1")

        // Assert
        assertThat(model.state().lastDigits).isEqualTo("8252")
        assertThat(model.state().isLoading).isTrue()
    }

    private fun lastMessageTitle(): TextReference? {
        val slot = slot<EventMessage>()
        verify { uiMessageSender.send(capture(slot)) }
        return (slot.captured as BottomSheetMessage).messageBottomSheetUM.elements
            .filterIsInstance<MessageBottomSheetUM.InfoBlock>()
            .firstNotNullOfOrNull { it.title }
    }

    private fun closeLastMessage() {
        val slot = slot<EventMessage>()
        verify { uiMessageSender.send(capture(slot)) }
        val sheet = (slot.captured as BottomSheetMessage).messageBottomSheetUM
        sheet.elements.filterIsInstance<MessageBottomSheetUM.Button>().first().onClick?.invoke(sheet.closeScope)
    }

    private fun submit(model: TangemPayCardActivationModel, lastDigits: String) {
        model.state().onLastDigitsChange(lastDigits)
        model.state().onContinueClick()
    }

    private fun TangemPayCardActivationModel.state() = uiState.value

    @Test
    fun `GIVEN the activation screen WHEN the model is created THEN the screen opened event is sent`() = runTest {
        // Act
        createModel(testScope = this)

        // Assert
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardActivationScreenOpened>())
        }
    }

    @Test
    fun `GIVEN a partial input WHEN the fourth digit is typed THEN the digits entered event is sent once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onLastDigitsChange("1")
        model.uiState.value.onLastDigitsChange("12")
        model.uiState.value.onLastDigitsChange("123")
        model.uiState.value.onLastDigitsChange("1234")

        // Assert
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardLast4DigitsEntered>()) }
    }

    @Test
    fun `GIVEN four digits entered WHEN more characters are typed THEN the digits entered event is not repeated`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            model.uiState.value.onLastDigitsChange("1234")

            // Act
            model.uiState.value.onLastDigitsChange("12345")
            model.uiState.value.onLastDigitsChange("1234a")

            // Assert
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardLast4DigitsEntered>()) }
        }

    @Test
    fun `GIVEN the correct last digits WHEN submitted THEN continue clicked and success events are sent`() = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns Unit.right()
        val model = createModel(testScope = this)
        model.uiState.value.onLastDigitsChange("1234")

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.ActivationContinueClicked>()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardActivationSuccess>()) }
    }

    @Test
    fun `GIVEN wrong last digits WHEN submitted THEN the validation error event is sent and no success`() = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns
            VisaApiError.CardActivationInvalidCardData.left()
        val model = createModel(testScope = this)
        model.uiState.value.onLastDigitsChange("1234")

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.Last4DigitsValidationErrorShowed>())
        }
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardActivationSuccess>()) }
    }

    @Test
    fun `GIVEN fewer than four digits WHEN Continue clicked THEN no continue clicked event is sent`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        model.uiState.value.onLastDigitsChange("123")

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.ActivationContinueClicked>()) }
    }

    private fun createModel(
        testScope: TestScope,
        card: TangemPayCard = tangemPayCard(id = CARD_ID, productInstanceId = PRODUCT_INSTANCE_ID),
    ) = TangemPayCardActivationModel(
        paramsContainer = MutableParamsContainer(
            TangemPayCardActivationComponent.Params(card = card, userWalletId = WALLET_ID),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        analytics = analytics,
        router = router,
        activatePlasticCardUseCase = activatePlasticCard,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        uiMessageSender = uiMessageSender,
    ).also { model = it }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        val WALLET_ID = UserWalletId("1234567890ABCDEF")
        const val CARD_ID = "card-1"
        const val PRODUCT_INSTANCE_ID = "pi-1"
        const val MAIN_IMAGE_URL = "https://tangem.com/card-main.png"
        const val ACTIVATION_IMAGE_URL = "https://tangem.com/card-activation.png"
    }
}