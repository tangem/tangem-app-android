package com.tangem.features.tangempay.addfunds

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetBankCredentialsUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
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
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayAddFundsModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")

    private val listener: AddFundsListener = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val featureToggles: TangemPayFeatureToggles = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val getBankCredentialsUseCase: GetBankCredentialsUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(listener, router, featureToggles, analytics, getBankCredentialsUseCase)
    }

    @Test
    fun `GIVEN v2 enabled and banking details load WHEN bank transfer clicked THEN account details screen opens`() =
        runTest {
            // Arrange
            every { featureToggles.isVisaOnrampV2Enabled } returns true
            coEvery { getBankCredentialsUseCase(userWalletId, PRODUCT_INSTANCE_ID) } returns bankCredentials().right()
            val model = createModel(VirtualAccountOnramp.Available(productInstanceId = PRODUCT_INSTANCE_ID))

            // Act
            model.clickBankTransfer()
            advanceUntilIdle()

            // Assert
            verifyOrder {
                listener.onDismissAddFunds()
                router.push(TangemPayAccountDetailsInnerRoute.BankTransferOnrampDetails(bankCredentials()), any())
            }
            verify(exactly = 0) { listener.onClickBankTransfer() }
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaTopupButtonClicked>()) }
        }

    @Test
    fun `GIVEN v2 enabled WHEN bank transfer clicked THEN the row loads until banking details arrive`() = runTest {
        // Arrange
        every { featureToggles.isVisaOnrampV2Enabled } returns true
        val pending = CompletableDeferred<BankCredentials>()
        coEvery { getBankCredentialsUseCase(userWalletId, PRODUCT_INSTANCE_ID) } coAnswers { pending.await().right() }
        val model = createModel(VirtualAccountOnramp.Available(productInstanceId = PRODUCT_INSTANCE_ID))

        // Act
        model.clickBankTransfer()
        advanceUntilIdle()

        // Assert
        assertThat(model.bankTransferRow().isLoading).isTrue()
        assertThat(model.bankTransferRow().onClick).isNull()
        pending.complete(bankCredentials())
        advanceUntilIdle()
        verify(exactly = 1) { router.push(any(), any()) }
    }

    @Test
    fun `GIVEN v2 enabled and banking details fail WHEN bank transfer clicked THEN host shows the error`() = runTest {
        // Arrange
        every { featureToggles.isVisaOnrampV2Enabled } returns true
        coEvery { getBankCredentialsUseCase(userWalletId, PRODUCT_INSTANCE_ID) } returns VisaApiError.Unspecified.left()
        val model = createModel(VirtualAccountOnramp.Available(productInstanceId = PRODUCT_INSTANCE_ID))

        // Act
        model.clickBankTransfer()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { listener.onBankingDetailsLoadFailed(PRODUCT_INSTANCE_ID) }
        verify(exactly = 0) { router.push(any(), any()) }
        assertThat(model.bankTransferRow().isLoading).isFalse()
        assertThat(model.bankTransferRow().onClick).isNotNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no v2 screen for the state WHEN bank transfer clicked THEN host flow handles the click`(
        model: HostFlowModel,
    ) = runTest {
        // Arrange
        every { featureToggles.isVisaOnrampV2Enabled } returns model.isV2Enabled
        val addFundsModel = createModel(model.onramp)

        // Act
        addFundsModel.clickBankTransfer()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { listener.onClickBankTransfer() }
        verify(exactly = 0) { router.push(any(), any()) }
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.VaTopupButtonClicked>()) }
        coVerify(exactly = 0) { getBankCredentialsUseCase(any(), any()) }
    }

    private fun TangemPayAddFundsModel.bankTransferRow(): TangemPayAddFundsItemUM = uiState.value.items.first {
        it.title == resourceReference(R.string.tangempay_topup_bank_transfer_title)
    }

    private fun TangemPayAddFundsModel.clickBankTransfer() {
        requireNotNull(bankTransferRow().onClick).invoke()
    }

    private fun TestScope.createModel(onramp: VirtualAccountOnramp) = TangemPayAddFundsModel(
        paramsContainer = MutableParamsContainer(
            TangemPayAddFundsComponent.Params(
                listener = listener,
                walletId = userWalletId,
                cryptoBalance = BigDecimal.ONE,
                fiatBalance = BigDecimal.ONE,
                depositAddress = "0xdeposit",
                cryptoCurrency = MockAccounts.createPaymentAccountToken(),
                virtualAccountOnramp = onramp,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        analytics = analytics,
        router = router,
        tangemPayFeatureToggles = featureToggles,
        getBankCredentialsUseCase = getBankCredentialsUseCase,
    )

    private fun bankCredentials() = BankCredentials(
        type = "ACH",
        beneficiaryName = "Rain Payments Inc",
        beneficiaryAddress = "123 Finance St",
        beneficiaryBankName = "SSB Bank",
        beneficiaryBankAddress = "8700 Perry Highway",
        accountNumber = "123456",
        routingNumber = "654321",
        transferMessage = null,
        depositFee = null,
        depositLimit = null,
    )

    private fun provideTestModels() = listOf(
        HostFlowModel(isV2Enabled = false, onramp = VirtualAccountOnramp.Available(PRODUCT_INSTANCE_ID)),
        HostFlowModel(isV2Enabled = true, onramp = VirtualAccountOnramp.Eligible),
        HostFlowModel(isV2Enabled = true, onramp = VirtualAccountOnramp.Processing),
    )

    data class HostFlowModel(val isV2Enabled: Boolean, val onramp: VirtualAccountOnramp)

    private companion object {
        const val PRODUCT_INSTANCE_ID = "pi_account"
    }
}