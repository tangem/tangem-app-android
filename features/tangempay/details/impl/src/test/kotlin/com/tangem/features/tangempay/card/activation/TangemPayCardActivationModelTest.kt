package com.tangem.features.tangempay.card.activation

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CardActivationOrder
import com.tangem.domain.pay.usecase.ActivatePlasticCardUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCardActivationModelTest {

    private val router: Router = mockk(relaxed = true)
    private val activatePlasticCard: ActivatePlasticCardUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxed = true)

    private var model: TangemPayCardActivationModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, activatePlasticCard, paymentAccountStatusFetcher)
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
        coEvery { activatePlasticCard(WALLET_ID, CardActivationOrder(PRODUCT_INSTANCE_ID, "8252"), any()) } returns Unit.right()
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
    @MethodSource("provideAlreadyHandledErrors")
    fun `GIVEN activation is already handled WHEN submitted THEN the status is refreshed and the flow closed`(
        error: VisaApiError,
    ) = runTest {
        // Arrange
        coEvery { activatePlasticCard(any(), any(), any()) } returns error.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        submit(model, "8252")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(WALLET_ID) }
        verify(exactly = 1) { router.pop() }
    }

    private fun provideAlreadyHandledErrors() = listOf(
        VisaApiError.CardActivationCardAlreadyActive,
        VisaApiError.CardActivationActiveOrderExists,
    )

    @Test
    fun `GIVEN an unexpected failure WHEN submitted THEN the error is shown inline and the input stays filled`() =
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
            assertThat(state.isHintError).isTrue()
            assertThat(state.lastDigits).isEqualTo("8252")
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

    private fun submit(model: TangemPayCardActivationModel, lastDigits: String) {
        model.state().onLastDigitsChange(lastDigits)
        model.state().onContinueClick()
    }

    private fun TangemPayCardActivationModel.state() = uiState.value

    private fun createModel(testScope: TestScope) = TangemPayCardActivationModel(
        paramsContainer = MutableParamsContainer(
            TangemPayCardActivationComponent.Params(
                card = tangemPayCard(id = CARD_ID, productInstanceId = PRODUCT_INSTANCE_ID),
                userWalletId = WALLET_ID,
                cardImageUrl = null,
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        activatePlasticCardUseCase = activatePlasticCard,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
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
    }
}