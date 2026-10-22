package com.tangem.features.moneyaccount.details.addfunds.options

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBankCredentialsUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.moneyaccount.details.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.pay.MockBankCredentials
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountAddFundsOptionsModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")

    private val listener: MoneyAccountAddFundsOptionsListener = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val getMoneyAccountBankCredentialsUseCase: GetMoneyAccountBankCredentialsUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(listener, analytics, getMoneyAccountBankCredentialsUseCase)
    }

    @Test
    fun `GIVEN options sheet WHEN model created THEN info screen event sent`() = runTest {
        // Act
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Assert
        verify(exactly = 1) { analytics.send(ofType<MoneyAccountAnalyticsEvents.DepositInfoScreenOpened>()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN deposit address WHEN model created THEN the options it allows are shown`(model: OptionsModel) =
        runTest {
            // Act
            val optionsModel = createModel(depositAddress = model.depositAddress)

            // Assert
            assertThat(optionsModel.uiState.value.items.map { it.title })
                .containsExactlyElementsIn(model.expectedTitles)
                .inOrder()
            optionsModel.onDestroy()
        }

    @Test
    fun `GIVEN swap option WHEN clicked THEN listener notified`() = runTest {
        // Arrange
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.uiState.value.items.first { it.title == resourceReference(R.string.tangempay_topup_swap_title) }
            .onClick?.invoke()

        // Assert
        verify(exactly = 1) { listener.onSwapClick() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN receive option WHEN clicked THEN listener receives the deposit address`() = runTest {
        // Arrange
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.receiveOption().onClick?.invoke()

        // Assert
        verify(exactly = 1) { listener.onReceiveClick(DEPOSIT_ADDRESS) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN credentials load WHEN bank option clicked THEN listener receives them`() = runTest {
        // Arrange
        coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId) } returns CREDENTIALS.right()
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.bankOption().onClick?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { listener.onBankDetailsLoaded(CREDENTIALS) }
        verify(exactly = 0) { listener.onBankingDetailsLoadFailed() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN credentials fail WHEN bank option clicked THEN listener notified and loading reset`() = runTest {
        // Arrange
        coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId) } returns VisaApiError.ServerUnavailable.left()
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.bankOption().onClick?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { listener.onBankingDetailsLoadFailed() }
        verify(exactly = 0) { listener.onBankDetailsLoaded(any()) }
        assertThat(model.bankOption().isLoading).isFalse()
        assertThat(model.bankOption().onClick).isNotNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN credentials in flight WHEN bank option tapped twice THEN one request and the row is loading`() =
        runTest {
            // Arrange
            val pending = CompletableDeferred<Either<VisaApiError, BankCredentials>>()
            coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId) } coAnswers { pending.await() }
            val model = createModel(depositAddress = DEPOSIT_ADDRESS)
            val onBankClick = requireNotNull(model.bankOption().onClick)

            // Act
            onBankClick()
            onBankClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.bankOption().isLoading).isTrue()
            assertThat(model.bankOption().onClick).isNull()
            coVerify(exactly = 1) { getMoneyAccountBankCredentialsUseCase(userWalletId) }
            pending.complete(CREDENTIALS.right())
            advanceUntilIdle()
            model.onDestroy()
        }

    @Test
    fun `GIVEN model WHEN dismissed THEN listener notified`() = runTest {
        // Arrange
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.onDismiss()

        // Assert
        verify(exactly = 1) { listener.onDismissAddFunds() }
        model.onDestroy()
    }

    private fun provideTestModels() = listOf(
        OptionsModel(
            depositAddress = DEPOSIT_ADDRESS,
            expectedTitles = listOf(
                resourceReference(R.string.tangempay_topup_swap_title),
                resourceReference(R.string.tangempay_topup_receive_title),
                resourceReference(R.string.tangempay_topup_bank_transfer_title),
            ),
        ),
        OptionsModel(
            depositAddress = null,
            expectedTitles = listOf(resourceReference(R.string.tangempay_topup_bank_transfer_title)),
        ),
    )

    data class OptionsModel(val depositAddress: String?, val expectedTitles: List<TextReference>)

    private fun MoneyAccountAddFundsOptionsModel.receiveOption() = uiState.value.items.first {
        it.title == resourceReference(R.string.tangempay_topup_receive_title)
    }

    private fun MoneyAccountAddFundsOptionsModel.bankOption() = uiState.value.items.first {
        it.title == resourceReference(R.string.tangempay_topup_bank_transfer_title)
    }

    private fun TestScope.createModel(depositAddress: String?) = MoneyAccountAddFundsOptionsModel(
        paramsContainer = MutableParamsContainer(
            MoneyAccountAddFundsOptionsComponent.Params(
                userWalletId = userWalletId,
                depositAddress = depositAddress,
                listener = listener,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        analytics = analytics,
        getMoneyAccountBankCredentialsUseCase = getMoneyAccountBankCredentialsUseCase,
    )

    private companion object {
        const val DEPOSIT_ADDRESS = "0xdeposit"

        val CREDENTIALS = MockBankCredentials.bankCredentials()
    }
}