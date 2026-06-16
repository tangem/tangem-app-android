package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import arrow.core.right
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.*
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.tokens.*
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.approval.api.GiveApprovalFeatureToggles
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.helpers.StakingBalanceUpdater
import com.tangem.features.staking.impl.presentation.state.helpers.StakingOperationsFactory
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class StakingModelTestBase {

    protected val testUserWalletId = UserWalletId("1234567890ABCDEF")
    protected val testCryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    protected open val testIntegrationId: StakingIntegrationID = StakingIntegrationID.StakeKit.Coin.Solana
    private val testParams get() = StakingComponent.Params(
        userWalletId = testUserWalletId,
        cryptoCurrency = testCryptoCurrency,
        integrationId = testIntegrationId,
    )
    protected val testYield: Yield = mockk(relaxed = true)
    protected val testUserWallet: UserWallet = mockk(relaxed = true)
    protected val initialUiState: StakingUiState = mockk(relaxed = true) {
        every { currentStep } returns StakingStep.InitialInfo
    }

    private lateinit var testCryptoCurrencyStatus: CryptoCurrencyStatus
    private lateinit var testAccountCurrencyStatus: AccountCryptoCurrencyStatus
    protected lateinit var mockBalanceUpdater: StakingBalanceUpdater

    protected val stateController: StakingStateController = mockk()
    private val getYieldUseCase: GetYieldUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    protected val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk()
    protected val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    protected val appRouter: AppRouter = mockk()

    protected val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    protected val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase = mockk()
    protected val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase = mockk()
    protected val sendTransactionUseCase: SendTransactionUseCase = mockk()
    protected val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase = mockk()
    protected val getAllowanceUseCase: GetAllowanceUseCase = mockk()
    protected val vibratorHapticManager: VibratorHapticManager = mockk()
    protected val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    protected val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase = mockk()
    protected val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase = mockk()
    protected val getCurrencyCheckUseCase: GetCurrencyCheckUseCase = mockk()
    protected val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk()
    protected val isAnyTokenStakedUseCase: IsAnyTokenStakedUseCase = mockk()
    private val invalidatePendingTransactionsUseCase: InvalidatePendingTransactionsUseCase = mockk()
    protected val stakingOperationsFactory: StakingOperationsFactory = mockk()
    protected val stakingBalanceUpdater: StakingBalanceUpdater.Factory = mockk()
    protected val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk()
    protected val getActionsUseCase: GetActionsUseCase = mockk()
    protected val p2pEthPoolRepository: P2PEthPoolRepository = mockk()
    protected val checkAccountInitializedUseCase: CheckAccountInitializedUseCase = mockk()
    protected val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk()
    protected val getFeeUseCase: GetFeeUseCase = mockk()
    protected val getNetworkAddressesUseCase: GetNetworkAddressesUseCase = mockk()
    protected val getActionRequirementAmountUseCase: GetActionRequirementAmountUseCase = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()
    protected val paramsInterceptorHolder: ParamsInterceptorHolder = mockk(relaxed = true)
    protected val shareManager: ShareManager = mockk()
    protected val urlOpener: UrlOpener = mockk()
    private val coroutineScope: AppCoroutineScope = mockk()
    protected val innerRouter: InnerStakingRouter = mockk()
    protected val messageSender: UiMessageSender = mockk()
    protected val giveApprovalFeatureToggles: GiveApprovalFeatureToggles = mockk()

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        val (status, accountStatus) = createMockedAccountCurrencyStatus()
        testCryptoCurrencyStatus = status
        testAccountCurrencyStatus = accountStatus

        coEvery { getYieldUseCase(testIntegrationId.value) } returns Either.Right(testYield)
        every { getSelectedAppCurrencyUseCase() } returns flowOf(Either.Right(AppCurrency.Default))
        every { stateController.uiState } returns MutableStateFlow(initialUiState)
        every { stateController.initializeWithUserWallet(any()) } just Runs
        every { stateController.updateAll(*anyVararg()) } just Runs
        every { stateController.update(any<Transformer<StakingUiState>>()) } just Runs
        every { stateController.update(any<(StakingUiState) -> StakingUiState>()) } just Runs
        every { getUserWalletUseCase(testUserWalletId) } returns Either.Right(testUserWallet)
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns flowOf(testAccountCurrencyStatus)
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns true
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())
        coEvery { checkAccountInitializedUseCase(testUserWalletId, any()) } returns true.right()
        coEvery { isAnyTokenStakedUseCase(testUserWalletId) } returns Either.Right(false)
        coEvery {
            isAmountSubtractAvailableUseCase(testUserWalletId, any())
        } returns Either.Right(false)
        every { getActionsUseCase(testUserWalletId, any()) } returns emptyFlow()
        every { getBalanceHidingSettingsUseCase() } returns emptyFlow()
        mockBalanceUpdater = mockk {
            coEvery { partialUpdate() } just Runs
        }
        every {
            stakingBalanceUpdater.create(any(), any(), any())
        } returns mockBalanceUpdater
    }

    @Suppress("LongParameterList")
    protected fun createModel(
        testScope: TestScope,
        paramsContainer: ParamsContainer = MutableParamsContainer(testParams),
        coroutineScope: AppCoroutineScope = this.coroutineScope,
    ): StakingModel {
        return StakingModel(
            paramsContainer = paramsContainer,
            stateController = stateController,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
            getMinimumTransactionAmountSyncUseCase = getMinimumTransactionAmountSyncUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            sendTransactionUseCase = sendTransactionUseCase,
            createApprovalTransactionUseCase = createApprovalTransactionUseCase,
            getAllowanceUseCase = getAllowanceUseCase,
            vibratorHapticManager = vibratorHapticManager,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
            getBalanceNotEnoughForFeeWarningUseCase = getBalanceNotEnoughForFeeWarningUseCase,
            getCurrencyCheckUseCase = getCurrencyCheckUseCase,
            isAmountSubtractAvailableUseCase = isAmountSubtractAvailableUseCase,
            isAnyTokenStakedUseCase = isAnyTokenStakedUseCase,
            invalidatePendingTransactionsUseCase = invalidatePendingTransactionsUseCase,
            stakingOperationsFactory = stakingOperationsFactory,
            stakingBalanceUpdater = stakingBalanceUpdater,
            analyticsEventHandler = analyticsEventHandler,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            getActionsUseCase = getActionsUseCase,
            getYieldUseCase = getYieldUseCase,
            p2pEthPoolRepository = p2pEthPoolRepository,
            checkAccountInitializedUseCase = checkAccountInitializedUseCase,
            createTransferTransactionUseCase = createTransferTransactionUseCase,
            getFeeUseCase = getFeeUseCase,
            getNetworkAddressesUseCase = getNetworkAddressesUseCase,
            getActionRequirementAmountUseCase = getActionRequirementAmountUseCase,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
            paramsInterceptorHolder = paramsInterceptorHolder,
            shareManager = shareManager,
            urlOpener = urlOpener,
            coroutineScope = coroutineScope,
            innerRouter = innerRouter,
            messageSender = messageSender,
            giveApprovalFeatureToggles = giveApprovalFeatureToggles,
            appRouter = appRouter,
        )
    }

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

    protected fun createMockedAccountCurrencyStatus(): Pair<CryptoCurrencyStatus, AccountCryptoCurrencyStatus> {
        val testCryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true)
        val testAccountCurrencyStatus = mockk<AccountCryptoCurrencyStatus> {
            every { component1() } returns mockk(relaxed = true)
            every { component2() } returns testCryptoCurrencyStatus
        }
        return testCryptoCurrencyStatus to testAccountCurrencyStatus
    }
}