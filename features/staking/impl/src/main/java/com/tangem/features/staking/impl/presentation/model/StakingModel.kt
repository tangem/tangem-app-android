package com.tangem.features.staking.impl.presentation.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionBottomSheetConfig
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.staking.*
import com.tangem.domain.staking.analytics.StakeScreenSource
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetAllowanceUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.analytics.StakingParamsInterceptor
import com.tangem.features.staking.impl.analytics.utils.StakingAnalyticSender
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.events.StakingAlertUM
import com.tangem.features.staking.impl.presentation.state.events.StakingEvent
import com.tangem.features.staking.impl.presentation.state.events.StakingEventFactory
import com.tangem.features.staking.impl.presentation.state.helpers.StakingBalanceUpdater
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeTransactionLoader
import com.tangem.features.staking.impl.presentation.state.helpers.StakingTransactionSender
import com.tangem.features.staking.impl.presentation.state.transformers.*
import com.tangem.features.staking.impl.presentation.state.transformers.amount.*
import com.tangem.features.staking.impl.presentation.state.transformers.approval.*
import com.tangem.features.staking.impl.presentation.state.transformers.confirmation.SetUpdatedAllowanceTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.AddStakingNotificationsTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.DismissStakingNotificationsStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.features.staking.impl.presentation.state.utils.checkAndCalculateSubtractedAmount
import com.tangem.features.staking.impl.presentation.state.utils.isSingleAction
import com.tangem.features.staking.impl.presentation.state.utils.withStubUnstakeAction
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import com.tangem.utils.extensions.isSingleItem
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LargeClass", "TooManyFunctions", "LongParameterList")
@Stable
@ModelScoped
internal class StakingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val stateController: StakingStateController,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getAllowanceUseCase: GetAllowanceUseCase,
    private val isApproveNeededUseCase: IsApproveNeededUseCase,
    private val vibratorHapticManager: VibratorHapticManager,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val isAnyTokenStakedUseCase: IsAnyTokenStakedUseCase,
    private val invalidatePendingTransactionsUseCase: InvalidatePendingTransactionsUseCase,
    private val stakingTransactionLoader: StakingTransactionSender.Factory,
    private val stakingFeeTransactionLoader: StakingFeeTransactionLoader.Factory,
    private val stakingBalanceUpdater: StakingBalanceUpdater.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getActionsUseCase: GetActionsUseCase,
    private val getYieldUseCase: GetYieldUseCase,
    private val checkAccountInitializedUseCase: CheckAccountInitializedUseCase,
    private val paramsInterceptorHolder: ParamsInterceptorHolder,
    private val shareManager: ShareManager,
    @DelayedWork private val coroutineScope: CoroutineScope,
    private val innerRouter: InnerStakingRouter,
    appRouter: AppRouter,
) : Model(), StakingClickIntents {

    val uiState: StateFlow<StakingUiState> = stateController.uiState
    val value: StakingUiState get() = uiState.value

    private val params = paramsContainer.require<StakingComponent.Params>()

    private var stakingStateRouter: StakingStateRouter = StakingStateRouter(
        appRouter = appRouter,
        stateController = stateController,
        analyticsEventsHandler = analyticsEventHandler,
    )

    private val cryptoCurrencyId: CryptoCurrency.ID = params.cryptoCurrencyId
    private val userWalletId: UserWalletId = params.userWalletId
    private val yield: Yield = runBlocking {
        getYieldUseCase(params.yieldId).getOrElse {
            error("yield must be not null")
        }
    }

    private var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    private var processingActions: List<StakingAction> = emptyList()
    private var feeCryptoCurrencyStatus: CryptoCurrencyStatus? = null
    private var minimumTransactionAmount: EnterAmountBoundary? = null

    private var userWallet: UserWallet by Delegates.notNull()
    private var appCurrency: AppCurrency by Delegates.notNull()

    private val balancesToShow: List<BalanceItem>
        get() {
            val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data
            return invalidatePendingTransactionsUseCase(
                balanceItems = yieldBalance?.balance?.items ?: emptyList(),
                processingActions = processingActions,
            ).getOrElse { emptyList() }
        }

    private var isInitialInfoAnalyticSent: Boolean = false

    private val balanceUpdater by lazy(LazyThreadSafetyMode.NONE) {
        stakingBalanceUpdater.create(
            cryptoCurrencyStatus,
            userWallet,
            yield,
        )
    }

    private val feeLoader by lazy(LazyThreadSafetyMode.NONE) {
        stakingFeeTransactionLoader.create(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWallet = userWallet,
            yield = yield,
        )
    }

    private val transactionSender by lazy(LazyThreadSafetyMode.NONE) {
        stakingTransactionLoader.create(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWallet = userWallet,
            yield = yield,
            isAmountSubtractAvailable = isAmountSubtractAvailable,
        )
    }

    private val stakingEventFactory: StakingEventFactory
        get() = StakingEventFactory(
            stateController = stateController,
            popBackStack = ::onBackClick,
            onFailedTxEmailClick = ::onFailedTxEmailClick,
        )

    private val stakingAnalyticSender = StakingAnalyticSender(
        analyticsEventHandler = analyticsEventHandler,
    )

    private var stakingApproval: StakingApproval = StakingApproval.Empty
    private var stakingAllowance: BigDecimal = BigDecimal.ZERO
    private var isAmountSubtractAvailable: Boolean = false
    private var isAnyTokenStaked: Boolean = false
    private val allowanceTaskScheduler = SingleTaskScheduler<BigDecimal>()

    private val transactionsInProgress: CopyOnWriteArrayList<StakingTransaction> = CopyOnWriteArrayList()

    private var actionsJobHolder: JobHolder = JobHolder()
    private var approvalJobHolder: JobHolder = JobHolder()
    private var feeJobHolder: JobHolder = JobHolder()
    private var sendTransactionJobHolder = JobHolder()
    private var stepChangesJobHolder = JobHolder()

    init {
        subscribeOnSelectedAppCurrency()
        subscribeOnBalanceHiding()
        subscribeOnCurrencyStatusUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        paramsInterceptorHolder.removeParamsInterceptor(StakingParamsInterceptor.ID)
        approvalJobHolder.cancel()
        feeJobHolder.cancel()
        sendTransactionJobHolder.cancel()
        stepChangesJobHolder.cancel()
    }

    override fun onBackClick() {
        stakingStateRouter.onBackClick()
    }

    override fun onNextClick(balanceState: BalanceState?) {
        modelScope.launch {
            val isInitialInfoStep = value.currentStep == StakingStep.InitialInfo
            val noBalanceState = balanceState == null
            val noYieldBalanceData = cryptoCurrencyStatus.value.yieldBalance !is YieldBalance.Data
            val isAccountInitialized = checkAccountInitializedUseCase.invoke(
                userWalletId = userWalletId,
                network = cryptoCurrencyStatus.currency.network,
            ).getOrElse {
                Timber.e(it)
                false
            }

            when {
                isInitialInfoStep && noBalanceState && yield.allValidatorsFull && noYieldBalanceData -> {
                    stakingEventFactory.createStakingValidatorsUnavailableAlert()
                    return@launch
                }
                isInitialInfoStep && noBalanceState && !isAccountInitialized -> {
                    stakingEventFactory.createInitializeAccountAlert()
                    return@launch
                }
                isInitialInfoStep && noBalanceState -> {
                    val list = buildList {
                        SetConfirmationStateInitTransformer(
                            isEnter = true,
                            isExplicitExit = false,
                            balanceState = null,
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                            stakingApproval = stakingApproval,
                            stakingAllowance = stakingAllowance,
                            yieldArgs = yield.args,
                        ).let(::add)
                        if (yield.args.enter.isPartialAmountDisabled) {
                            ValidatorSelectChangeTransformer(
                                selectedValidator = yield.preferredValidators.firstOrNull(),
                                yield = yield,
                            ).let(::add)
                            SetAmountDataTransformer(
                                clickIntents = this@StakingModel,
                                cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                                userWalletProvider = Provider { userWallet },
                                appCurrencyProvider = Provider { appCurrency },
                            ).let(::add)
                            AmountMaxValueStateTransformer(
                                cryptoCurrencyStatus = cryptoCurrencyStatus,
                                minimumTransactionAmount = minimumTransactionAmount,
                                actionType = uiState.value.actionType,
                                yield = yield,
                            ).let(::add)
                        }
                    }
                    stateController.updateAll(*list.toTypedArray())
                }
            }
            stakingStateRouter.onNextClick()
        }
    }

    override fun getFee() {
        stateController.update(
            SetConfirmationStateLoadingTransformer(
                yield = yield,
                appCurrency = appCurrency,
                cryptoCurrency = cryptoCurrencyStatus.currency,
            ),
        )
        modelScope.launch {
            feeLoader.getFee(
                onStakingFee = { gasEstimate ->
                    stateController.update(
                        SetConfirmationStateAssentTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = gasEstimate,
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                        ),
                    )
                    updateNotifications()
                },
                onStakingFeeError = { stakingFeeError ->
                    stateController.update(AddStakingErrorTransformer)
                    updateNotifications(stakingError = stakingFeeError)
                },
                onFeeError = { error ->
                    analyticsEventHandler.send(StakingAnalyticsEvent.TransactionError)
                    stateController.update(AddStakingErrorTransformer)
                    updateNotifications(error)
                },
                onApprovalFee = { fee ->
                    stateController.update(
                        SetConfirmationStateAssentApprovalTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = fee,
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                        ),
                    )
                    updateNotifications()
                },
            )
        }.saveIn(feeJobHolder)
    }

    override fun onActionClick() {
        if (isAssentState()) {
            modelScope.launch {
                stakingAnalyticSender.sendTransactionStakingClickedAnalytics(value)
                stateController.update(SetConfirmationStateInProgressTransformer())
                transactionSender.constructAndSendTransactions(
                    onConstructSuccess = { constructedTransactions ->
                        transactionsInProgress.addAll(constructedTransactions)
                    },
                    onConstructError = { error ->
                        stakingEventFactory.createStakingErrorAlert(error)
                        stateController.update(SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus))
                    },
                    onSendSuccess = { txUrl ->
                        stakingAnalyticSender.sendTransactionStakingAnalytics(stateController.value)
                        transactionsInProgress.clear()
                        stateController.update(SetConfirmationStateCompletedTransformer(txUrl, cryptoCurrencyStatus))
                    },
                    onSendError = { error ->
                        analyticsEventHandler.send(StakingAnalyticsEvent.TransactionError)
                        stakingEventFactory.createSendTransactionErrorAlert(error)
                        stateController.update(SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus))
                    },
                    onFeeIncreased = { increasedFee ->
                        stateController.updateAll(
                            SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus),
                            SetConfirmationStateAssentTransformer(
                                appCurrencyProvider = Provider { appCurrency },
                                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                                fee = increasedFee,
                                cryptoCurrencyStatus = cryptoCurrencyStatus,
                            ),
                        )
                        stateController.updateEvent(
                            StakingEvent.ShowAlert(
                                StakingAlertUM.FeeIncreased(stateController::dismissAlert),
                            ),
                        )
                        updateNotifications()
                    },
                )
            }.saveIn(sendTransactionJobHolder)
        }
    }

    override fun onPrevClick() {
        if (value.currentStep == StakingStep.Confirmation) {
            val confirmationState = value.confirmationState as? StakingStates.ConfirmationState.Data
            when (confirmationState?.innerState) {
                InnerConfirmationStakingState.ASSENT -> {
                    val isApprovalInProgress = confirmationState.notifications.any {
                        it is StakingNotification.Warning.TransactionInProgress
                    }
                    if (!isApprovalInProgress) {
                        stakingStateRouter.onPrevClick()
                        stateController.update(
                            SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus = cryptoCurrencyStatus),
                        )
                    }
                }
                null,
                InnerConfirmationStakingState.IN_PROGRESS,
                -> {
                    // do nothing while transaction is in progress
                }
                InnerConfirmationStakingState.COMPLETED -> {
                    onNextClick()
                }
            }
        } else {
            stakingStateRouter.onPrevClick()
        }
    }

    override fun onRefreshSwipe(isRefreshing: Boolean) {
        stateController.update(SetInitialLoadingStateTransformer(isRefreshing))
        coroutineScope.launch {
            balanceUpdater.partialUpdate()
        }.invokeOnCompletion {
            stateController.update(SetInitialLoadingStateTransformer(false))
        }
    }

    override fun onInitialInfoBannerClick() {
        analyticsEventHandler.send(StakingAnalyticsEvent.WhatIsStaking)
        innerRouter.openUrl(WHAT_IS_STAKING_ARTICLE_URL)
    }

    override fun onInfoClick(infoType: InfoType) {
        stateController.update(
            ShowInfoBottomSheetStateTransformer(infoType) {
                stateController.update(DismissBottomSheetStateTransformer)
            },
        )
    }

    override fun onAmountEnterClick() {
        if (yield.preferredValidators.isEmpty()) {
            stateController.updateEvent(
                StakingEvent.ShowAlert(StakingAlertUM.NoAvailableValidators),
            )
        } else {
            if (uiState.value.actionType is StakingActionCommonType.Enter) {
                stateController.updateAll(
                    ValidatorSelectChangeTransformer(
                        selectedValidator = null,
                        yield = yield,
                    ),
                )
            }
            onNextClick()
        }
    }

    override fun onAmountValueChange(value: String) {
        stateController.update(
            AmountChangeStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                minimumTransactionAmount = minimumTransactionAmount,
                value = value,
                yield = yield,
            ),
        )
    }

    override fun onAmountPasteTriggerDismiss() {
        stateController.update(AmountPasteDismissStateTransformer())
    }

    override fun onMaxValueClick() {
        analyticsEventHandler.send(StakingAnalyticsEvent.ButtonMax)
        stateController.update(
            AmountMaxValueStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                minimumTransactionAmount = minimumTransactionAmount,
                actionType = uiState.value.actionType,
                yield = yield,
            ),
        )
    }

    override fun onCurrencyChangeClick(isFiat: Boolean) {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.AmountSelectCurrency(isFiat),
        )
        stateController.update(AmountCurrencyChangeStateTransformer(cryptoCurrencyStatus, isFiat))
    }

    override fun openValidators() {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.ButtonValidator(
                source = StakeScreenSource.Confirmation,
            ),
        )
        stakingStateRouter.showValidators()
    }

    override fun onValidatorSelect(validator: Yield.Validator) {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.ValidatorChosen(
                validator = validator.name,
            ),
        )
        stateController.update(
            ValidatorSelectChangeTransformer(
                selectedValidator = validator,
                yield = yield,
            ),
        )
    }

    override fun openRewardsValidators() {
        analyticsEventHandler.send(StakingAnalyticsEvent.ButtonRewards)
        val rewardsValidators =
            stateController.value.rewardsValidatorsState as? StakingStates.RewardsValidatorsState.Data
        val rewards = rewardsValidators?.rewards
        if (rewards != null && rewards.isSingleItem()) {
            onActiveStake(rewards.first())
        } else {
            analyticsEventHandler.send(
                StakingAnalyticsEvent.ButtonValidator(
                    source = StakeScreenSource.Info,
                ),
            )
            stateController.update {
                value.copy(actionType = StakingActionCommonType.Pending.Rewards)
            }
            stakingStateRouter.showRewardsValidators()
        }
    }

    override fun onActiveStake(activeStake: BalanceState) {
        val networkId = cryptoCurrencyStatus.currency.network.id.value
        if (isSingleAction(networkId, activeStake)) {
            prepareForConfirmation(
                balanceType = activeStake.type,
                pendingActions = activeStake.pendingActions,
                balanceState = activeStake,
                validator = activeStake.validator,
                amountValue = activeStake.cryptoValue,
            )
            onNextClick(activeStake)
        } else {
            stateController.update(
                ShowActionSelectorBottomSheetTransformer(
                    pendingActions = withStubUnstakeAction(networkId, activeStake),
                    onActionSelect = { action ->
                        prepareForConfirmation(
                            balanceType = activeStake.type,
                            pendingAction = action,
                            balanceState = activeStake,
                            validator = activeStake.validator,
                            amountValue = activeStake.cryptoValue,
                        )
                        stateController.update(DismissBottomSheetStateTransformer)
                        onNextClick(activeStake)
                    },
                    onDismiss = { stateController.update(DismissBottomSheetStateTransformer) },
                ),
            )
        }
    }

    override fun onActiveStakeAnalytic() {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.ButtonValidator(
                source = StakeScreenSource.Info,
            ),
        )
    }

    override fun showApprovalBottomSheet() {
        stateController.update(
            ShowApprovalBottomSheetTransformer(
                appCurrencyProvider = Provider { appCurrency },
                cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            ) {
                stateController.update(DismissBottomSheetStateTransformer)
            },
        )
    }

    override fun onApproveTypeChange(approveType: ApproveType) {
        stateController.update(SetApprovalBottomSheetTypeChangeTransformer(approveType))
    }

    @Suppress("LongMethod")
    override fun onApprovalClick() {
        modelScope.launch {
            stateController.update(
                SetApprovalBottomSheetInProgressTransformer {
                    stateController.update(DismissBottomSheetStateTransformer)
                },
            )

            val tokenCryptoCurrency =
                cryptoCurrencyStatus.currency as? CryptoCurrency.Token ?: error("No token currency")
            val amountValue = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value

            val confirmationState = value.confirmationState as? StakingStates.ConfirmationState.Data
                ?: error("No confirmation state")
            val fee = (confirmationState.feeState as? FeeState.Content)?.fee ?: error("No fee provided")
            val approval = stakingApproval as? StakingApproval.Needed ?: error("No staking approve spender address")

            val approvalBottomSheetConfig = value.bottomSheetConfig?.content as? GiveTxPermissionBottomSheetConfig
            val isLimitedApproval = approvalBottomSheetConfig?.data?.approveType == ApproveType.LIMITED

            val approvalTransaction = createApprovalTransactionUseCase(
                amount = amountValue.takeIf { isLimitedApproval },
                contractAddress = tokenCryptoCurrency.contractAddress,
                spenderAddress = approval.spenderAddress,
                fee = fee,
                cryptoCurrency = tokenCryptoCurrency,
                userWalletId = userWalletId,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error.toString())
                    analyticsEventHandler.send(StakingAnalyticsEvent.TransactionError)
                    stateController.update(
                        SetConfirmationStateAssentApprovalTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = TransactionFee.Single(fee),
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                        ),
                    )
                    stakingEventFactory.createGenericErrorAlert(error.message ?: error.toString())
                    stateController.update(
                        SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus = cryptoCurrencyStatus),
                    )
                    return@launch
                },
                ifRight = { it },
            )

            sendTransactionUseCase(
                txData = approvalTransaction,
                userWallet = userWallet,
                network = tokenCryptoCurrency.network,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error.toString())
                    analyticsEventHandler.send(StakingAnalyticsEvent.TransactionError)
                    stateController.update(
                        SetConfirmationStateAssentApprovalTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = TransactionFee.Single(fee),
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                        ),
                    )
                    stakingEventFactory.createSendTransactionErrorAlert(error)
                    stateController.update(
                        SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus = cryptoCurrencyStatus),
                    )
                },
                ifRight = {
                    stakingAnalyticSender.sendTransactionApprovalAnalytics(tokenCryptoCurrency)
                    stateController.update(SetApprovalInProgressTransformer)
                    stateController.update(DismissBottomSheetStateTransformer)
                    awaitForAllowance()
                },
            )
        }.saveIn(approvalJobHolder)
    }

    private fun updateNotifications(feeError: GetFeeError? = null, stakingError: StakingError? = null) {
        modelScope.launch {
            val confirmationState = value.confirmationState as? StakingStates.ConfirmationState.Data
            val feeState = confirmationState?.feeState as? FeeState.Content
            val amountState = value.amountState as? AmountState.Data

            val amount = amountState?.amountTextField?.cryptoAmount?.value
            val fee = feeState?.fee?.amount?.value
            val currencyWarning = if (feeCryptoCurrencyStatus != null && fee != null) {
                getBalanceNotEnoughForFeeWarningUseCase(
                    fee = fee,
                    userWalletId = userWalletId,
                    tokenStatus = cryptoCurrencyStatus,
                    coinStatus = feeCryptoCurrencyStatus ?: cryptoCurrencyStatus,
                ).getOrNull()
            } else {
                null
            }

            val balanceAfterTransaction = calculateBalanceAfterTransaction(
                amount = amount.orZero(),
                fee = fee.orZero(),
                reduceAmountBy = confirmationState?.reduceAmountBy.orZero(),
                actionType = value.actionType,
            )
            val currencyStatus = getCurrencyCheckUseCase(
                userWalletId = userWalletId,
                currencyStatus = cryptoCurrencyStatus,
                amount = amount,
                fee = fee,
                feeCurrencyBalanceAfterTransaction = balanceAfterTransaction,
            )
            stateController.update(
                AddStakingNotificationsTransformer(
                    cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                    appCurrencyProvider = Provider { appCurrency },
                    feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                    currencyWarning = currencyWarning,
                    currencyCheck = currencyStatus,
                    isSubtractAvailable = isAmountSubtractAvailable,
                    feeError = feeError,
                    stakingError = stakingError,
                    yield = yield,
                ),
            )
        }
    }

    override fun onAmountReduceByClick(
        reduceAmountBy: BigDecimal,
        reduceAmountByDiff: BigDecimal,
        notification: Class<out NotificationUM>,
    ) {
        AmountReduceByStateTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            minimumTransactionAmount = minimumTransactionAmount,
            value = AmountReduceByTransformer.ReduceByData(
                reduceAmountBy = reduceAmountBy,
                reduceAmountByDiff = reduceAmountByDiff,
            ),
        )
        onNotificationCancel(notification)
    }

    override fun onAmountReduceToClick(reduceAmountTo: BigDecimal, notification: Class<out NotificationUM>) {
        stateController.update(
            AmountReduceToStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                minimumTransactionAmount = minimumTransactionAmount,
                value = reduceAmountTo,
            ),
        )
        onNotificationCancel(notification)
    }

    override fun onNotificationCancel(notification: Class<out NotificationUM>) {
        stateController.update(DismissStakingNotificationsStateTransformer(notification))
    }

    private fun calculateBalanceAfterTransaction(
        amount: BigDecimal,
        fee: BigDecimal,
        reduceAmountBy: BigDecimal,
        actionType: StakingActionCommonType,
    ): BigDecimal? {
        // TODO split for different networks
        val subtractedBalanceAmount = when (actionType) {
            is StakingActionCommonType.Enter -> checkAndCalculateSubtractedAmount(
                isAmountSubtractAvailable = isAmountSubtractAvailable,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                amountValue = amount.orZero(),
                feeValue = fee.orZero(),
                reduceAmountBy = reduceAmountBy,
            )
            is StakingActionCommonType.Exit,
            is StakingActionCommonType.Pending,
            -> BigDecimal.ZERO
        }
        val statusValue = cryptoCurrencyStatus.value as? CryptoCurrencyStatus.Loaded
        return statusValue?.let { it.amount - subtractedBalanceAmount - fee.orZero() }
    }

    private fun awaitForAllowance() {
        val approval = stakingApproval as? StakingApproval.Needed ?: return
        allowanceTaskScheduler.scheduleTask(
            scope = modelScope,
            task = PeriodicTask(
                delay = ALLOWANCE_UPDATE_DELAY,
                task = {
                    runCatching {
                        getAllowanceUseCase(
                            userWalletId = userWalletId,
                            cryptoCurrency = cryptoCurrencyStatus.currency,
                            spenderAddress = approval.spenderAddress,
                        ).getOrElse { BigDecimal.ZERO }
                    }
                },
                onSuccess = { allowance ->
                    stakingAllowance = allowance
                    val amount = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                        ?: error("No amount provided")
                    if (allowance >= amount) {
                        stateController.update(SetUpdatedAllowanceTransformer(allowance))
                        getFee()
                        allowanceTaskScheduler.cancelTask()
                    }
                },
                onError = { /* no-op */ },
            ),
        )
    }

    override fun onExploreClick() {
        analyticsEventHandler.send(StakingAnalyticsEvent.ButtonExplore)
        val confirmationDataState = uiState.value.confirmationState as? StakingStates.ConfirmationState.Data
        val transactionDoneState = confirmationDataState?.transactionDoneState as? TransactionDoneState.Content
        val txUrl = transactionDoneState?.txUrl
        if (txUrl != null) {
            innerRouter.openUrl(txUrl)
        }
    }

    override fun onShareClick() {
        val confirmationDataState = uiState.value.confirmationState as? StakingStates.ConfirmationState.Data
        val transactionDoneState = confirmationDataState?.transactionDoneState as? TransactionDoneState.Content
        val txUrl = transactionDoneState?.txUrl

        analyticsEventHandler.send(StakingAnalyticsEvent.ButtonShare)
        if (txUrl != null) {
            vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
            shareManager.shareText(txUrl)
        }
    }

    override fun onFailedTxEmailClick(errorMessage: String) {
        modelScope.launch {
            val network = cryptoCurrencyStatus.currency.network

            val cardInfo = getCardInfoUseCase(userWallet.scanResponse).getOrElse { error("CardInfo must be not null") }
            val amountState = uiState.value.amountState as? AmountState.Data
            val confirmationState = uiState.value.confirmationState as? StakingStates.ConfirmationState.Data
            val validatorState = uiState.value.validatorState as? StakingStates.ValidatorState.Data
            val feeState = confirmationState?.feeState as? FeeState.Content

            val validator = validatorState?.chosenValidator
            val feeAmount = feeState?.fee?.amount
            val amount = amountState?.amountTextField?.cryptoAmount
            saveBlockchainErrorUseCase(
                error = BlockchainErrorInfo(
                    errorMessage = errorMessage,
                    blockchainId = network.id.value,
                    derivationPath = network.derivationPath.value,
                    destinationAddress = validator?.address.orEmpty(),
                    tokenSymbol = (cryptoCurrencyStatus.currency as? CryptoCurrency.Token)?.symbol,
                    amount = amount?.run { value?.toPlainString() + currencySymbol }.orEmpty(),
                    fee = feeAmount?.run { value?.toPlainString() + currencySymbol }.orEmpty(),
                ),
            )

            val email = FeedbackEmailType.StakingProblem(
                cardInfo = cardInfo,
                validatorName = validator?.name,
                transactionTypes = transactionsInProgress.map { it.type.name },
                unsignedTransactions = transactionsInProgress.map { it.unsignedTransaction },
            )

            sendFeedbackEmailUseCase(email)
        }
    }

    override fun openTokenDetails(cryptoCurrency: CryptoCurrency) {
        innerRouter.openTokenDetails(userWalletId, cryptoCurrency)
    }

    override fun showPrimaryClickAlert() {
        stateController.updateEvent(
            StakingEvent.ShowAlert(
                StakingAlertUM.StakeMoreClickUnavailable(cryptoCurrencyStatus.currency),
            ),
        )
    }

    private suspend fun setupApprovalNeeded() {
        stakingApproval = isApproveNeededUseCase(cryptoCurrencyStatus.currency).fold(
            ifRight = { approval ->
                if (approval is StakingApproval.Needed) {
                    stakingAllowance = getAllowanceUseCase(
                        userWalletId = userWalletId,
                        cryptoCurrency = cryptoCurrencyStatus.currency,
                        spenderAddress = approval.spenderAddress,
                    ).getOrElse { BigDecimal.ZERO }
                }
                approval
            },
            ifLeft = {
                StakingApproval.Empty
            },
        )
    }

    private suspend fun setupIsAnyTokenStaked() {
        isAnyTokenStaked = isAnyTokenStakedUseCase(userWalletId).getOrNull() == true
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        getUserWalletUseCase(userWalletId).fold(
            ifRight = { wallet ->
                userWallet = wallet
            },
            ifLeft = {
                stakingEventFactory.createGenericErrorAlert(it.toString())
                stateController.update(
                    SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus = cryptoCurrencyStatus),
                )
            },
        )
        getCurrencyStatusUpdatesUseCase(
            userWalletId = userWalletId,
            currencyId = cryptoCurrencyId,
            isSingleWalletWithTokens = false,
        )
            .conflate()
            .distinctUntilChangedBy { it.getOrNull()?.value?.yieldBalance }
            .filter { value.currentStep == StakingStep.InitialInfo }
            .onEach { maybeStatus ->
                maybeStatus.fold(
                    ifRight = { status ->
                        if (!isInitialInfoAnalyticSent) {
                            isInitialInfoAnalyticSent = true
                            val balances = status.value.yieldBalance as? YieldBalance.Data
                            paramsInterceptorHolder.addParamsInterceptor(
                                interceptor = StakingParamsInterceptor(status.currency.symbol),
                            )
                            analyticsEventHandler.send(
                                StakingAnalyticsEvent.StakingInfoScreenOpened(
                                    validatorsCount = balances?.getValidatorsCount() ?: 0,
                                ),
                            )
                        }

                        feeCryptoCurrencyStatus =
                            getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId, status).getOrNull()
                        minimumTransactionAmount =
                            getMinimumTransactionAmountSyncUseCase(userWalletId, status).getOrNull()?.let {
                                EnterAmountBoundary(
                                    amount = it,
                                    fiatRate = status.value.fiatRate.orZero(),
                                )
                            }
                        cryptoCurrencyStatus = status

                        setupApprovalNeeded()
                        setupIsAnyTokenStaked()
                        checkIfSubtractAvailable()
                        subscribeOnActionsUpdates(status)
                        subscribeOnStepChanges(status)
                    },
                    ifLeft = { error ->
                        stakingEventFactory.createGenericErrorAlert(error.toString())
                        stateController.update(
                            SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus = cryptoCurrencyStatus),
                        )
                    },
                )
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun subscribeOnBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                stateController.update(transformer = HideBalanceStateTransformer(it.isBalanceHidden))
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun subscribeOnSelectedAppCurrency() {
        getSelectedAppCurrencyUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach { maybeAppCurrency ->
                appCurrency = maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun subscribeOnStepChanges(status: CryptoCurrencyStatus) {
        uiState
            .distinctUntilChangedBy { it.currentStep }
            .onEach {
                when {
                    isInitState() -> {
                        updateInitialData(status)
                        balanceUpdater.partialUpdate()
                    }
                    isAssentState() -> {
                        getFee()
                        val amountState = value.amountState as? AmountState.Data
                        if (amountState?.amountTextField?.isWarning == true) {
                            stateController.update(
                                AmountRoundToIntegerTransformer(
                                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                                ),
                            )
                        }
                    }
                }
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(stepChangesJobHolder)
    }

    private fun subscribeOnActionsUpdates(status: CryptoCurrencyStatus) {
        getActionsUseCase(userWalletId = userWalletId, cryptoCurrencyId = cryptoCurrencyId)
            .conflate()
            .distinctUntilChanged()
            .onEach { result ->
                result.getOrNull()?.let { actions ->
                    processingActions = actions
                    if (isInitState()) {
                        updateInitialData(status)
                    }
                }
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(actionsJobHolder)
    }

    private fun updateInitialData(status: CryptoCurrencyStatus) {
        stateController.updateAll(
            SetInitialDataStateTransformer(
                clickIntents = this@StakingModel,
                yield = yield,
                isAnyTokenStaked = isAnyTokenStaked,
                cryptoCurrencyStatus = status,
                userWalletProvider = Provider { userWallet },
                appCurrencyProvider = Provider { appCurrency },
                balancesToShowProvider = Provider { balancesToShow },
            ),
            SetConfirmationStateEmptyTransformer,
        )
    }

    private fun prepareForConfirmation(
        balanceType: BalanceType,
        balanceState: BalanceState,
        pendingActions: ImmutableList<PendingAction> = persistentListOf(),
        pendingAction: PendingAction? = pendingActions.firstOrNull(),
        validator: Yield.Validator?,
        amountValue: String,
    ) {
        stateController.updateAll(
            SetConfirmationStateInitTransformer(
                isEnter = false,
                isExplicitExit = isExplicitExit(balanceType, pendingAction),
                balanceState = balanceState,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                stakingApproval = stakingApproval,
                pendingActions = pendingActions,
                pendingAction = pendingAction,
                stakingAllowance = stakingAllowance,
                yieldArgs = yield.args,
            ),
            ValidatorSelectChangeTransformer(
                selectedValidator = validator,
                yield = yield,
            ),
            SetAmountDataTransformer(
                clickIntents = this,
                cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                userWalletProvider = Provider { userWallet },
                appCurrencyProvider = Provider { appCurrency },
            ),
            AmountChangeStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                value = amountValue,
                minimumTransactionAmount = minimumTransactionAmount,
                yield = yield,
            ),
        )
    }

    private fun isExplicitExit(balanceType: BalanceType, pendingAction: PendingAction?): Boolean {
        return balanceType == BalanceType.STAKED && pendingAction?.type?.isRestake == false
    }

    private fun isAssentState(): Boolean {
        return value.currentStep == StakingStep.Confirmation &&
            (value.confirmationState as? StakingStates.ConfirmationState.Data)?.innerState ==
            InnerConfirmationStakingState.ASSENT
    }

    private fun isInitState(): Boolean {
        return value.currentStep == StakingStep.InitialInfo
    }

    private suspend fun checkIfSubtractAvailable() {
        isAmountSubtractAvailable = isAmountSubtractAvailableUseCase(userWalletId, cryptoCurrencyStatus.currency)
            .getOrElse { false }
    }

    private companion object {
        const val WHAT_IS_STAKING_ARTICLE_URL = "https://tangem.com/en/blog/post/how-to-stake-cryptocurrency/"
        const val ALLOWANCE_UPDATE_DELAY = 10_000L
    }
}