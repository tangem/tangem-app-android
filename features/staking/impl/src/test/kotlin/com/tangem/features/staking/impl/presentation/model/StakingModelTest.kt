package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import arrow.core.right
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.*
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.*
import com.tangem.domain.staking.analytics.StakeScreenSource
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.tokens.*
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.approval.api.GiveApprovalFeatureToggles
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.helpers.StakingBalanceUpdater
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeLoader
import com.tangem.features.staking.impl.presentation.state.helpers.StakingOperationsFactory
import com.tangem.features.staking.impl.presentation.state.helpers.StakingTransactionSender
import com.tangem.features.staking.impl.presentation.state.transformers.*
import com.tangem.features.staking.impl.presentation.state.transformers.amount.*
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalBottomSheetInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalBottomSheetTypeChangeTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.ShowApprovalBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.DismissStakingNotificationsStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.CompleteInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.SetFeeErrorToTonInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.SetFeeToTonInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.ShowTonInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelTest {

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val testCryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    private val testIntegrationId = StakingIntegrationID.StakeKit.Coin.Solana
    private val testParams = StakingComponent.Params(
        userWalletId = testUserWalletId,
        cryptoCurrency = testCryptoCurrency,
        integrationId = testIntegrationId,
    )
    private val testYield: Yield = mockk(relaxed = true)
    private val testUserWallet: UserWallet = mockk(relaxed = true)
    private val initialUiState: StakingUiState = mockk(relaxed = true) {
        every { currentStep } returns StakingStep.InitialInfo
    }

    private lateinit var testCryptoCurrencyStatus: CryptoCurrencyStatus
    private lateinit var testAccountCurrencyStatus: AccountCryptoCurrencyStatus
    private lateinit var mockBalanceUpdater: StakingBalanceUpdater

    private val stateController: StakingStateController = mockk()
    private val getYieldUseCase: GetYieldUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk()

    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase = mockk()
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase = mockk()
    private val sendTransactionUseCase: SendTransactionUseCase = mockk()
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase = mockk()
    private val getAllowanceUseCase: GetAllowanceUseCase = mockk()
    private val vibratorHapticManager: VibratorHapticManager = mockk()
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase = mockk()
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase = mockk()
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase = mockk()
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk()
    private val isAnyTokenStakedUseCase: IsAnyTokenStakedUseCase = mockk()
    private val invalidatePendingTransactionsUseCase: InvalidatePendingTransactionsUseCase = mockk()
    private val stakingOperationsFactory: StakingOperationsFactory = mockk()
    private val stakingBalanceUpdater: StakingBalanceUpdater.Factory = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk()
    private val getActionsUseCase: GetActionsUseCase = mockk()
    private val p2pEthPoolRepository: P2PEthPoolRepository = mockk()
    private val checkAccountInitializedUseCase: CheckAccountInitializedUseCase = mockk()
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk()
    private val getFeeUseCase: GetFeeUseCase = mockk()
    private val getNetworkAddressesUseCase: GetNetworkAddressesUseCase = mockk()
    private val getActionRequirementAmountUseCase: GetActionRequirementAmountUseCase = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()
    private val paramsInterceptorHolder: ParamsInterceptorHolder = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk()
    private val urlOpener: UrlOpener = mockk()
    private val coroutineScope: AppCoroutineScope = mockk()
    private val innerRouter: InnerStakingRouter = mockk()
    private val messageSender: UiMessageSender = mockk()
    private val giveApprovalFeatureToggles: GiveApprovalFeatureToggles = mockk()

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

    @Test
    fun `GIVEN currency status emitted WHEN model created THEN analytics sent and fee status fetched`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.value } returns mockk {
            every { stakingBalance } returns mockk<StakingBalance.Data.StakeKit> {
                every { balance } returns YieldBalanceItem(
                    items = listOf(
                        mockk { every { validatorAddress } returns "address1" },
                        mockk { every { validatorAddress } returns "address2" },
                    ),
                    integrationId = "test"
                )
            }
        }
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns flowOf(testAccountCurrencyStatus)
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())

        val model = createModel(testScope = this)
        advanceUntilIdle()

        verify {
            paramsInterceptorHolder.addParamsInterceptor(
                match { it.id() == "StakingParamsInterceptorId" }
            )
        }
        verify {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.StakingInfoScreenOpened(
                    validatorsCount = 2
                ),
            )
        }
        verify {
            stateController.initializeWithUserWallet(testUserWallet)
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN currency status emitted twice WHEN model created THEN analytics sent only once`() = runTest {
        val statusFlow = MutableSharedFlow<AccountCryptoCurrencyStatus>()
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns statusFlow
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())

        val model = createModel(testScope = this)
        statusFlow.emit(testAccountCurrencyStatus)
        advanceUntilIdle()
        statusFlow.emit(testAccountCurrencyStatus)
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsEventHandler.send(
                event = StakingAnalyticsEvent.StakingInfoScreenOpened(validatorsCount = 0)
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN account initialized WHEN checkForTonHeatupCase THEN no error logged`() = runTest {
        mockkObject(TangemLogger)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify { checkAccountInitializedUseCase(testUserWalletId, any()) }
        verify(exactly = 0) { TangemLogger.e(any(), any()) }

        model.onDestroy()
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN checkAccountInitialized fails WHEN checkForTonHeatupCase THEN error logged`() = runTest {
        val testError = RuntimeException("network error")
        coEvery {
            checkAccountInitializedUseCase(testUserWalletId, any())
        } returns Either.Left(testError)
        mockkObject(TangemLogger)
        every { TangemLogger.e(any(), any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        verify { TangemLogger.e("Error", testError) }

        model.onDestroy()
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN approval needed WHEN setupApprovalNeeded THEN getAllowanceUseCase called`() = runTest {
        val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"
        mockkObject(StakingIntegrationID.Companion)
        every {
            StakingIntegrationID.create(any())
        } returns mockk {
            every { approval } returns StakingApproval.Needed(spenderAddress)
        }
        coEvery {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        } returns Either.Right(BigDecimal.TEN)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        }

        model.onDestroy()
        unmockkObject(StakingIntegrationID.Companion)
    }

    @Test
    fun `GIVEN approval needed AND getAllowance fails WHEN setupApprovalNeeded THEN no crash`() = runTest {
        val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"
        mockkObject(StakingIntegrationID.Companion)
        every {
            StakingIntegrationID.create(any())
        } returns mockk {
            every { approval } returns StakingApproval.Needed(spenderAddress)
        }
        coEvery {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        } returns Either.Left(RuntimeException("allowance error")) // error

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify {
            getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
        }

        model.onDestroy()
        unmockkObject(StakingIntegrationID.Companion)
    }

    @Test
    fun `GIVEN any token staked WHEN setupIsAnyTokenStaked THEN use case called with correct wallet id`() = runTest {
        coEvery { isAnyTokenStakedUseCase(testUserWalletId) } returns Either.Right(true)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify { isAnyTokenStakedUseCase(testUserWalletId) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN subtract available WHEN checkIfSubtractAvailable THEN use case called with correct params`() = runTest {
        coEvery {
            isAmountSubtractAvailableUseCase(testUserWalletId, any())
        } returns Either.Right(true)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        coVerify { isAmountSubtractAvailableUseCase(testUserWalletId, any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN actions emitted WHEN subscribeOnActionsUpdates AND isInitState THEN updateInitialData`() = runTest {
        val testActions = listOf(mockk<StakingAction>(relaxed = true))
        every {
            getActionsUseCase(testUserWalletId, any())
        } returns flowOf(Either.Right(testActions))

        val model = createModel(testScope = this)
        advanceUntilIdle()

        verify(atLeast = 1) {
            stateController.updateAll(
                match { it is SetInitialDataStateTransformer },
                any(),
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN init step WHEN subscribeOnStepChanges THEN updateInitialData and partialUpdate called`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        verify(atLeast = 1) {
            stateController.updateAll(
                match { it is SetInitialDataStateTransformer },
                any(),
            )
        }
        coVerify { mockBalanceUpdater.partialUpdate() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN assent step AND isWarning WHEN subscribeOnStepChanges THEN getFee AND amount rounded to integer`() =
        runTest {
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            every {
                stakingOperationsFactory.createFeeLoader(
                    cryptoCurrencyStatus = any(),
                    userWallet = any(),
                    integration = any()
                )
            } returns mockk<StakingFeeLoader> {
                coEvery {
                    getFee(any(), any(), any(), any())
                } just Runs
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            val assentUiState = mockk<StakingUiState>(relaxed = true) {
                every { currentStep } returns StakingStep.Confirmation
                every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data> {
                    every { innerState } returns InnerConfirmationStakingState.ASSENT
                }
                every { amountState } returns mockk<AmountState.Data> {
                    every { amountTextField } returns mockk {
                        every { isWarning } returns true
                    }
                }
            }
            uiStateFlow.value = assentUiState
            advanceUntilIdle()

            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is SetConfirmationStateLoadingTransformer }
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN balance hidden WHEN subscribeOnBalanceHiding THEN HideBalanceStateTransformer applied`() = runTest {
        val balanceHidingSettings = BalanceHidingSettings(
            isHidingEnabledInSettings = true,
            isBalanceHidden = true,
            isBalanceHidingNotificationEnabled = false,
        )
        every { getBalanceHidingSettingsUseCase() } returns flowOf(balanceHidingSettings)

        val model = createModel(testScope = this)
        advanceUntilIdle()

        verify {
            stateController.update(
                match<Transformer<StakingUiState>> { it is HideBalanceStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onDestroy THEN params interceptor removed`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onDestroy()

        verify {
            paramsInterceptorHolder.removeParamsInterceptor("StakingParamsInterceptorId")
        }
    }

    @Test
    fun `WHEN onBackClick THEN router pop and stateController clear called`() = runTest {
        every { stateController.value } returns initialUiState
        every { appRouter.pop(any()) } just Runs
        every { stateController.clear() } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onBackClick()

        verify { appRouter.pop(any()) }
        verify { stateController.clear() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN targets AND no yield balance WHEN onNextClick with balance THEN validators unavailable alert sent`() =
        runTest {
            every { messageSender.send(any()) } just Runs
            every { testYield.allValidatorsFull } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onNextClick(balanceState = null)
            advanceUntilIdle()

            verify {
                messageSender.send(
                    match { it is DialogMessage } // dialog from StakingModel.stakingEventFactory
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN partial amount disabled WHEN onNextClick with null balance THEN updateAll called with transformers`() =
        runTest {
            every { stateController.value } returns initialUiState
            every { testYield.args.enter.isPartialAmountDisabled } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onNextClick(balanceState = null)
            advanceUntilIdle()

            verify {
                stateController.updateAll(
                    match { it is SetConfirmationStateInitTransformer },
                    match { it is ValidatorSelectChangeTransformer },
                    match { it is SetAmountDataTransformer },
                    match { it is AmountMaxValueStateTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN non-initial step WHEN onNextClick THEN only stakingStateRouter onNextClick called`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every { stateController.value } returns initialUiState

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val amountUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Amount
        }
        uiStateFlow.value = amountUiState
        every { stateController.value } returns amountUiState
        advanceUntilIdle()

        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        model.onNextClick(balanceState = null)
        advanceUntilIdle()

        verify {
            stateController.update(match<(StakingUiState) -> StakingUiState> { true })
        }
        verify(exactly = 0) {
            stateController.updateAll(*anyVararg())
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN getFee THEN loading state set and feeLoader called`() = runTest {
        val mockFeeLoader = mockk<StakingFeeLoader> {
            coEvery {
                getFee(any(), any(), any(), any())
            } just Runs
        }
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockFeeLoader

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.getFee()
        advanceUntilIdle()

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> {
                    it is SetConfirmationStateLoadingTransformer
                }
            )
        }
        coVerify {
            mockFeeLoader.getFee(any(), any(), any(), any())
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN StakeKit integration AND assent state WHEN onActionClick THEN sendTransaction called`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        val mockFeeLoader = mockk<StakingFeeLoader> {
            coEvery {
                getFee(any(), any(), any(), any())
            } just Runs
        }
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockFeeLoader
        val mockTransactionSender = mockk<StakingTransactionSender> {
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatus = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any()
            )
        } returns mockTransactionSender

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val assentUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data> {
                every { innerState } returns InnerConfirmationStakingState.ASSENT
            }
        }
        uiStateFlow.value = assentUiState
        every { stateController.value } returns assentUiState
        advanceUntilIdle()

        model.onActionClick()
        advanceUntilIdle()

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> {
                    it is SetConfirmationStateInProgressTransformer
                }
            )
        }
        coVerify { mockTransactionSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN P2PEthPool AND fee not increased WHEN onActionClick THEN sendTransaction called directly`() = runTest {
        val p2pParams = StakingComponent.Params(
            userWalletId = testUserWalletId,
            cryptoCurrency = testCryptoCurrency,
            integrationId = StakingIntegrationID.P2PEthPool,
        )
        coEvery { p2pEthPoolRepository.getVaultsSync() } returns emptyList()
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        coEvery {
            getBalanceNotEnoughForFeeWarningUseCase(
                fee = any(),
                userWalletId = any(),
                tokenStatus = any(),
                feeStatus = any()
            )
        } returns Either.Right(null)
        coEvery {
            getCurrencyCheckUseCase(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any()
            )
        } returns mockk(relaxed = true)
        val newFee = mockk<Fee.Common>(relaxed = true) {
            every { amount.value } returns BigDecimal.ONE
        }
        val mockFeeLoader = mockk<StakingFeeLoader> {
            coEvery {
                getFee(any(), any(), any(), any())
            } coAnswers {
                firstArg<(Fee, Boolean) -> Unit>().invoke(newFee, false)
            }
        }
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockFeeLoader
        val mockTransactionSender = mockk<StakingTransactionSender> {
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatus = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any()
            )
        } returns mockTransactionSender
        val currentFee = mockk<Fee.Common>(relaxed = true) {
            every { amount.value } returns BigDecimal.TEN
        }
        val assentUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.ASSENT
                every { feeState } returns mockk<FeeState.Content>(relaxed = true) {
                    every { fee } returns currentFee
                }
            }
        }

        val model = createModel(
            paramsContainer = MutableParamsContainer(p2pParams),
            testScope = this,
        )
        advanceUntilIdle()

        uiStateFlow.value = assentUiState
        every { stateController.value } returns assentUiState
        advanceUntilIdle()

        model.onActionClick()
        advanceUntilIdle()

        coVerify { mockTransactionSender.send(any()) }
        verify(exactly = 0) { messageSender.send(match { it is DialogMessage }) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN P2PEthPool AND fee increased WHEN onActionClick THEN fee updated alert shown`() = runTest {
        val p2pParams = StakingComponent.Params(
            userWalletId = testUserWalletId,
            cryptoCurrency = testCryptoCurrency,
            integrationId = StakingIntegrationID.P2PEthPool,
        )
        coEvery { p2pEthPoolRepository.getVaultsSync() } returns emptyList()
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        coEvery {
            getBalanceNotEnoughForFeeWarningUseCase(
                fee = any(),
                userWalletId = any(),
                tokenStatus = any(),
                feeStatus = any()
            )
        } returns Either.Right(null)
        coEvery {
            getCurrencyCheckUseCase(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)
        every { messageSender.send(any()) } just Runs
        val newFee = mockk<Fee.Common>(relaxed = true) {
            every { amount.value } returns BigDecimal.TEN
        }
        val mockFeeLoader = mockk<StakingFeeLoader> {
            coEvery {
                getFee(any(), any(), any(), any())
            } coAnswers {
                firstArg<(Fee, Boolean) -> Unit>().invoke(newFee, false)
            }
        }
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockFeeLoader
        val mockTransactionSender = mockk<StakingTransactionSender> {
            coEvery { send(any()) } just Runs
        }
        every {
            stakingOperationsFactory.createTransactionSender(
                cryptoCurrencyStatus = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any(),
            )
        } returns mockTransactionSender
        val currentFee = mockk<Fee.Common>(relaxed = true) {
            every { amount.value } returns BigDecimal.ONE
        }
        val assentUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.ASSENT
                every { feeState } returns mockk<FeeState.Content>(relaxed = true) {
                    every { fee } returns currentFee
                }
            }
        }

        val model = createModel(
            paramsContainer = MutableParamsContainer(p2pParams),
            testScope = this,
        )
        advanceUntilIdle()

        uiStateFlow.value = assentUiState
        every { stateController.value } returns assentUiState
        advanceUntilIdle()

        model.onActionClick()
        advanceUntilIdle()

        verify {
            stateController.update(
                match<Transformer<StakingUiState>> {
                    it is SetConfirmationStateResetAssentTransformer
                },
            )
        }
        verify { messageSender.send(any()) }
        coVerify(exactly = 0) { mockTransactionSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN assent state AND no approval in progress WHEN onPrevClick THEN prev navigated and assent reset`() =
        runTest {
            val uiStateFlow = MutableStateFlow(initialUiState)
            every { stateController.uiState } returns uiStateFlow
            every {
                stakingOperationsFactory.createFeeLoader(
                    cryptoCurrencyStatus = any(),
                    userWallet = any(),
                    integration = any()
                )
            } returns mockk {
                coEvery {
                    getFee(any(), any(), any(), any())
                } just Runs
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            val assentUiState = mockk<StakingUiState>(relaxed = true) {
                every { currentStep } returns StakingStep.Confirmation
                every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                    every { innerState } returns InnerConfirmationStakingState.ASSENT
                    every { notifications } returns persistentListOf()
                }
            }
            uiStateFlow.value = assentUiState
            every { stateController.value } returns assentUiState
            advanceUntilIdle()

            clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)
            every { stateController.update(any<Transformer<StakingUiState>>()) } just Runs
            every { stateController.update(any<(StakingUiState) -> StakingUiState>()) } just Runs
            every { stateController.value } returns assentUiState

            model.onPrevClick()

            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> {
                        it is SetConfirmationStateResetAssentTransformer
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN in progress state WHEN onPrevClick THEN nothing happens`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockk {
            coEvery {
                getFee(any(), any(), any(), any())
            } just Runs
        }

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val inProgressUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.IN_PROGRESS
            }
        }
        uiStateFlow.value = inProgressUiState
        every { stateController.value } returns inProgressUiState
        advanceUntilIdle()

        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)

        model.onPrevClick()

        verify(exactly = 0) { stateController.update(any<Transformer<StakingUiState>>()) }
        verify(exactly = 0) { stateController.update(any<(StakingUiState) -> StakingUiState>()) }
        verify(exactly = 0) { stateController.updateAll(*anyVararg()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN completed state WHEN onPrevClick THEN onNextClick called`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every {
            stakingOperationsFactory.createFeeLoader(any(), any(), any())
        } returns mockk {
            coEvery {
                getFee(any(), any(), any(), any())
            } just Runs
        }

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val completedUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Confirmation
            every { confirmationState } returns mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { innerState } returns InnerConfirmationStakingState.COMPLETED
            }
        }
        uiStateFlow.value = completedUiState
        every { stateController.value } returns completedUiState
        advanceUntilIdle()

        clearMocks(stateController, answers = false, recordedCalls = true, verificationMarks = true)
        every { stateController.value } returns completedUiState
        every { stateController.update(any<(StakingUiState) -> StakingUiState>()) } just Runs
        every { stateController.clear() } just Runs

        model.onPrevClick()
        advanceUntilIdle()

        verify {
            stateController.update(any<(StakingUiState) -> StakingUiState>())
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN non-confirmation step WHEN onPrevClick THEN stakingStateRouter onPrevClick called`() = runTest {
        val uiStateFlow = MutableStateFlow(initialUiState)
        every { stateController.uiState } returns uiStateFlow
        every { stateController.value } returns initialUiState
        every { stateController.update(any<(StakingUiState) -> StakingUiState>()) } just Runs
        every { appRouter.pop(any()) } just Runs
        every { stateController.clear() } just Runs

        val amountUiState = mockk<StakingUiState>(relaxed = true) {
            every { currentStep } returns StakingStep.Amount
        }

        val model = createModel(testScope = this)
        advanceUntilIdle()

        uiStateFlow.value = amountUiState
        every { stateController.value } returns amountUiState
        every { stateController.uiState } returns MutableStateFlow(amountUiState)

        model.onPrevClick()

        verify { stateController.update(any<(StakingUiState) -> StakingUiState>()) }

        model.onDestroy()
    }

    @Test
    fun `WHEN onRefreshSwipe true THEN loading set and balanceUpdater partialUpdate called`() = runTest {
        val testAppScope = object : AppCoroutineScope,
            CoroutineScope by this {}

        val model = createModel(
            testScope = this,
            coroutineScope = testAppScope,
        )
        advanceUntilIdle()

        model.onRefreshSwipe(isRefreshing = true)
        advanceUntilIdle()

        verify {
            stateController.update(
                match<Transformer<StakingUiState>> {
                    it is SetInitialLoadingStateTransformer
                }
            )
        }
        coVerify { mockBalanceUpdater.partialUpdate() }

        model.onDestroy()
    }

    @Test
    fun `WHEN onInitialInfoBannerClick THEN analytics sent and url opened`() = runTest {
        every { innerRouter.openUrl(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onInitialInfoBannerClick()

        verify {
            analyticsEventHandler.send(match { it is StakingAnalyticsEvent.WhatIsStaking })
        }
        verify {
            innerRouter.openUrl("https://tangem.com/en/blog/post/how-to-stake-cryptocurrency/")
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onInfoClick THEN ShowInfoBottomSheetStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onInfoClick(InfoType.ANNUAL_PERCENTAGE_RATE)

        verify {
            stateController.update(
                match<Transformer<StakingUiState>> {
                    it is ShowInfoBottomSheetStateTransformer
                }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN empty preferredTargets WHEN onAmountEnterClick THEN noAvailableValidators alert sent`() = runTest {
        every { testYield.preferredValidators } returns emptyList()
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountEnterClick()

        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN non-empty preferredTargets WHEN onAmountEnterClick THEN validator reset and onNextClick called`() =
        runTest {
            every { stateController.value } returns initialUiState
            every { testYield.preferredValidators } returns listOf(mockk(relaxed = true))
            every { initialUiState.actionType } returns StakingActionCommonType.Enter(skipEnterAmount = false)
            every { testYield.args.enter.isPartialAmountDisabled } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onAmountEnterClick()
            advanceUntilIdle()

            verify {
                stateController.updateAll(
                    match { it is ValidatorSelectChangeTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `WHEN onAmountPasteTriggerDismiss THEN AmountPasteDismissStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountPasteTriggerDismiss()

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is AmountPasteDismissStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onMaxValueClick THEN analytics sent and AmountMaxValueStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onMaxValueClick()

        verify {
            analyticsEventHandler.send(
                match {
                    it is StakingAnalyticsEvent.ButtonMax
                }
            )
        }
        verify {
            stateController.update(
                match<Transformer<StakingUiState>> { it is AmountMaxValueStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onCurrencyChangeClick THEN analytics sent and AmountCurrencyChangeStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onCurrencyChangeClick(isFiat = true)

        verify {
            analyticsEventHandler.send(match { it is StakingAnalyticsEvent.AmountSelectCurrency })
        }
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is AmountCurrencyChangeStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN openValidators THEN analytics sent and step changed to Validators`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.openValidators()

        verify {
            analyticsEventHandler.send(
                match { it is StakingAnalyticsEvent.ButtonValidator },
            )
        }
        verify {
            stateController.update(any<(StakingUiState) -> StakingUiState>())
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onTargetSelect THEN analytics sent and ValidatorSelectChangeTransformer applied`() = runTest {
        val target: StakingTarget = mockk(relaxed = true) {
            every { name } returns "TestValidator"
        }

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onTargetSelect(target)

        verify {
            analyticsEventHandler.send(event = StakingAnalyticsEvent.ValidatorChosen("TestValidator"))
        }
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is ValidatorSelectChangeTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN RewardsRequirementsError AND minimumAmount WHEN openRewardsValidators THEN alert shown call`() =
        runTest {
            every { messageSender.send(any()) } just Runs

            val constraints = PendingActionConstraints(
                type = StakingActionType.CLAIM_REWARDS,
                amountArg = PendingAction.PendingActionArgs.Amount(
                    required = true,
                    minimum = BigDecimal.TEN,
                    maximum = null,
                ),
            )
            val yieldBalance = InnerYieldBalanceState.Data(
                integrationId = "test-integration",
                reward = YieldReward(
                    rewardsCrypto = "1.0",
                    rewardsFiat = "$1.00",
                    rewardBlockType = RewardBlockType.RewardsRequirementsError,
                    rewardConstraints = constraints,
                ),
                isActionable = true,
                balances = persistentListOf(),
            )
            val initialInfoState = mockk<StakingStates.InitialInfoState.Data>(relaxed = true) {
                every { this@mockk.yieldBalance } returns yieldBalance
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.initialInfoState } returns initialInfoState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.openRewardsValidators()

            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify { messageSender.send(any()) }
            verify(exactly = 0) { getActionRequirementAmountUseCase.invoke(any(), any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN RewardsRequirementsError WHEN openRewardsValidators THEN getActionRequirementAmountUseCase called`() =
        runTest {
            every { messageSender.send(any()) } just Runs
            every {
                getActionRequirementAmountUseCase.invoke(any(), any())
            } returns BigDecimal.ONE

            val yieldBalance = InnerYieldBalanceState.Data(
                integrationId = "test-integration",
                reward = YieldReward(
                    rewardsCrypto = "1.0",
                    rewardsFiat = "$1.00",
                    rewardBlockType = RewardBlockType.RewardsRequirementsError,
                    rewardConstraints = null,
                ),
                isActionable = true,
                balances = persistentListOf(),
            )
            val initialInfoState = mockk<StakingStates.InitialInfoState.Data>(relaxed = true) {
                every { this@mockk.yieldBalance } returns yieldBalance
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.initialInfoState } returns initialInfoState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.openRewardsValidators()

            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify {
                getActionRequirementAmountUseCase.invoke(
                    integrationId = "test-integration",
                    actionType = StakingActionType.CLAIM_REWARDS
                )
            }
            verify { messageSender.send(any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN no RewardsRequirementsError AND single reward WHEN openRewardsValidators THEN onActiveStake called`() =
        runTest {
            every { stateController.value } returns initialUiState
            every { messageSender.send(any()) } just Runs

            val singleReward: BalanceState = mockk(relaxed = true) {
                every { pendingActions } returns persistentListOf()
            }
            val yieldBalance = InnerYieldBalanceState.Data(
                integrationId = "test-integration",
                reward = YieldReward(
                    rewardsCrypto = "1.0",
                    rewardsFiat = "$1.00",
                    rewardBlockType = RewardBlockType.Rewards,
                    rewardConstraints = null,
                ),
                isActionable = true,
                balances = persistentListOf(),
            )
            val initialInfoState = mockk<StakingStates.InitialInfoState.Data>(relaxed = true) {
                every { this@mockk.yieldBalance } returns yieldBalance
            }
            val rewardsValidatorsState = mockk<StakingStates.RewardsValidatorsState.Data>(relaxed = true) {
                every { rewards } returns persistentListOf(singleReward)
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.initialInfoState } returns initialInfoState
                every { this@mockk.rewardsValidatorsState } returns rewardsValidatorsState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.openRewardsValidators()
            advanceUntilIdle()

            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            // onActiveStake path — ButtonValidator analytics should NOT be sent
            verify(exactly = 0) {
                analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonValidator })
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN no RewardsRequirementsError AND rewards WHEN openRewardsValidators THEN showRewardsValidators called`() =
        runTest {
            every { stateController.value } returns initialUiState

            val reward1: BalanceState = mockk(relaxed = true)
            val reward2: BalanceState = mockk(relaxed = true)
            val yieldBalance = InnerYieldBalanceState.Data(
                integrationId = "test-integration",
                reward = YieldReward(
                    rewardsCrypto = "2.0",
                    rewardsFiat = "$2.00",
                    rewardBlockType = RewardBlockType.Rewards,
                    rewardConstraints = null,
                ),
                isActionable = true,
                balances = persistentListOf(),
            )
            val initialInfoState = mockk<StakingStates.InitialInfoState.Data>(relaxed = true) {
                every { this@mockk.yieldBalance } returns yieldBalance
            }
            val rewardsValidatorsState = mockk<StakingStates.RewardsValidatorsState.Data>(relaxed = true) {
                every { rewards } returns persistentListOf(reward1, reward2)
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.initialInfoState } returns initialInfoState
                every { this@mockk.rewardsValidatorsState } returns rewardsValidatorsState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.openRewardsValidators()

            verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonRewards }) }
            verify {
                analyticsEventHandler.send(
                    event = StakingAnalyticsEvent.ButtonValidator(source = StakeScreenSource.Info)
                )
            }
            verify {
                stateController.update(any<(StakingUiState) -> StakingUiState>())
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN single pending action WHEN onActiveStake THEN prepareForConfirmation and onNextClick called`() =
        runTest {
            every { stateController.value } returns initialUiState

            val singleAction = PendingAction(
                type = StakingActionType.CLAIM_REWARDS,
                passthrough = "test",
                args = null,
            )
            val activeStake: BalanceState = mockk(relaxed = true) {
                every { type } returns BalanceType.STAKED
                every { pendingActions } returns persistentListOf(singleAction)
                every { target } returns null
                every { cryptoValue } returns "100"
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onActiveStake(activeStake)
            advanceUntilIdle()

            // prepareForConfirmation calls updateAll with 4 transformers
            verify {
                stateController.updateAll(
                    match { it is SetConfirmationStateInitTransformer },
                    match { it is ValidatorSelectChangeTransformer },
                    match { it is SetAmountDataTransformer },
                    any(),
                )
            }
            // onNextClick updates step
            verify { stateController.update(any<(StakingUiState) -> StakingUiState>()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN multiple pending actions WHEN onActiveStake THEN ShowActionSelectorBottomSheetTransformer applied`() =
        runTest {
            every { stateController.value } returns initialUiState

            val action1 = PendingAction(
                type = StakingActionType.CLAIM_REWARDS,
                passthrough = "test1",
                args = null,
            )
            val action2 = PendingAction(
                type = StakingActionType.WITHDRAW,
                passthrough = "test2",
                args = null,
            )
            val activeStake: BalanceState = mockk(relaxed = true) {
                every { type } returns BalanceType.STAKED
                every { pendingActions } returns persistentListOf(action1, action2)
                every { target } returns null
                every { cryptoValue } returns "100"
            }

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onActiveStake(activeStake)

            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is ShowActionSelectorBottomSheetTransformer },
                )
            }
            // prepareForConfirmation should NOT have been called
            verify(exactly = 0) {
                stateController.updateAll(
                    match { it is SetConfirmationStateInitTransformer },
                    any(), any(), any(),
                )
            }

            model.onDestroy()
        }

    @Test
    fun `WHEN onActiveStakeAnalytic THEN ButtonValidator analytics sent`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onActiveStakeAnalytic()

        verify {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.ButtonValidator(
                    source = StakeScreenSource.Info,
                )
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN gasless approval enabled WHEN showApprovalBottomSheet THEN approvalSlotNavigation activated`() =
        runTest {
            every { giveApprovalFeatureToggles.isGaslessApprovalEnabled } returns true

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.showApprovalBottomSheet()

            verify(exactly = 0) {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowApprovalBottomSheetTransformer }
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN gasless disabled WHEN showApprovalBottomSheet THEN ShowApprovalBottomSheetTransformer applied`() =
        runTest {
            every { giveApprovalFeatureToggles.isGaslessApprovalEnabled } returns false

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.showApprovalBottomSheet()

            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowApprovalBottomSheetTransformer }
                )
            }

            model.onDestroy()
        }

    @Test
    fun `WHEN onApproveTypeChange THEN SetApprovalBottomSheetTypeChangeTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onApproveTypeChange(ApproveType.LIMITED)

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is SetApprovalBottomSheetTypeChangeTransformer },
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN approval needed WHEN onApprovalClick THEN in progress set and createApprovalTransaction called`() =
        runTest {
            val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"
            val expectedNetwork = mockk<Network> {
                every { name } returns "KEK"
            }
            val testToken: CryptoCurrency.Token = mockk(relaxed = true) {
                every { network } returns expectedNetwork
            }
            val testCryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
                every { currency } returns testToken
            }
            val testAccountCurrencyStatus = mockk<AccountCryptoCurrencyStatus> {
                every { component1() } returns mockk(relaxed = true)
                every { component2() } returns testCryptoCurrencyStatus
            }
            every {
                getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
            } returns flowOf(testAccountCurrencyStatus)
            coEvery {
                getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Left(mockk())

            // Setup stakingApproval = Needed
            mockkObject(StakingIntegrationID.Companion)
            every {
                StakingIntegrationID.create(any())
            } returns mockk {
                every { approval } returns StakingApproval.Needed(spenderAddress)
            }
            coEvery {
                getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
            } returns Either.Right(BigDecimal.TEN)

            every {
                stakingOperationsFactory.createFeeLoader(
                    cryptoCurrencyStatus = any(),
                    userWallet = any(),
                    integration = any()
                )
            } returns mockk<StakingFeeLoader> {
                coEvery {
                    getFee(
                        onStakingFee = any(),
                        onStakingFeeError = any(),
                        onApprovalFee = any(),
                        onFeeError = any()
                    )
                } just Runs
            }
            val expectedApprovalTx = Either.Right<TransactionData.Uncompiled>(mockk(relaxed = true))
            coEvery {
                createApprovalTransactionUseCase.invoke(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = any(),
                    fee = any(),
                    contractAddress = any(),
                    spenderAddress = any(),
                )
            } returns expectedApprovalTx
            coEvery {
                sendTransactionUseCase(any(), any(), any())
            } returns Either.Right("txHash")
            every { vibratorHapticManager.performOneTime(any()) } just Runs

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Now override stateController.value with confirmation state after cryptoCurrencyStatus is initialized
            val testFee: Fee.Common = mockk(relaxed = true)
            val confirmationState = mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
                every { feeState } returns mockk<FeeState.Content>(relaxed = true) {
                    every { fee } returns testFee
                }
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.confirmationState } returns confirmationState
                every { bottomSheetConfig } returns null
            }
            every { stateController.value } returns uiState

            model.onApprovalClick()
            advanceUntilIdle()

            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> {
                        it is SetApprovalBottomSheetInProgressTransformer
                    },
                )
            }
            coVerify {
                sendTransactionUseCase(
                    txData = expectedApprovalTx.value,
                    userWallet = testUserWallet,
                    network = expectedNetwork,
                )
            }

            model.onDestroy()
            unmockkObject(StakingIntegrationID.Companion)
        }

    @Test
    fun `WHEN onAmountReduceByClick THEN AmountReduceByStateTransformer and DismissNotification applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountReduceByClick(
            reduceAmountBy = BigDecimal.ONE,
            reduceAmountByDiff = BigDecimal.TEN,
            notification = NotificationUM::class.java,
        )

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is AmountReduceByStateTransformer }
            )
        }
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is DismissStakingNotificationsStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onAmountReduceToClick THEN AmountReduceToStateTransformer and DismissNotification applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountReduceToClick(
            reduceAmountTo = BigDecimal.ONE,
            notification = NotificationUM::class.java,
        )

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is AmountReduceToStateTransformer }
            )
        }
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> {
                    it is DismissStakingNotificationsStateTransformer
                }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onNotificationCancel THEN DismissStakingNotificationsStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onNotificationCancel(NotificationUM::class.java)

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is DismissStakingNotificationsStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN txUrl not null WHEN onExploreClick THEN analytics sent and url opened`() = runTest {
        val txUrl = "https://explorer.solana.com/tx/abc123"
        val transactionDoneState = TransactionDoneState.Content(
            timestamp = 1000L,
            txUrl = txUrl,
        )
        val confirmationState = mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
            every { this@mockk.transactionDoneState } returns transactionDoneState
        }
        val uiState = mockk<StakingUiState>(relaxed = true) {
            every { this@mockk.confirmationState } returns confirmationState
        }
        every { stateController.uiState } returns MutableStateFlow(uiState)
        every { innerRouter.openUrl(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onExploreClick()

        verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonExplore }) }
        verify { innerRouter.openUrl(txUrl) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN txUrl not null WHEN onShareClick THEN analytics sent and shareManager called`() = runTest {
        val txUrl = "https://explorer.solana.com/tx/abc123"
        val transactionDoneState = TransactionDoneState.Content(
            timestamp = 1000L,
            txUrl = txUrl,
        )
        val confirmationState = mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
            every { this@mockk.transactionDoneState } returns transactionDoneState
        }
        val uiState = mockk<StakingUiState>(relaxed = true) {
            every { this@mockk.confirmationState } returns confirmationState
        }
        every { stateController.uiState } returns MutableStateFlow(uiState)
        every { vibratorHapticManager.performOneTime(any()) } just Runs
        every { shareManager.shareText(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onShareClick()

        verify { analyticsEventHandler.send(match { it is StakingAnalyticsEvent.ButtonShare }) }
        verify { vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click) }
        verify { shareManager.shareText(txUrl) }

        model.onDestroy()
    }

    @Test
    fun `WHEN onFailedTxEmailClick THEN analytics sent and sendFeedbackEmail called`() = runTest {
        coEvery { getWalletMetaInfoUseCase(userWalletId = any()) } returns Either.Right(mockk(relaxed = true))
        every { saveBlockchainErrorUseCase(error = any()) } just Runs
        coEvery { sendFeedbackEmailUseCase(type = any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onFailedTxEmailClick("test error")
        advanceUntilIdle()

        verify { analyticsEventHandler.send(match { it is Basic.ButtonSupport }) }
        coVerify { sendFeedbackEmailUseCase(match { it is FeedbackEmailType.StakingProblem }) }

        model.onDestroy()
    }

    @Test
    fun `WHEN openTokenDetails THEN innerRouter openTokenDetails called`() = runTest {
        every { innerRouter.openTokenDetails(any(), any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        val currency: CryptoCurrency = mockk(relaxed = true)
        model.openTokenDetails(currency)

        verify { innerRouter.openTokenDetails(testUserWalletId, currency) }

        model.onDestroy()
    }

    @Test
    fun `WHEN showPrimaryClickAlert THEN messageSender sends alert`() = runTest {
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.showPrimaryClickAlert()

        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `WHEN onOpenLearnMoreAboutApproveClick THEN urlOpener opens approve url`() = runTest {
        every { urlOpener.openUrl(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onOpenLearnMoreAboutApproveClick()

        verify { urlOpener.openUrl("https://tangem.com/en/blog/post/give-revoke-permission/") }

        model.onDestroy()
    }

    @Test
    fun `GIVEN getFee returns Left WHEN onActivateTonAccountNotificationClick THEN fee error transformer applied`() =
        runTest {
            val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
            every { testCryptoCurrencyStatus.currency } returns mockk {
                every { id } returns CryptoCurrency.ID.fromValue("coin⟨ethereum→-1843072795⟩ethereum")
                every { symbol } returns "KEK"
                every { network } returns mockk()
                every { decimals } returns 2
            }
            every {
                getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
            } returns flowOf(testAccountCurrencyStatus)
            coEvery {
                getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Left(mockk())
            coEvery {
                getNetworkAddressesUseCase.invokeSync(
                    userWalletId = any(),
                    network = any<Network>()
                )
            } returns listOf(mockk(relaxed = true) { every { address } returns "TON_ADDRESS" })
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), memo = any(), destination = any(),
                    userWalletId = any(), network = any(),
                )
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns Either.Left(mockk(relaxed = true))

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddressScreen("KEK"))
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowTonInitializeBottomSheetTransformer }
                )
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> {
                        it is SetFeeErrorToTonInitializeBottomSheetTransformer
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN getFee returns Right WHEN onActivateTonAccountNotificationClick THEN fee transformer applied`() =
        runTest {
            val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
            every { testCryptoCurrencyStatus.currency } returns mockk {
                every { id } returns CryptoCurrency.ID.fromValue("coin⟨ethereum→-1843072795⟩ethereum")
                every { symbol } returns "SHMEK"
                every { network } returns mockk()
                every { decimals } returns 2
            }
            every {
                getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
            } returns flowOf(testAccountCurrencyStatus)
            coEvery {
                getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Left(mockk())
            coEvery {
                getNetworkAddressesUseCase.invokeSync(
                    userWalletId = any(),
                    network = any<Network>()
                )
            } returns listOf(mockk(relaxed = true) { every { address } returns "TON_ADDRESS" })
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), memo = any(), destination = any(),
                    userWalletId = any(), network = any(),
                )
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns Either.Right(mockk(relaxed = true))

            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddressScreen("SHMEK"))
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is ShowTonInitializeBottomSheetTransformer }
                )
            }
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> {
                        it is SetFeeToTonInitializeBottomSheetTransformer
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `WHEN onActivateTonAccountNotificationShow THEN UninitializedAddress analytics sent with token`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.currency.symbol } returns "TON"
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns flowOf(testAccountCurrencyStatus)
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onActivateTonAccountNotificationShow()

        verify {
            analyticsEventHandler.send(StakingAnalyticsEvent.UninitializedAddress(token = "TON"))
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onNotEnoughFeeNotificationShow THEN NotEnoughFee analytics sent with token`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.currency.symbol } returns "SOL"
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns flowOf(testAccountCurrencyStatus)
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())

        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onNotEnoughFeeNotificationShow()

        verify {
            analyticsEventHandler.send(StakingAnalyticsEvent.NotEnoughFee(token = "SOL"))
        }

        model.onDestroy()
    }

    @Test
    fun `GIVEN sendTransaction returns Left WHEN onActivateTonAccountClick THEN error dialog sent`() = runTest {
        val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
        every { testCryptoCurrencyStatus.currency.symbol } returns "TON"
        every {
            getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
        } returns flowOf(testAccountCurrencyStatus)
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
        } returns Either.Left(mockk())
        coEvery {
            getNetworkAddressesUseCase.invokeSync(
                userWalletId = any(),
                network = any<Network>()
            )
        } returns listOf(mockk(relaxed = true) { every { address } returns "TON_ADDRESS" })
        coEvery {
            createTransferTransactionUseCase(
                amount = any(), memo = any(), destination = any(),
                userWalletId = any(), network = any(),
            )
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
        } returns Either.Right(mockk(relaxed = true))
        coEvery {
            sendTransactionUseCase(any(), any(), any())
        } returns Either.Left(mockk(relaxed = true))
        every { messageSender.send(any()) } just Runs

        val model = createModel(testScope = this)
        advanceUntilIdle()

        // First populate tonAccountInitializeTransaction
        model.onActivateTonAccountNotificationClick()
        advanceUntilIdle()

        model.onActivateTonAccountClick()
        advanceUntilIdle()

        verify {
            analyticsEventHandler.send(StakingAnalyticsEvent.ButtonActivate(token = "TON"))
        }
        verify { messageSender.send(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN sendTransaction returns Right WHEN onActivateTonAccountClick THEN complete transformer applied`() =
        runTest {
            val (testCryptoCurrencyStatus, testAccountCurrencyStatus) = createMockedAccountCurrencyStatus()
            every { testCryptoCurrencyStatus.currency.symbol } returns "TON"
            every {
                getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency)
            } returns flowOf(testAccountCurrencyStatus)
            coEvery {
                getFeePaidCryptoCurrencyStatusSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getMinimumTransactionAmountSyncUseCase(testUserWalletId, testCryptoCurrencyStatus)
            } returns Either.Left(mockk())
            val mockBalanceUpdater: StakingBalanceUpdater = mockk {
                coEvery { partialUpdate() } just Runs
                coEvery { partialUpdateWithDelay() } just Runs
            }
            every {
                stakingBalanceUpdater.create(any(), any(), any())
            } returns mockBalanceUpdater
            coEvery {
                getNetworkAddressesUseCase.invokeSync(
                    userWalletId = any(),
                    network = any<Network>()
                )
            } returns listOf(mockk(relaxed = true) { every { address } returns "TON_ADDRESS" })
            coEvery {
                createTransferTransactionUseCase(
                    amount = any(), memo = any(), destination = any(),
                    userWalletId = any(), network = any(),
                )
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns Either.Right(mockk(relaxed = true))
            coEvery {
                sendTransactionUseCase(any(), any(), any())
            } returns Either.Right("txHash")

            val model = createModel(testScope = this)
            advanceUntilIdle()

            // First populate tonAccountInitializeTransaction
            model.onActivateTonAccountNotificationClick()
            advanceUntilIdle()

            model.onActivateTonAccountClick()
            advanceUntilIdle()

            verify {
                analyticsEventHandler.send(StakingAnalyticsEvent.ButtonActivate(token = "TON"))
            }
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is CompleteInitializeBottomSheetTransformer },
                )
            }
            coVerify { mockBalanceUpdater.partialUpdateWithDelay() }

            model.onDestroy()
        }

    @Test
    fun `WHEN onAmountReduceByFeeClick THEN AmountReduceByStateTransformer and DismissNotification applied`() =
        runTest {
            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onAmountReduceByFeeClick(
                reduceAmount = BigDecimal.ONE,
                notification = NotificationUM::class.java,
            )

            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is AmountReduceByStateTransformer }
                )
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> {
                        it is DismissStakingNotificationsStateTransformer
                    }
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN approval needed AND amountState data WHEN getApprovalParams THEN returns non-null params`() = runTest {
        val spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908"

        mockkObject(StakingIntegrationID.Companion)
        try {
            every {
                StakingIntegrationID.create(any())
            } returns mockk {
                every { approval } returns StakingApproval.Needed(spenderAddress)
            }
            coEvery {
                getAllowanceUseCase(testUserWalletId, any(), spenderAddress)
            } returns Either.Right(BigDecimal.TEN)

            val amountState = mockk<AmountState.Data>(relaxed = true) {
                every { amountTextField.value } returns "100"
            }
            val uiState = mockk<StakingUiState>(relaxed = true) {
                every { this@mockk.amountState } returns amountState
            }
            every { stateController.value } returns uiState

            val model = createModel(testScope = this)
            advanceUntilIdle()

            val result = model.getApprovalParams()

            assert(result != null) { "Expected non-null GiveApprovalComponent.Params" }
            assert(result!!.spenderAddress == spenderAddress) {
                "Expected spenderAddress=$spenderAddress, got=${result.spenderAddress}"
            }

            model.onDestroy()
        } finally {
            unmockkObject(StakingIntegrationID.Companion)
        }
    }

    @Suppress("LongParameterList")
    private fun createModel(
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

    private fun createMockedAccountCurrencyStatus(): Pair<CryptoCurrencyStatus, AccountCryptoCurrencyStatus> {
        val testCryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true)
        val testAccountCurrencyStatus = mockk<AccountCryptoCurrencyStatus> {
            every { component1() } returns mockk(relaxed = true)
            every { component2() } returns testCryptoCurrencyStatus
        }
        return testCryptoCurrencyStatus to testAccountCurrencyStatus
    }
}