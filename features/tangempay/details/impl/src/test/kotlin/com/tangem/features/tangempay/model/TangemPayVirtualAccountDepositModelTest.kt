package com.tangem.features.tangempay.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.CreateVirtualAccountOrderUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.components.TangemPayVirtualAccountDepositComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayVirtualAccountDepositModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val paymentAccountAddress = "0xcollateral"

    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val createVirtualAccountOrderUseCase: CreateVirtualAccountOrderUseCase = mockk()
    private val onShowDetails: (VirtualAccountOnramp.Available) -> Unit = mockk(relaxed = true)
    private val onShowBankingDetailsError: () -> Unit = mockk(relaxed = true)
    private val onOrderCreated: () -> Unit = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            createVirtualAccountOrderUseCase,
            onShowDetails,
            onShowBankingDetailsError,
            onOrderCreated,
            uiMessageSender,
            analytics,
        )
    }

    @Test
    fun `GIVEN available WHEN show details THEN opens requisites and does not create order`() = runTest {
        // Arrange
        val onramp = VirtualAccountOnramp.Available(productInstanceId = "pi_1", bankCredentials = bankCredentials())
        val model = createModel(onramp)

        // Act
        model.uiState.value.onShowDetailsClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onShowDetails(onramp) }
        coVerify(exactly = 0) { createVirtualAccountOrderUseCase(any(), any()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaConditionsPopupShowed>()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaShowDetailsClicked>()) }
    }

    @Test
    fun `GIVEN bank credentials error WHEN show details THEN shows banking details error sheet`() = runTest {
        // Arrange
        val model = createModel(VirtualAccountOnramp.BankCredentialsError)

        // Act
        model.uiState.value.onShowDetailsClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onShowBankingDetailsError() }
        verify(exactly = 0) { onShowDetails(any()) }
        coVerify(exactly = 0) { createVirtualAccountOrderUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN eligible and create succeeds WHEN show details THEN order created and loading reset`() = runTest {
        // Arrange
        coEvery { createVirtualAccountOrderUseCase(userWalletId, paymentAccountAddress) } returns Unit.right()
        val model = createModel(VirtualAccountOnramp.Eligible)

        // Act
        model.uiState.value.onShowDetailsClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { createVirtualAccountOrderUseCase(userWalletId, paymentAccountAddress) }
        verify(exactly = 1) { onOrderCreated() }
        assertThat(model.uiState.value.isLoading).isFalse()
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaConditionsPopupShowedFirstTime>()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaShowDetailsFirstTimeClicked>()) }
    }

    @Test
    fun `GIVEN eligible and create fails WHEN show details THEN toast shown and loading reset`() = runTest {
        // Arrange
        coEvery {
            createVirtualAccountOrderUseCase(userWalletId, paymentAccountAddress)
        } returns VisaApiError.Unspecified.left()
        val model = createModel(VirtualAccountOnramp.Eligible)

        // Act
        model.uiState.value.onShowDetailsClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<ToastMessage>()) }
        verify { onOrderCreated wasNot Called }
        assertThat(model.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `GIVEN already loading WHEN show details twice THEN use case invoked once`() = runTest {
        // Arrange
        val pending = CompletableDeferred<Either<VisaApiError, Unit>>()
        coEvery { createVirtualAccountOrderUseCase(userWalletId, paymentAccountAddress) } coAnswers { pending.await() }
        val model = createModel(VirtualAccountOnramp.Eligible)

        // Act
        model.uiState.value.onShowDetailsClick() // starts loading, use case suspends
        advanceUntilIdle()
        model.uiState.value.onShowDetailsClick() // gated by isLoading — must be ignored
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isLoading).isTrue()
        coVerify(exactly = 1) { createVirtualAccountOrderUseCase(userWalletId, paymentAccountAddress) }

        pending.complete(Unit.right()) // let the in-flight call finish cleanly
        advanceUntilIdle()
    }

    private fun TestScope.createModel(onramp: VirtualAccountOnramp) = TangemPayVirtualAccountDepositModel(
        paramsContainer = MutableParamsContainer(
            TangemPayVirtualAccountDepositComponent.Params(
                virtualAccountOnramp = onramp,
                userWalletId = userWalletId,
                paymentAccountAddress = paymentAccountAddress,
                onDismiss = {},
                onShowDetails = onShowDetails,
                onShowBankingDetailsError = onShowBankingDetailsError,
                onOrderCreated = onOrderCreated,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        urlOpener = urlOpener,
        uiMessageSender = uiMessageSender,
        createVirtualAccountOrderUseCase = createVirtualAccountOrderUseCase,
        analytics = analytics,
    )

    private fun bankCredentials() = BankCredentials(
        type = "ACH",
        beneficiaryName = "Test Beneficiary",
        beneficiaryAddress = "Addr",
        beneficiaryBankName = "Bank",
        beneficiaryBankAddress = "Bank Addr",
        accountNumber = "123",
        routingNumber = "456",
    )

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
}