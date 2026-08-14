package com.tangem.features.tangempay.addfunds.va.deposit

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.TangemPayOnrampFee
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.CreateVirtualAccountOrderUseCase
import com.tangem.domain.pay.usecase.GetBankCredentialsUseCase
import com.tangem.domain.pay.usecase.GetOnrampFeesUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class TangemPayVirtualAccountDepositModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val paymentAccountAddress = "0xcollateral"
    private val productInstanceId = "pi_1"

    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val getBankCredentialsUseCase: GetBankCredentialsUseCase = mockk()
    private val createVirtualAccountOrderUseCase: CreateVirtualAccountOrderUseCase = mockk()
    private val getOnrampFeesUseCase: GetOnrampFeesUseCase = mockk()
    private val onShowDetails: (BankCredentials) -> Unit = mockk(relaxed = true)
    private val onShowBankingDetailsError: (String) -> Unit = mockk(relaxed = true)
    private val onOrderCreated: () -> Unit = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        // Fee amounts are formatted via NumberFormat with the default locale — pin it so assertions are stable.
        Locale.setDefault(Locale.US)
        clearMocks(
            getBankCredentialsUseCase,
            createVirtualAccountOrderUseCase,
            getOnrampFeesUseCase,
            onShowDetails,
            onShowBankingDetailsError,
            onOrderCreated,
            uiMessageSender,
            analytics,
        )
        coEvery { getOnrampFeesUseCase(any()) } returns emptyList<TangemPayOnrampFee>().right()
    }

    @Test
    fun `GIVEN fees not loaded yet WHEN model created THEN fee block shimmers`() = runTest {
        // GIVEN
        coEvery { getOnrampFeesUseCase(userWalletId) } coAnswers { CompletableDeferred<Nothing>().await() }

        // WHEN
        val model = createModel(VirtualAccountOnramp.Eligible)
        advanceUntilIdle()

        // THEN
        assertThat(model.uiState.value.fees).isEqualTo(TangemPayVirtualAccountDepositUM.FeesUM.Loading)
    }

    @Test
    fun `GIVEN onramp fees returned WHEN model created THEN rows built from response`() = runTest {
        // GIVEN
        coEvery { getOnrampFeesUseCase(userWalletId) } returns listOf(
            onrampFee(type = "ACH_ONRAMP", name = "ACH", amount = BigDecimal("1.00")),
            onrampFee(type = "FEDWIRE_ONRAMP", name = "FedWire", amount = BigDecimal("11.00")),
        ).right()

        // WHEN
        val model = createModel(VirtualAccountOnramp.Eligible)
        advanceUntilIdle()

        // THEN
        assertThat(model.uiState.value.fees).isEqualTo(
            TangemPayVirtualAccountDepositUM.FeesUM.Content(
                rows = persistentListOf(
                    TangemPayVirtualAccountDepositUM.FeeRow(title = stringReference("ACH"), value = "$1"),
                    TangemPayVirtualAccountDepositUM.FeeRow(title = stringReference("FedWire"), value = "$11"),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN fees request fails WHEN model created THEN error banner shown`() = runTest {
        // GIVEN
        coEvery { getOnrampFeesUseCase(userWalletId) } returns VisaApiError.Unspecified.left()

        // WHEN
        val model = createModel(VirtualAccountOnramp.Eligible)
        advanceUntilIdle()

        // THEN
        assertThat(model.uiState.value.fees).isInstanceOf(TangemPayVirtualAccountDepositUM.FeesUM.Error::class.java)
    }

    @Test
    fun `GIVEN fees error WHEN retry succeeds THEN rows shown`() = runTest {
        // GIVEN
        coEvery { getOnrampFeesUseCase(userWalletId) } returnsMany listOf(
            VisaApiError.Unspecified.left(),
            listOf(onrampFee(type = "ACH_ONRAMP", name = "ACH", amount = BigDecimal("1.00"))).right(),
        )
        val model = createModel(VirtualAccountOnramp.Eligible)
        advanceUntilIdle()

        // WHEN
        (model.uiState.value.fees as TangemPayVirtualAccountDepositUM.FeesUM.Error).onRetryClick()
        advanceUntilIdle()

        // THEN
        assertThat(model.uiState.value.fees).isEqualTo(
            TangemPayVirtualAccountDepositUM.FeesUM.Content(
                rows = persistentListOf(
                    TangemPayVirtualAccountDepositUM.FeeRow(title = stringReference("ACH"), value = "$1"),
                ),
            ),
        )
        coVerify(exactly = 2) { getOnrampFeesUseCase(userWalletId) }
    }

    @Test
    fun `GIVEN fees error WHEN retry clicked THEN fee block shimmers while request in flight`() = runTest {
        // GIVEN
        val pending = CompletableDeferred<Either<VisaApiError, List<TangemPayOnrampFee>>>()
        var calls = 0
        coEvery { getOnrampFeesUseCase(userWalletId) } coAnswers {
            calls++
            if (calls == 1) VisaApiError.Unspecified.left() else pending.await()
        }
        val model = createModel(VirtualAccountOnramp.Eligible)
        advanceUntilIdle()

        // WHEN
        (model.uiState.value.fees as TangemPayVirtualAccountDepositUM.FeesUM.Error).onRetryClick()
        advanceUntilIdle()

        // THEN
        assertThat(model.uiState.value.fees).isEqualTo(TangemPayVirtualAccountDepositUM.FeesUM.Loading)
        coVerify(exactly = 2) { getOnrampFeesUseCase(userWalletId) }

        pending.complete(emptyList<TangemPayOnrampFee>().right())
        advanceUntilIdle()
    }

    @Test
    fun `GIVEN available and fetch succeeds WHEN show details THEN opens requisites`() = runTest {
        // Arrange
        val credentials = bankCredentials()
        coEvery { getBankCredentialsUseCase(userWalletId, productInstanceId) } returns credentials.right()
        val model = createModel(VirtualAccountOnramp.Available(productInstanceId = productInstanceId))

        // Act
        model.uiState.value.onShowDetailsClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onShowDetails(credentials) }
        coVerify(exactly = 0) { createVirtualAccountOrderUseCase(any(), any()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaConditionsPopupShowed>()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaShowDetailsClicked>()) }
    }

    @Test
    fun `GIVEN available and fetch fails WHEN show details THEN banking details error shown`() = runTest {
        // Arrange
        coEvery {
            getBankCredentialsUseCase(userWalletId, productInstanceId)
        } returns VisaApiError.Unspecified.left()
        val model = createModel(VirtualAccountOnramp.Available(productInstanceId = productInstanceId))

        // Act
        model.uiState.value.onShowDetailsClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { onShowBankingDetailsError(productInstanceId) }
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
        getBankCredentialsUseCase = getBankCredentialsUseCase,
        createVirtualAccountOrderUseCase = createVirtualAccountOrderUseCase,
        getOnrampFeesUseCase = getOnrampFeesUseCase,
        analytics = analytics,
    )

    private fun onrampFee(type: String, name: String, amount: BigDecimal) = TangemPayOnrampFee(
        type = type,
        name = name,
        amount = amount,
        currency = "USD",
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