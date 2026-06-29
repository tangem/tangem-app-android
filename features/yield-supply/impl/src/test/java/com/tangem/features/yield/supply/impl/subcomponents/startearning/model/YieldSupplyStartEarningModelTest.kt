package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.models.YieldSupplyFee
import com.tangem.domain.yield.supply.models.YieldSupplyMaxFee
import com.tangem.domain.yield.supply.usecase.YieldSupplyActivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetCurrentFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetMaxFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStartEarningUseCase
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningComponent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyStartEarningModelTest : YieldSupplyActionModelTestBase() {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val startEarningUseCase: YieldSupplyStartEarningUseCase = mockk()
    private val estimateEnterFeeUseCase: YieldSupplyEstimateEnterFeeUseCase = mockk()
    private val activateUseCase: YieldSupplyActivateUseCase = mockk()
    private val minAmountUseCase: YieldSupplyMinAmountUseCase = mockk()
    private val getMaxFeeUseCase: YieldSupplyGetMaxFeeUseCase = mockk()
    private val getCurrentFeeUseCase: YieldSupplyGetCurrentFeeUseCase = mockk()

    private val accountStatusList: AccountStatusList = mockk()
    private val callback: YieldSupplyStartEarningComponent.ModelCallback = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkObject(CryptoCurrencyStatusOperations)
        every { getUserWalletUseCase(userWalletId) } returns userWallet.right()
        every { accountStatusListSupplier(userWalletId) } returns flowOf(accountStatusList)
        stubCurrencyStatusLookup(cryptoCurrencyStatus.some())
        coEvery { minAmountUseCase(any(), any()) } returns BigDecimal("5").right()
        coEvery { getMaxFeeUseCase(any(), any()) } returns maxFee().right()
        coEvery { getCurrentFeeUseCase(any(), any()) } returns YieldSupplyFee(BigDecimal("0.001")).right()
        coEvery { startEarningUseCase(any(), any(), any()) } returns listOf(uncompiledTx()).right()
        coEvery { estimateEnterFeeUseCase(any(), any(), any()) } returns listOf(uncompiledTx()).right()
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns listOf("0xhash").right()
        coEvery { activateUseCase(any(), any(), any()) } returns true.right()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Test
    fun `GIVEN successful fee load WHEN model created THEN fee content and button enabled`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isInstanceOf(YieldSupplyFeeUM.Content::class.java)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()
        coVerify { notificationsUpdateTrigger.triggerUpdate(any()) }
    }

    @Test
    fun `GIVEN estimate fee fails WHEN model created THEN fee error state`() = runTest {
        // Arrange
        coEvery { estimateEnterFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Error)
    }

    @Test
    fun `GIVEN max fee unavailable WHEN model created THEN fee error state`() = runTest {
        // Arrange
        coEvery { getMaxFeeUseCase(any(), any()) } returns Throwable("no max fee").left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Error)
        coVerify(exactly = 0) { estimateEnterFeeUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN user wallet unavailable WHEN model created THEN shows generic error`() = runTest {
        // Arrange
        every { getUserWalletUseCase(userWalletId) } returns mockk<GetUserWalletError>(relaxed = true).left()

        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        verify { alertFactory.getGenericErrorState(any(), any()) }
        coVerify(exactly = 0) { getMaxFeeUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN currency status not found WHEN model created THEN shows generic error`() = runTest {
        // Arrange
        stubCurrencyStatusLookup(none())

        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        verify { alertFactory.getGenericErrorState(any(), any()) }
        coVerify(exactly = 0) { getMaxFeeUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN content loaded WHEN onClick THEN sends activates tracks pending and notifies sent`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify { yieldSupplyRepository.saveTokenProtocolPendingStatus(userWalletId, any(), any()) }
        coVerify { activateUseCase(userWalletId, any(), any()) }
        coVerify { pendingTracker.addPending(userWalletId, any(), any()) }
        verify { callback.onTransactionSent() }
    }

    @Test
    fun `GIVEN content loaded WHEN onClick and send fails THEN shows error and not sent`() = runTest {
        // Arrange
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns SendTransactionError.UnknownError().left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isTransactionSending).isFalse()
        verify { alertFactory.getSendTransactionErrorState(any(), any(), any()) }
        verify(exactly = 0) { callback.onTransactionSent() }
    }

    @Test
    fun `GIVEN fee not loaded WHEN onClick THEN does not send transactions`() = runTest {
        // Arrange — estimate fee fails so the fee state is Error; onClick must early-return before sending
        coEvery { estimateEnterFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        }
    }

    @Test
    fun `GIVEN notifications report an error WHEN flag emitted THEN primary button disabled`() = runTest {
        // Arrange
        val hasErrorFlow = MutableStateFlow(false)
        every { notificationsUpdateTrigger.hasErrorFlow } returns hasErrorFlow
        val model = createModel()
        advanceUntilIdle()
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()

        // Act
        hasErrorFlow.value = true
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
    }

    private fun stubCurrencyStatusLookup(result: arrow.core.Option<com.tangem.domain.models.currency.CryptoCurrencyStatus>) {
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>())
            }
        } returns result
    }

    private fun maxFee(): YieldSupplyMaxFee = YieldSupplyMaxFee(
        nativeMaxFee = BigDecimal("0.01"),
        tokenMaxFee = BigDecimal("2"),
        fiatMaxFee = BigDecimal("4"),
    )

    private fun TestScope.createModel(): YieldSupplyStartEarningModel = YieldSupplyStartEarningModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            YieldSupplyStartEarningComponent.Params(
                userWalletId = userWalletId,
                cryptoCurrency = token,
                yieldSupplyActionUM = actionUM(),
                callback = callback,
            ),
        ),
        analytics = analytics,
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = accountStatusListSupplier,
        getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        yieldSupplyStartEarningUseCase = startEarningUseCase,
        yieldSupplyEstimateEnterFeeUseCase = estimateEnterFeeUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        yieldSupplyNotificationsUpdateTrigger = notificationsUpdateTrigger,
        yieldSupplyAlertFactory = alertFactory,
        yieldSupplyActivateUseCase = activateUseCase,
        yieldSupplyMinAmountUseCase = minAmountUseCase,
        yieldSupplyGetMaxFeeUseCase = getMaxFeeUseCase,
        yieldSupplyGetCurrentFeeUseCase = getCurrentFeeUseCase,
        yieldSupplyRepository = yieldSupplyRepository,
        yieldSupplyPendingTracker = pendingTracker,
        appsFlyerStore = appsFlyerStore,
    )

    private fun actionUM(): YieldSupplyActionUM = YieldSupplyActionUM(
        title = stringReference(""),
        subtitle = stringReference(""),
        footer = stringReference(""),
        footerLink = stringReference(""),
        currencyIconState = mockk<CurrencyIconState>(relaxed = true),
        yieldSupplyFeeUM = YieldSupplyFeeUM.Loading,
        isPrimaryButtonEnabled = false,
        isTransactionSending = false,
        isHoldToConfirmEnabled = false,
    )
}