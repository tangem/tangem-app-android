package com.tangem.features.tangempay.addfunds.bankTransferOnramp.onboarding

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.AccountFeeGroupType
import com.tangem.domain.pay.usecase.CreateVirtualAccountOrderUseCase
import com.tangem.domain.pay.usecase.GetOnrampFeesUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import com.tangem.features.tangempay.details.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale
import com.tangem.core.ui.R as CoreUiR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankTransferOnrampOnboardingModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")

    private val router: Router = mockk(relaxed = true)
    private val getOnrampFeesUseCase: GetOnrampFeesUseCase = mockk()
    private val createVirtualAccountOrderUseCase: CreateVirtualAccountOrderUseCase = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private val defaultLocale = Locale.getDefault()

    @BeforeEach
    fun resetMocks() {
        Locale.setDefault(Locale.US)
        clearMocks(
            router,
            getOnrampFeesUseCase,
            createVirtualAccountOrderUseCase,
            paymentAccountStatusSupplier,
            uiMessageSender,
            urlOpener,
            analytics,
        )
        coEvery { getOnrampFeesUseCase(userWalletId) } returns emptyList<AccountFee>().right()
        every { paymentAccountStatusSupplier(userWalletId) } returns flowOf(paymentStatus())
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN screen opened WHEN model created THEN conditions shown event sent`() = runTest {
        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaConditionsPopupShowedFirstTime>()) }
    }

    @Test
    fun `GIVEN onramp fees returned WHEN model created THEN fee rows shown`() = runTest {
        // Arrange
        coEvery { getOnrampFeesUseCase(userWalletId) } returns listOf(onrampFee()).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.fees).isEqualTo(
            TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(
                    TangemPayOnrampFeesUM.FeeRow(type = "ACH_ONRAMP", title = stringReference("ACH"), value = "$0"),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN fees request fails WHEN retry succeeds THEN fee rows shown`() = runTest {
        // Arrange
        coEvery { getOnrampFeesUseCase(userWalletId) } returnsMany listOf(
            VisaApiError.Unspecified.left(),
            listOf(onrampFee()).right(),
        )
        val model = createModel()
        advanceUntilIdle()
        val error = model.uiState.value.fees as TangemPayOnrampFeesUM.Error

        // Act
        error.onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.fees).isInstanceOf(TangemPayOnrampFeesUM.Content::class.java)
    }

    @Test
    fun `GIVEN fees retry in flight WHEN retry clicked again THEN shimmer shown and fees requested once more`() =
        runTest {
            // Arrange
            val pendingRetry = CompletableDeferred<Either<VisaApiError, List<AccountFee>>>()
            coEvery { getOnrampFeesUseCase(userWalletId) } returns VisaApiError.Unspecified.left()
            val model = createModel()
            advanceUntilIdle()
            val error = model.uiState.value.fees as TangemPayOnrampFeesUM.Error
            coEvery { getOnrampFeesUseCase(userWalletId) } coAnswers { pendingRetry.await() }

            // Act
            error.onRetryClick()
            error.onRetryClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.fees).isEqualTo(TangemPayOnrampFeesUM.Loading)
            coVerify(exactly = 2) { getOnrampFeesUseCase(userWalletId) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN order created WHEN continue clicked THEN screen closed and request sent toast shown`() = runTest {
        // Arrange
        coEvery {
            createVirtualAccountOrderUseCase(userWalletId = userWalletId, paymentAccountAddress = PAYMENT_ACCOUNT_ADDRESS)
        } returns Unit.right()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
        verify(exactly = 1) {
            uiMessageSender.send(
                SnackbarMessage(
                    message = resourceReference(R.string.tangempay_bank_transfer_request_sent),
                    startIconId = CoreUiR.drawable.ic_success_20,
                    isIconTinted = false,
                ),
            )
        }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaShowDetailsFirstTimeClicked>()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaSuccessScreenActivation>()) }
    }

    @Test
    fun `GIVEN order creation fails WHEN continue clicked THEN error shown and screen stays`() = runTest {
        // Arrange
        coEvery { createVirtualAccountOrderUseCase(any(), any()) } returns VisaApiError.Unspecified.left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isLoading).isFalse()
        verify(exactly = 0) { router.pop(any()) }
        verify(exactly = 1) { uiMessageSender.send(ToastMessage(resourceReference(R.string.common_unknown_error))) }
    }

    @Test
    fun `GIVEN no payment account address WHEN continue clicked THEN order not created`() = runTest {
        // Arrange
        every { paymentAccountStatusSupplier(userWalletId) } returns flowOf(paymentStatus(paymentAccountAddress = null))
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { createVirtualAccountOrderUseCase(any(), any()) }
        verify(exactly = 1) { uiMessageSender.send(ToastMessage(resourceReference(R.string.common_unknown_error))) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN banking details already requested WHEN continue clicked THEN screen closed without a new order`(
        virtualAccount: VirtualAccountOnramp,
    ) = runTest {
        // Arrange
        val status = paymentStatus(virtualAccount = virtualAccount)
        every { paymentAccountStatusSupplier(userWalletId) } returns flowOf(status)
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
        coVerify(exactly = 0) { createVirtualAccountOrderUseCase(any(), any()) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN order in progress WHEN continue clicked again THEN order requested once`() = runTest {
        // Arrange
        coEvery { createVirtualAccountOrderUseCase(any(), any()) } coAnswers { CompletableDeferred<Nothing>().await() }
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { createVirtualAccountOrderUseCase(any(), any()) }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaShowDetailsFirstTimeClicked>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN order in progress WHEN close clicked THEN screen stays`() = runTest {
        // Arrange
        coEvery { createVirtualAccountOrderUseCase(any(), any()) } coAnswers { CompletableDeferred<Nothing>().await() }
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Act
        model.onCloseClick()

        // Assert
        assertThat(model.uiState.value.isLoading).isTrue()
        verify(exactly = 0) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN screen idle WHEN close clicked THEN screen popped`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onCloseClick()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN screen shown WHEN terms clicked THEN terms of service opened`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onTermsClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/docs/en/virtual-account-terms.pdf") }
    }

    private fun TestScope.createModel() = TangemPayBankTransferOnrampOnboardingModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankTransferOnrampOnboardingComponent.Params(userWalletId = userWalletId),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        router = router,
        getOnrampFeesUseCase = getOnrampFeesUseCase,
        createVirtualAccountOrderUseCase = createVirtualAccountOrderUseCase,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        uiMessageSender = uiMessageSender,
        urlOpener = urlOpener,
        analytics = analytics,
    )

    private fun paymentStatus(
        paymentAccountAddress: String? = PAYMENT_ACCOUNT_ADDRESS,
        virtualAccount: VirtualAccountOnramp? = VirtualAccountOnramp.Eligible,
    ): AccountStatus.Payment {
        val status = MockAccounts.createPaymentAccountStatus(
            userWalletId = userWalletId,
            paymentAccountAddress = paymentAccountAddress,
        )
        val loaded = status.value as PaymentAccountStatusValue.Loaded
        return status.copy(value = loaded.copy(virtualAccount = virtualAccount))
    }

    private fun onrampFee() = AccountFee(
        type = "ACH_ONRAMP",
        name = "ACH",
        paymentMethodId = null,
        group = AccountFeeGroupType.ONRAMP,
        amount = BigDecimal.ZERO,
        currency = "USD",
    )

    private fun provideTestModels() = listOf(
        VirtualAccountOnramp.Processing,
        VirtualAccountOnramp.Available(productInstanceId = PRODUCT_INSTANCE_ID),
    )

    private companion object {
        const val PAYMENT_ACCOUNT_ADDRESS = "0xcollateral"
        const val PRODUCT_INSTANCE_ID = "product_instance_id"
    }
}