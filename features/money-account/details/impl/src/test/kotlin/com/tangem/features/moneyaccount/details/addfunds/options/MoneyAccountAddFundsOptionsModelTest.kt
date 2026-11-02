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
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.features.moneyaccount.details.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.pay.MockBankCredentials
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountAddFundsOptionsModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")

    private val listener: MoneyAccountAddFundsOptionsListener = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val getMoneyAccountBankCredentialsUseCase: GetMoneyAccountBankCredentialsUseCase = mockk()
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(listener, analytics, getMoneyAccountBankCredentialsUseCase, moneyAccountFeatureToggles)
        every { moneyAccountFeatureToggles.isMxnEnabled } returns false
    }

    @Test
    fun `GIVEN options sheet WHEN model created THEN add funds screen opened event sent`() = runTest {
        // Act
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Assert
        verify(exactly = 1) { analytics.send(MoneyAccountAnalyticsEvents.AddFundsScreenOpened()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideMethodModels")
    fun `GIVEN option WHEN clicked THEN its add funds method reported`(model: MethodModel) = runTest {
        // Arrange
        coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId, false) } returns listOf(CREDENTIALS).right()
        val optionsModel = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        optionsModel.option(model.title).onClick?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(MoneyAccountAnalyticsEvents.AddFundsMethodSelected(model.expectedMethod)) }
        optionsModel.onDestroy()
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
        coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId, false) } returns listOf(CREDENTIALS).right()
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.bankOption().onClick?.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { listener.onBankDetailsLoaded(listOf(CREDENTIALS)) }
        verify(exactly = 0) { listener.onBankingDetailsLoadFailed() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN MXN toggle WHEN bank option clicked THEN credentials of all rails requested`() = runTest {
        // Arrange
        every { moneyAccountFeatureToggles.isMxnEnabled } returns true
        coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId, true) } returns listOf(CREDENTIALS).right()
        val model = createModel(depositAddress = DEPOSIT_ADDRESS)

        // Act
        model.bankOption().onClick?.invoke()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getMoneyAccountBankCredentialsUseCase(userWalletId, true) }
        verify(exactly = 1) { listener.onBankDetailsLoaded(listOf(CREDENTIALS)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN credentials fail WHEN bank option clicked THEN listener notified and loading reset`() = runTest {
        // Arrange
        coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId, false) } returns
            VisaApiError.ServerUnavailable.left()
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
    fun `GIVEN credentials in flight WHEN bank option tapped twice THEN one request and one method event sent`() =
        runTest {
            // Arrange
            val pending = CompletableDeferred<Either<VisaApiError, List<BankCredentials>>>()
            coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId, false) } coAnswers { pending.await() }
            val model = createModel(depositAddress = DEPOSIT_ADDRESS)
            val onBankClick = requireNotNull(model.bankOption().onClick)

            // Act
            onBankClick()
            onBankClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.bankOption().isLoading).isTrue()
            assertThat(model.bankOption().onClick).isNull()
            coVerify(exactly = 1) { getMoneyAccountBankCredentialsUseCase(userWalletId, false) }
            verify(exactly = 1) {
                analytics.send(
                    MoneyAccountAnalyticsEvents.AddFundsMethodSelected(
                        MoneyAccountAnalyticsEvents.AddFundsMethod.BANK_TRANSFER,
                    ),
                )
            }
            pending.complete(listOf(CREDENTIALS).right())
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

    private fun provideMethodModels() = listOf(
        MethodModel(
            title = resourceReference(R.string.tangempay_topup_swap_title),
            expectedMethod = MoneyAccountAnalyticsEvents.AddFundsMethod.WALLET,
        ),
        MethodModel(
            title = resourceReference(R.string.tangempay_topup_receive_title),
            expectedMethod = MoneyAccountAnalyticsEvents.AddFundsMethod.EXTERNAL_WALLET,
        ),
        MethodModel(
            title = resourceReference(R.string.tangempay_topup_bank_transfer_title),
            expectedMethod = MoneyAccountAnalyticsEvents.AddFundsMethod.BANK_TRANSFER,
        ),
    )

    data class MethodModel(val title: TextReference, val expectedMethod: MoneyAccountAnalyticsEvents.AddFundsMethod)

    private fun MoneyAccountAddFundsOptionsModel.option(title: TextReference) = uiState.value.items.first {
        it.title == title
    }

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
        moneyAccountFeatureToggles = moneyAccountFeatureToggles,
    )

    private companion object {
        const val DEPOSIT_ADDRESS = "0xdeposit"

        val CREDENTIALS = MockBankCredentials.bankCredentials()
    }
}