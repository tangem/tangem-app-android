package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import arrow.core.Option
import com.tangem.domain.transaction.usecase.gasless.IsEvmGaslessFeeSupportedForNetwork
import arrow.core.right
import arrow.core.some
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendEvmGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessBatchUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.models.YieldSupplyFee
import com.tangem.domain.yield.supply.models.YieldSupplyMaxFee
import com.tangem.domain.yield.supply.usecase.YieldSupplyActivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApplyEnterAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetCurrentFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetMaxFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStartEarningUseCase
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.common.YieldSupplySmartGas
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class YieldSupplyStartEarningModelTestBase : YieldSupplyActionModelTestBase() {

    protected val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    protected val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    protected val startEarningUseCase: YieldSupplyStartEarningUseCase = mockk()
    protected val estimateEnterFeeUseCase: YieldSupplyEstimateEnterFeeUseCase = mockk()
    protected val activateUseCase: YieldSupplyActivateUseCase = mockk()
    protected val minAmountUseCase: YieldSupplyMinAmountUseCase = mockk()
    protected val getMaxFeeUseCase: YieldSupplyGetMaxFeeUseCase = mockk()
    protected val getCurrentFeeUseCase: YieldSupplyGetCurrentFeeUseCase = mockk()
    protected val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles = mockk()
    protected val estimateFeeForGaslessBatchUseCase: EstimateFeeForGaslessBatchUseCase = mockk()
    protected val applyEnterAmountUseCase: YieldSupplyApplyEnterAmountUseCase = mockk()
    protected val createAndSendEvmGaslessTransactionUseCase: CreateAndSendEvmGaslessTransactionUseCase = mockk()

    protected val isEvmGaslessFeeSupportedForNetwork: IsEvmGaslessFeeSupportedForNetwork = mockk()
    protected val accountStatusList: AccountStatusList = mockk()
    protected val callback: YieldSupplyStartEarningComponent.ModelCallback = mockk(relaxed = true)

    @BeforeEach
    fun startEarningSetUp() {
        mockkObject(CryptoCurrencyStatusOperations)
        every { getUserWalletUseCase(userWalletId) } returns userWallet.right()
        every { accountStatusListSupplier(userWalletId) } returns flowOf(accountStatusList)
        coEvery { accountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
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
        every { yieldSupplyFeatureToggles.isSmartGasEnabled } returns false
        every { isEvmGaslessFeeSupportedForNetwork(any()) } returns true
        coEvery { applyEnterAmountUseCase.supportsEnterAmount(any(), any(), any()) } returns true
    }

    @AfterEach
    fun startEarningTearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    protected fun stubCurrencyStatusLookup(result: Option<CryptoCurrencyStatus>) {
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>())
            }
        } returns result
    }

    protected fun maxFee(): YieldSupplyMaxFee = YieldSupplyMaxFee(
        nativeMaxFee = BigDecimal("0.01"),
        tokenMaxFee = BigDecimal("2"),
        fiatMaxFee = BigDecimal("4"),
    )

    protected fun TestScope.createModel(): YieldSupplyStartEarningModel = YieldSupplyStartEarningModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            YieldSupplyStartEarningComponent.Params(
                userWalletId = userWalletId,
                cryptoCurrency = token,
                yieldSupplyActionUM = actionUM(),
                callback = callback,
                feeSelectorComponentFactory = mockk(),
            ),
        ),
        analytics = analytics,
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = accountStatusListSupplier,
        getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
        yieldSupplyStartEarningUseCase = startEarningUseCase,
        yieldSupplyEstimateEnterFeeUseCase = estimateEnterFeeUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        yieldSupplyNotificationsUpdateTrigger = notificationsUpdateTrigger,
        yieldSupplyAlertFactory = alertFactory,
        yieldSupplyMinAmountUseCase = minAmountUseCase,
        yieldSupplyGetMaxFeeUseCase = getMaxFeeUseCase,
        yieldSupplyGetCurrentFeeUseCase = getCurrentFeeUseCase,
        yieldSupplyRepository = yieldSupplyRepository,
        yieldSupplyPendingTracker = pendingTracker,
        appsFlyerRepository = appsFlyerRepository,
        yieldSupplyFeatureToggles = yieldSupplyFeatureToggles,
        estimateFeeForGaslessBatchUseCase = estimateFeeForGaslessBatchUseCase,
        yieldSupplyApplyEnterAmountUseCase = applyEnterAmountUseCase,
        yieldSupplySmartGas = YieldSupplySmartGas(
            sendTransactionUseCase = sendTransactionUseCase,
            createAndSendEvmGaslessTransactionUseCase = createAndSendEvmGaslessTransactionUseCase,
            singleAccountStatusListSupplier = accountStatusListSupplier,
            getAvailableFeeTokensUseCase = getAvailableFeeTokensUseCase,
        ),
        isEvmGaslessFeeSupportedForNetwork = isEvmGaslessFeeSupportedForNetwork,
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