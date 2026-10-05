package com.tangem.features.swap.v2.impl.sendviaswap.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.api.entry.SendEntryRoute
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.SwapAlertFactory
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendWithSwapModelCallbackTest {

    private val router: Router = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val swapAlertFactory: SwapAlertFactory = mockk(relaxed = true)
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase =
        mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk(relaxed = true)
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk(relaxed = true)
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk(relaxed = true)

    private val cryptoCurrency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
    private val defaultUserWalletId = UserWalletId(stringValue = "0123456789")

    private val callback: SendWithSwapComponent.ModelCallback = mockk(relaxed = true)

    private val initialState = SendWithSwapUM(
        amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct),
        destinationUM = DestinationUM.Empty(),
        feeSelectorUM = FeeSelectorUM.Loading,
        confirmUM = ConfirmUM.Empty,
    )

    private val changedState = SendWithSwapUM(
        amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Reverse),
        destinationUM = DestinationUM.Empty(isPrimaryButtonEnabled = true),
        feeSelectorUM = FeeSelectorUM.Error(error = GetFeeError.UnknownError),
        confirmUM = ConfirmUM.Content(
            isTransactionInProcess = false,
            isShowTapHelp = false,
            sendingFooter = TextReference.EMPTY,
            notifications = persistentListOf(),
            tosUM = null,
        ),
    )

    /**
     * `initUserWallet()` runs synchronously in `init {}`, so the not-found default keeps construction inert
     * (no router calls, no status subscription). Tests needing another branch re-stub before [createModel].
     * App currency and balance hiding are stubbed so their `init {}` collectors complete cleanly if advanced.
     */
    @BeforeEach
    fun setUpDefaultStubs() {
        every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        every { getBalanceHidingSettingsUseCase() } returns emptyFlow()
    }

    @Test
    fun `GIVEN initial state WHEN onAmountResult THEN only amountUM is replaced`() = runTest {
        // Arrange
        val model = createModel(this)

        // Act
        model.onAmountResult(changedState.amountUM)

        // Assert
        assertThat(model.uiState.value).isEqualTo(initialState.copy(amountUM = changedState.amountUM))
        model.onDestroy()
    }

    @Test
    fun `GIVEN initial state WHEN onDestinationResult THEN only destinationUM is replaced`() = runTest {
        // Arrange
        val model = createModel(this)

        // Act
        model.onDestinationResult(changedState.destinationUM)

        // Assert
        assertThat(model.uiState.value).isEqualTo(initialState.copy(destinationUM = changedState.destinationUM))
        model.onDestroy()
    }

    @Test
    fun `GIVEN initial state WHEN onResult THEN whole state is replaced`() = runTest {
        // Arrange
        val model = createModel(this)

        // Act
        model.onResult(changedState)

        // Assert
        assertThat(model.uiState.value).isEqualTo(changedState)
        model.onDestroy()
    }

    @Test
    fun `GIVEN callback provided WHEN onSeparatorClick THEN closes swap and pops to initial route`() = runTest {
        // Arrange
        val model = createModel(this, callback = callback)

        // Act
        model.onSeparatorClick(lastAmount = "1.5", isEnterInFiatSelected = true)

        // Assert
        verify(exactly = 1) { callback.onCloseSwap(lastAmount = "1.5", isEnterInFiatSelected = true) }
        verify(exactly = 1) { router.popTo(model.initialRoute, any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no callback WHEN onSeparatorClick THEN does not throw and pops to initial route`() = runTest {
        // Arrange
        val model = createModel(this, callback = null)

        // Act
        val error = runCatching {
            model.onSeparatorClick(lastAmount = "1.5", isEnterInFiatSelected = false)
        }.exceptionOrNull()

        // Assert
        assertThat(error).isNull()
        verify(exactly = 1) { router.popTo(model.initialRoute, any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN changed state WHEN reset with navigation THEN clears later steps and pops to amount`() = runTest {
        // Arrange
        val model = createModel(this)
        model.onResult(changedState)

        // Act
        model.resetSendWithSwapNavigation(resetNavigation = true)

        // Assert
        assertThat(model.uiState.value).isEqualTo(changedState.withStepsAfterAmountReset())
        verify(exactly = 1) { router.popTo(SendWithSwapRoute.Amount(isEditMode = false), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN changed state WHEN reset without navigation THEN clears later steps and keeps route`() = runTest {
        // Arrange
        val model = createModel(this)
        model.onResult(changedState)

        // Act
        model.resetSendWithSwapNavigation(resetNavigation = false)

        // Assert
        assertThat(model.uiState.value).isEqualTo(changedState.withStepsAfterAmountReset())
        verify(exactly = 0) { router.popTo(any<Route>(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN user wallet not found WHEN model created THEN shows generic error alert`() = runTest {
        // Arrange
        every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        val model = createModel(this)

        // Assert
        verify(exactly = 1) {
            swapAlertFactory.getGenericErrorState(
                expressError = ExpressError.UnknownError,
                onFailedTxEmailClick = any(),
                popBack = any(),
            )
        }
        verify(exactly = 0) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN generic error alert shown WHEN popBack invoked THEN router pops`() = runTest {
        // Arrange
        val popBackSlot = slot<() -> Unit>()
        every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()
        val model = createModel(this)
        verify {
            swapAlertFactory.getGenericErrorState(
                expressError = ExpressError.UnknownError,
                onFailedTxEmailClick = any(),
                popBack = capture(popBackSlot),
            )
        }

        // Act
        popBackSlot.captured()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN user wallet found WHEN model created THEN publishes wallet and currency status updates`() = runTest {
        // Arrange
        val userWallet = MockUserWalletFactory.create()
        val account = Account.Personal.createMainAccount(
            userWalletId = defaultUserWalletId,
            cryptoCurrencies = listOf(cryptoCurrency),
        )
        val cryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
        )
        val feePaidCurrencyStatus = CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
        )
        every { getUserWalletUseCase(defaultUserWalletId) } returns userWallet.right()
        every {
            getAccountCurrencyStatusUseCase(userWalletId = defaultUserWalletId, currency = cryptoCurrency)
        } returns flowOf(AccountCryptoCurrencyStatus(account = account, status = cryptoCurrencyStatus))
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns true
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = defaultUserWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            )
        } returns feePaidCurrencyStatus.right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        assertThat(model.userWallet).isEqualTo(userWallet)
        assertThat(model.accountFlow.value).isEqualTo(account)
        assertThat(model.isAccountModeFlow.value).isTrue()
        assertThat(model.primaryCryptoCurrencyStatusFlow.value).isEqualTo(cryptoCurrencyStatus)
        assertThat(model.primaryFeePaidCurrencyStatusFlow.value).isEqualTo(feePaidCurrencyStatus)
        verify(exactly = 0) { swapAlertFactory.getGenericErrorState(any(), any(), any()) }
        model.onDestroy()
    }

    private fun createModel(
        testScope: TestScope,
        callback: SendWithSwapComponent.ModelCallback? = null,
        userWalletId: UserWalletId = defaultUserWalletId,
    ): SendWithSwapModel {
        val params = SendWithSwapComponent.Params(
            userWalletId = userWalletId,
            currency = cryptoCurrency,
            callback = callback,
            currentRoute = MutableStateFlow(SendEntryRoute.SendWithSwap),
        )
        return SendWithSwapModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            swapAlertFactory = swapAlertFactory,
            paramsContainer = MutableParamsContainer(value = params),
        )
    }

    private fun SendWithSwapUM.withStepsAfterAmountReset(): SendWithSwapUM = copy(
        destinationUM = DestinationUM.Empty(),
        feeSelectorUM = FeeSelectorUM.Loading,
        confirmUM = ConfirmUM.Empty,
    )
}