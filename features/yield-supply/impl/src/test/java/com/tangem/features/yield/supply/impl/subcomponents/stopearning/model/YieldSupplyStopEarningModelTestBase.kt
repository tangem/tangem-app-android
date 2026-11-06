package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.right
import com.tangem.domain.transaction.usecase.gasless.IsEvmGaslessFeeSupportedForNetwork
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendEvmGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessBatchUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStopEarningUseCase
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.impl.common.YieldSupplySmartGas
import com.tangem.features.yield.supply.impl.common.YieldSupplyTxHistoryUpdater
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class YieldSupplyStopEarningModelTestBase : YieldSupplyActionModelTestBase() {

    protected val txHistoryUpdater: YieldSupplyTxHistoryUpdater = mockk(relaxed = true)
    protected val stopEarningUseCase: YieldSupplyStopEarningUseCase = mockk()
    protected val deactivateUseCase: YieldSupplyDeactivateUseCase = mockk()
    protected val withdrawalClaimPreparer: YieldSupplyWithdrawalClaimPreparer = mockk()
    protected val claimTracker: YieldSupplyClaimTracker = mockk(relaxed = true)
    protected val callback: YieldSupplyStopEarningComponent.ModelCallback = mockk(relaxed = true)
    protected val tangemBlogUrlBuilder: TangemBlogUrlBuilder = mockk()
    protected val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    protected val accountStatusList: AccountStatusList = mockk()
    protected val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles = mockk()
    protected val estimateFeeForGaslessBatchUseCase: EstimateFeeForGaslessBatchUseCase = mockk()
    protected val createAndSendEvmGaslessTransactionUseCase: CreateAndSendEvmGaslessTransactionUseCase = mockk()
    protected val isEvmGaslessFeeSupportedForNetwork: IsEvmGaslessFeeSupportedForNetwork = mockk()

    @BeforeEach
    fun stopEarningSetUp() {
        mockkObject(CryptoCurrencyStatusOperations)
        coEvery { stopEarningUseCase(any(), any(), any()) } returns uncompiledTx().right()
        coEvery { getFeeUseCase(any(), any(), any()) } returns transactionFee().right()
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns listOf("0xhash").right()
        coEvery { deactivateUseCase(any(), any()) } returns true.right()
        coEvery {
            withdrawalClaimPreparer.prepare(userWallet = any(), cryptoCurrencyStatus = any())
        } returns YieldPreClaim.Skip
        coEvery { accountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
        every { yieldSupplyFeatureToggles.isSmartGasEnabled } returns false
        every { isEvmGaslessFeeSupportedForNetwork(any()) } returns true
    }

    @AfterEach
    fun stopEarningTearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    protected fun TestScope.createModel(
        statusFlow: StateFlow<CryptoCurrencyStatus> = cryptoCurrencyStatusFlow,
    ): YieldSupplyStopEarningModel = YieldSupplyStopEarningModel(
        tangemBlogUrlBuilder = tangemBlogUrlBuilder,
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            YieldSupplyStopEarningComponent.Params(
                userWallet = userWallet,
                cryptoCurrencyStatusFlow = statusFlow,
                callback = callback,
                feeSelectorComponentFactory = mockk(),
            ),
        ),
        analytics = analytics,
        getFeeUseCase = getFeeUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
        yieldSupplyStopEarningUseCase = stopEarningUseCase,
        urlOpener = urlOpener,
        yieldSupplyNotificationsUpdateTrigger = notificationsUpdateTrigger,
        yieldSupplyAlertFactory = alertFactory,
        yieldSupplyDeactivateUseCase = deactivateUseCase,
        yieldSupplyRepository = yieldSupplyRepository,
        yieldSupplyPendingTracker = pendingTracker,
        appsFlyerRepository = appsFlyerRepository,
        yieldSupplyWithdrawalClaimPreparer = withdrawalClaimPreparer,
        yieldSupplyClaimTracker = claimTracker,
        yieldSupplyFeatureToggles = yieldSupplyFeatureToggles,
        estimateFeeForGaslessBatchUseCase = estimateFeeForGaslessBatchUseCase,
        yieldSupplySmartGas = YieldSupplySmartGas(
            sendTransactionUseCase = sendTransactionUseCase,
            createAndSendEvmGaslessTransactionUseCase = createAndSendEvmGaslessTransactionUseCase,
            singleAccountStatusListSupplier = accountStatusListSupplier,
            getAvailableFeeTokensUseCase = getAvailableFeeTokensUseCase,
        ),
        isEvmGaslessFeeSupportedForNetwork = isEvmGaslessFeeSupportedForNetwork,
        yieldSupplyTxHistoryUpdater = txHistoryUpdater,
    )
}