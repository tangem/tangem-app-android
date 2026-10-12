package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.usecase.GetOnrampFeesUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.details.TangemPayBankTransferOnrampDetailsScreenUM.DailyDepositLimitUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import com.tangem.features.tangempay.bankCredentials
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.onrampFee
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankTransferOnrampDetailsModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")

    private val router: Router = mockk(relaxed = true)
    private val getOnrampFeesUseCase: GetOnrampFeesUseCase = mockk()
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private val defaultLocale = Locale.getDefault()

    @BeforeEach
    fun resetMocks() {
        Locale.setDefault(Locale.US)
        clearMocks(router, getOnrampFeesUseCase, clipboardManager, shareManager, analytics)
        coEvery { getOnrampFeesUseCase(userWalletId) } returns emptyList<AccountFee>().right()
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN bank credentials WHEN model created THEN requisites shown in the mock order and shown event sent`() =
        runTest {
            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.requisites.map { it.key to it.value }).containsExactly(
                "Account number" to "123456",
                "Routing number" to "654321",
                "Beneficiary name" to "Rain Payments Inc",
                "Beneficiary address" to "123 Finance St",
                "Bank name" to "SSB Bank",
                "Bank address" to "8700 Perry Highway",
            ).inOrder()
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaBankingDetailsShowed>()) }
        }

    @Test
    fun `GIVEN daily deposit limit WHEN model created THEN limitation shown`() = runTest {
        // Arrange
        val bankCredentials = bankCredentials(
            depositLimit = BankCredentials.DepositLimit(
                amount = BigDecimal("10000"),
                period = BankCredentials.DepositLimit.Period.DAY,
            ),
        )

        // Act
        val model = createModel(bankCredentials)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.dailyDepositLimit).isEqualTo(
            DailyDepositLimitUM(
                value = stringReference("$10,000"),
                hint = resourceReference(
                    id = R.string.tangempay_bank_transfer_details_daily_deposits_hint,
                    formatArgs = wrappedList("$10,000"),
                ),
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no daily deposit limit WHEN model created THEN limitation hidden`(model: HiddenLimitModel) = runTest {
        // Act
        val detailsModel = createModel(bankCredentials(depositLimit = model.depositLimit))
        advanceUntilIdle()

        // Assert
        assertThat(detailsModel.uiState.value.dailyDepositLimit).isNull()
    }

    @Test
    fun `GIVEN onramp fees returned WHEN model created THEN fee rows shown`() = runTest {
        // Arrange
        coEvery { getOnrampFeesUseCase(userWalletId) } returns listOf(onrampFee(amount = BigDecimal("1.00"))).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.fees).isEqualTo(
            TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(
                    TangemPayOnrampFeesUM.FeeRow(type = "ACH_ONRAMP", title = stringReference("ACH"), value = "$1"),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN fees request fails WHEN retry succeeds THEN fee rows shown`() = runTest {
        // Arrange
        coEvery { getOnrampFeesUseCase(userWalletId) } returnsMany listOf(
            VisaApiError.Unspecified.left(),
            listOf(onrampFee(amount = BigDecimal("1.00"))).right(),
        )
        val model = createModel()
        advanceUntilIdle()
        val error = model.uiState.value.fees as TangemPayOnrampFeesUM.Error

        // Act
        error.onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.fees).isInstanceOf(TangemPayOnrampFeesUM.Content::class.java)
        coVerify(exactly = 2) { getOnrampFeesUseCase(userWalletId) }
    }

    @Test
    fun `GIVEN fees retry in flight WHEN retry clicked again THEN shimmer shown and fees requested once more`() =
        runTest {
            // Arrange
            val pending = CompletableDeferred<Either<VisaApiError, List<AccountFee>>>()
            var calls = 0
            coEvery { getOnrampFeesUseCase(userWalletId) } coAnswers {
                calls++
                if (calls == 1) VisaApiError.Unspecified.left() else pending.await()
            }
            val model = createModel()
            advanceUntilIdle()
            val error = model.uiState.value.fees as TangemPayOnrampFeesUM.Error

            // Act
            error.onRetryClick()
            error.onRetryClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.fees).isEqualTo(TangemPayOnrampFeesUM.Loading)
            coVerify(exactly = 2) { getOnrampFeesUseCase(userWalletId) }
            pending.complete(emptyList<AccountFee>().right())
            advanceUntilIdle()
            model.onDestroy()
        }

    @Test
    fun `GIVEN requisites shown WHEN copy clicked THEN value copied as sensitive`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.requisites.first { it.key == "Account number" }.onCopyClick()

        // Assert
        verify(exactly = 1) { clipboardManager.setText(text = "123456", isSensitive = true) }
        verify(exactly = 1) {
            analytics.send(TangemPayAnalyticsEvents.VaCopyFieldClicked(field = "Account number"))
        }
    }

    @Test
    fun `GIVEN requisites shown WHEN share clicked THEN every requisite shared`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onShareClick()

        // Assert
        verify(exactly = 1) {
            shareManager.shareText(
                "Account number: 123456\n" +
                    "Routing number: 654321\n" +
                    "Beneficiary name: Rain Payments Inc\n" +
                    "Beneficiary address: 123 Finance St\n" +
                    "Bank name: SSB Bank\n" +
                    "Bank address: 8700 Perry Highway",
            )
        }
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaShareDetailsButtonClicked>()) }
    }

    @Test
    fun `GIVEN screen shown WHEN close clicked THEN screen popped`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
    }

    private fun TestScope.createModel(bankCredentials: BankCredentials = bankCredentials()) =
        TangemPayBankTransferOnrampDetailsModel(
            paramsContainer = MutableParamsContainer(
                TangemPayBankTransferOnrampDetailsComponent.Params(
                    userWalletId = userWalletId,
                    bankCredentials = bankCredentials,
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            getOnrampFeesUseCase = getOnrampFeesUseCase,
            clipboardManager = clipboardManager,
            shareManager = shareManager,
            analytics = analytics,
        )

    private fun provideTestModels() = listOf(
        HiddenLimitModel(depositLimit = null),
        HiddenLimitModel(
            depositLimit = BankCredentials.DepositLimit(
                amount = BigDecimal("10000"),
                period = BankCredentials.DepositLimit.Period.UNKNOWN,
            ),
        ),
    )

    data class HiddenLimitModel(val depositLimit: BankCredentials.DepositLimit?)
}