package com.tangem.features.staking.impl.presentation.viewmodel

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.FeedbackManager
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.staking.InvalidatePendingTransactionsUseCase
import com.tangem.domain.staking.IsAnyTokenStakedUseCase
import com.tangem.domain.staking.IsApproveNeededUseCase
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetAllowanceUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.ValidateTransactionUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.staking.impl.analytics.StakeScreenSource
import com.tangem.features.staking.impl.analytics.StakingAnalyticsEvents
import com.tangem.features.staking.impl.analytics.utils.StakingAnalyticSender
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.events.StakingEventFactory
import com.tangem.features.staking.impl.presentation.state.helpers.StakingBalanceUpdater
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeTransactionLoader
import com.tangem.features.staking.impl.presentation.state.helpers.StakingTransactionSender
import com.tangem.features.staking.impl.presentation.state.transformers.*
import com.tangem.features.staking.impl.presentation.state.transformers.amount.*
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalBottomSheetInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetConfirmationStateAssentApprovalTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.ShowApprovalBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.features.staking.impl.presentation.state.utils.isSolanaWithdraw
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import com.tangem.utils.extensions.isSingleItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@HiltViewModel
internal class StakingViewModel @Inject constructor(
    private val stateController: StakingStateController,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getAllowanceUseCase: GetAllowanceUseCase,
    private val isApproveNeededUseCase: IsApproveNeededUseCase,
    private val vibratorHapticManager: VibratorHapticManager,
    private val feedbackManager: FeedbackManager,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val isAnyTokenStakedUseCase: IsAnyTokenStakedUseCase,
    private val invalidatePendingTransactionsUseCase: InvalidatePendingTransactionsUseCase,
    private val stakingTransactionLoader: StakingTransactionSender.Factory,
    private val stakingFeeTransactionLoader: StakingFeeTransactionLoader.Factory,
    private val stakingBalanceUpdater: StakingBalanceUpdater.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
    @DelayedWork private val coroutineScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, StakingClickIntents {

    val uiState: StateFlow<StakingUiState> = stateController.uiState
    val value: StakingUiState get() = uiState.value

    private var stakingStateRouter: StakingStateRouter by Delegates.notNull()

    private val cryptoCurrencyId: CryptoCurrency.ID =
        savedStateHandle.get<Bundle>(AppRoute.Staking.CRYPTO_CURRENCY_ID_KEY)
            ?.unbundle(CryptoCurrency.ID.serializer())
            ?: error("This screen can't be opened without `CryptoCurrency.ID`")

    private val userWalletId: UserWalletId = savedStateHandle.get<Bundle>(AppRoute.Staking.USER_WALLET_ID_KEY)
        ?.unbundle(UserWalletId.serializer())
        ?: error("This screen can't be opened without `UserWalletId`")

    private val yield: Yield = savedStateHandle.get<Bundle>(AppRoute.Staking.YIELD_KEY)
        ?.unbundle(Yield.serializer())
        ?: error("This screen can't be opened without `Yield`")

    private var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    private var feeCryptoCurrencyStatus: CryptoCurrencyStatus? = null

    private var innerRouter: InnerStakingRouter by Delegates.notNull()
    private var userWallet: UserWallet by Delegates.notNull()
    private var appCurrency: AppCurrency by Delegates.notNull()

    private val balancesToShow: List<BalanceItem>
        get() {
            val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data
            return invalidatePendingTransactionsUseCase(
                userWalletId = userWallet.walletId,
                balanceItems = yieldBalance?.balance?.items ?: emptyList(),
                balancesId = yieldBalance?.getBalancesUniqueId() ?: 0,
            ).getOrElse { emptyList() }
        }

    private var isInitialInfoAnalyticSent: Boolean = false

    private val balanceUpdater by lazy(LazyThreadSafetyMode.NONE) {
        stakingBalanceUpdater.create(
            cryptoCurrencyStatus,
            userWallet,
        )
    }

    private val feeLoader by lazy(LazyThreadSafetyMode.NONE) {
        stakingFeeTransactionLoader.create(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWallet = userWallet,
            yield = yield,
            stakingApproval = stakingApproval,
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
    private var isAmountSubtractAvailable: Boolean = false
    private val allowanceTaskScheduler = SingleTaskScheduler<BigDecimal>()

    private val transactionsInProgress: CopyOnWriteArrayList<StakingTransaction> = CopyOnWriteArrayList()

    private var approvalJobHolder: JobHolder = JobHolder()
    private var feeJobHolder: JobHolder = JobHolder()
    private var sendTransactionJobHolder = JobHolder()

    init {
        subscribeOnSelectedAppCurrency()
        subscribeOnBalanceHiding()
        subscribeOnCurrencyStatusUpdates()
    }

    override fun onCleared() {
        super.onCleared()
        approvalJobHolder.cancel()
        feeJobHolder.cancel()
        sendTransactionJobHolder.cancel()
    }

    override fun onBackClick() {
        stakingStateRouter.onBackClick()
    }

    override fun onNextClick(
        actionTypeToOverwrite: StakingActionCommonType?,
        pendingAction: PendingAction?,
        pendingActions: ImmutableList<PendingAction>?,
        balanceState: BalanceState?,
    ) {
        if (actionTypeToOverwrite != null) {
            stateController.update(SetActionToExecuteTransformer(actionTypeToOverwrite, pendingAction, pendingActions))
        }
        stakingStateRouter.onNextClick()
        when {
            isInitState() -> {
                stateController.update(SetConfirmationStateResetAssentTransformer)
                stateController.update(SetConfirmationStateLoadingTransformer(yield, appCurrency))
                if (balanceState != null) {
                    stateController.update(SetPossiblePendingTransactionTransformer(balanceState, cryptoCurrencyStatus))
                }

                stateController.update(
                    transformer = SetInitialDataStateTransformer(
                        clickIntents = this@StakingViewModel,
                        yield = yield,
                        isAnyTokenStaked = true,
                        isApprovalNeeded = stakingApproval is StakingApproval.Needed,
                        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                        userWalletProvider = Provider { userWallet },
                        appCurrencyProvider = Provider { appCurrency },
                        balancesToShowProvider = Provider { balancesToShow },
                    ),
                )
                onRefreshSwipe(isRefreshing = false)
            }
            isAssentState() -> {
                getFee(pendingAction, pendingActions)
                if (balanceState != null) {
                    stateController.update(SetPossiblePendingTransactionTransformer(balanceState, cryptoCurrencyStatus))
                }
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

    override fun onActionClick() {
        handleOnNextConfirmationClick()
    }

    override fun getFee(pendingAction: PendingAction?, pendingActions: ImmutableList<PendingAction>?) {
        stateController.update(SetConfirmationStateLoadingTransformer(yield, appCurrency))
        viewModelScope.launch {
            feeLoader.getFee(
                pendingAction = pendingAction,
                pendingActions = pendingActions,
                onStakingFee = { gasEstimate ->
                    stateController.update(
                        SetConfirmationStateAssentTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = gasEstimate,
                            action = pendingAction,
                            actions = pendingActions,
                        ),
                    )
                    updateNotifications()
                },
                onStakingFeeError = { error ->
                    analyticsEventHandler.send(
                        StakingAnalyticsEvents.StakingError(
                            value.cryptoCurrencySymbol,
                            error.javaClass.simpleName,
                        ),
                    )
                    stateController.update(AddStakingErrorTransformer())
                    updateNotifications(GetFeeError.UnknownError)
                },
                onFeeError = { error ->
                    analyticsEventHandler.send(StakingAnalyticsEvents.TransactionError(value.cryptoCurrencySymbol))
                    stateController.update(AddStakingErrorTransformer())
                    updateNotifications(error)
                },
                onApprovalFee = { fee ->
                    stateController.update(
                        SetConfirmationStateAssentApprovalTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = fee,
                        ),
                    )
                    updateNotifications()
                },
            )
        }.saveIn(feeJobHolder)
    }

    private fun handleOnNextConfirmationClick() {
        if (isAssentState()) {
            viewModelScope.launch {
                stakingAnalyticSender.sendTransactionStakingClickedAnalytics(value)
                stateController.update(SetConfirmationStateInProgressTransformer())
                transactionSender.constructAndSendTransactions(
                    onConstructSuccess = { constructedTransactions ->
                        transactionsInProgress.addAll(constructedTransactions)
                    },
                    onConstructError = { error ->
                        Timber.e(error.toString())
                        analyticsEventHandler.send(
                            StakingAnalyticsEvents.StakingError(
                                token = value.cryptoCurrencySymbol,
                                errorType = error.javaClass.simpleName,
                            ),
                        )
                        stakingEventFactory.createStakingErrorAlert(error)
                        stateController.update(SetConfirmationStateResetAssentTransformer)
                    },
                    onSendSuccess = { txUrl ->
                        stakingAnalyticSender.sendTransactionStakingAnalytics(stateController.value)
                        transactionsInProgress.clear()
                        stateController.update(SetConfirmationStateCompletedTransformer(txUrl))
                    },
                    onSendError = { error ->
                        Timber.e(error.toString())
                        analyticsEventHandler.send(StakingAnalyticsEvents.TransactionError(value.cryptoCurrencySymbol))
                        stakingEventFactory.createSendTransactionErrorAlert(error)
                        stateController.update(SetConfirmationStateResetAssentTransformer)
                    },
                )
            }.saveIn(sendTransactionJobHolder)
        }
    }

    override fun onPrevClick() {
        if (value.currentStep == StakingStep.Confirmation) {
            when ((value.confirmationState as? StakingStates.ConfirmationState.Data)?.innerState) {
                InnerConfirmationStakingState.ASSENT -> {
                    stateController.update(SetConfirmationStateResetAssentTransformer)
                    stakingStateRouter.onPrevClick()
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
            balanceUpdater.instantUpdate()
        }.invokeOnCompletion {
            stateController.update(SetInitialLoadingStateTransformer(false))
        }
    }

    override fun onInitialInfoBannerClick() {
        analyticsEventHandler.send(StakingAnalyticsEvents.WhatIsStaking(value.cryptoCurrencySymbol))
        innerRouter.openUrl(WHAT_IS_STAKING_ARTICLE_URL)
    }

    override fun onInfoClick(infoType: InfoType) {
        stateController.update(
            ShowInfoBottomSheetStateTransformer(infoType) {
                stateController.update(DismissBottomSheetStateTransformer)
            },
        )
    }

    override fun onAmountValueChange(value: String) {
        stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, value, yield))
    }

    override fun onAmountPasteTriggerDismiss() {
        stateController.update(AmountPasteDismissStateTransformer())
    }

    override fun onMaxValueClick() {
        analyticsEventHandler.send(StakingAnalyticsEvents.ButtonMax(value.cryptoCurrencySymbol))
        stateController.update(AmountMaxValueStateTransformer(cryptoCurrencyStatus, yield))
    }

    override fun onCurrencyChangeClick(isFiat: Boolean) {
        analyticsEventHandler.send(
            StakingAnalyticsEvents.AmountSelectCurrency(value.cryptoCurrencySymbol, isFiat),
        )
        stateController.update(AmountCurrencyChangeStateTransformer(cryptoCurrencyStatus, isFiat))
    }

    override fun openValidators() {
        analyticsEventHandler.send(
            StakingAnalyticsEvents.ButtonValidator(
                source = StakeScreenSource.Confirmation,
                token = value.cryptoCurrencySymbol,
            ),
        )
        stakingStateRouter.showValidators()
    }

    override fun onValidatorSelect(validator: Yield.Validator) {
        analyticsEventHandler.send(
            StakingAnalyticsEvents.ValidatorChosen(
                value.cryptoCurrencySymbol,
                validator.name,
            ),
        )
        stateController.update(ValidatorSelectChangeTransformer(validator))
    }

    override fun openRewardsValidators() {
        analyticsEventHandler.send(
            StakingAnalyticsEvents.ButtonRewards(value.cryptoCurrencySymbol),
        )
        val rewardsValidators =
            stateController.value.rewardsValidatorsState as? StakingStates.RewardsValidatorsState.Data
        val rewards = rewardsValidators?.rewards
        if (rewards != null && rewards.isSingleItem()) {
            onActiveStake(rewards.first())
        } else {
            analyticsEventHandler.send(
                StakingAnalyticsEvents.ButtonValidator(
                    source = StakeScreenSource.Info,
                    token = value.cryptoCurrencySymbol,
                ),
            )
            onNextClick(actionTypeToOverwrite = StakingActionCommonType.PENDING_REWARDS)
        }
    }

    override fun onActiveStake(activeStake: BalanceState) {
        val isAllWithdrawActions = isSolanaWithdraw(
            cryptoCurrencyStatus.currency.network.id.value,
            activeStake.pendingActions,
        )
        val isMultiplePendingActions = activeStake.pendingActions.size > 1
        if (isMultiplePendingActions && !isAllWithdrawActions) {
            stateController.update(
                ShowActionSelectorBottomSheetTransformer(
                    pendingActions = activeStake.pendingActions,
                    onActionSelect = { action ->
                        stateController.update(ValidatorSelectChangeTransformer(activeStake.validator))
                        stateController.update(
                            AmountChangeStateTransformer(
                                cryptoCurrencyStatus,
                                activeStake.cryptoValue,
                                yield,
                            ),
                        )
                        onNextClick(
                            actionTypeToOverwrite = StakingActionCommonType.PENDING_OTHER,
                            pendingAction = action,
                            balanceState = activeStake,
                        )
                        stateController.update(DismissBottomSheetStateTransformer)
                    },
                    onDismiss = { stateController.update(DismissBottomSheetStateTransformer) },
                ),
            )
        } else {
            stateController.update(ActionTypeActiveStakeTransformer(cryptoCurrencyStatus, activeStake))
            stateController.update(ValidatorSelectChangeTransformer(activeStake.validator))
            stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, activeStake.cryptoValue, yield))

            onNextClick(
                actionTypeToOverwrite = null,
                pendingAction = activeStake.pendingActions.firstOrNull(),
                pendingActions = activeStake.pendingActions.takeIf { isAllWithdrawActions },
                balanceState = activeStake,
            )
        }
    }

    override fun onActiveStakeAnalytic() {
        analyticsEventHandler.send(
            StakingAnalyticsEvents.ButtonValidator(
                source = StakeScreenSource.Info,
                token = value.cryptoCurrencySymbol,
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

    override fun onApprovalClick() {
        viewModelScope.launch {
            stateController.update(
                SetApprovalBottomSheetInProgressTransformer {
                    stateController.update(DismissBottomSheetStateTransformer)
                },
            )

            val tokenCryptoCurrency =
                cryptoCurrencyStatus.currency as? CryptoCurrency.Token ?: error("No token currency")
            val amountValue = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                ?: error("No amount provided")

            val confirmationState = value.confirmationState as? StakingStates.ConfirmationState.Data
                ?: error("No confirmation state")
            val fee = (confirmationState.feeState as? FeeState.Content)?.fee ?: error("No fee provided")
            val approval = stakingApproval as? StakingApproval.Needed ?: error("No staking approve spender address")

            val approvalTransaction = createApprovalTransactionUseCase(
                amount = amountValue,
                contractAddress = tokenCryptoCurrency.contractAddress,
                spenderAddress = approval.spenderAddress,
                fee = fee,
                cryptoCurrency = tokenCryptoCurrency,
                userWalletId = userWalletId,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error.toString())
                    analyticsEventHandler.send(StakingAnalyticsEvents.TransactionError(value.cryptoCurrencySymbol))
                    stateController.update(
                        SetConfirmationStateAssentApprovalTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = TransactionFee.Single(fee),
                        ),
                    )
                    stakingEventFactory.createGenericErrorAlert(error.message ?: error.toString())
                    stateController.update(SetConfirmationStateResetAssentTransformer)
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
                    analyticsEventHandler.send(StakingAnalyticsEvents.TransactionError(value.cryptoCurrencySymbol))
                    stateController.update(
                        SetConfirmationStateAssentApprovalTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = TransactionFee.Single(fee),
                        ),
                    )
                    stakingEventFactory.createSendTransactionErrorAlert(error)
                    stateController.update(SetConfirmationStateResetAssentTransformer)
                },
                ifRight = {
                    stakingAnalyticSender.sendTransactionApprovalAnalytics(tokenCryptoCurrency)
                    stateController.update(SetApprovalInProgressTransformer)
                    stateController.update(DismissBottomSheetStateTransformer)
                    awaitForAllowance(confirmationState.pendingAction)
                },
            )
        }.saveIn(approvalJobHolder)
    }

    private fun updateNotifications(feeError: GetFeeError? = null) {
        viewModelScope.launch {
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
            val validation = amount?.let {
                validateTransactionUseCase(
                    userWalletId = userWalletId,
                    amount = amount.convertToSdkAmount(cryptoCurrencyStatus.currency),
                    fee = feeState?.fee,
                    memo = null,
                    destination = "",
                    network = cryptoCurrencyStatus.currency.network,
                ).leftOrNull()
            }

            val currencyStatus = getCurrencyCheckUseCase(
                userWalletId = userWalletId,
                currencyStatus = cryptoCurrencyStatus,
                amount = amount,
                fee = fee,
            )
            stateController.update(
                AddStakingNotificationsTransformer(
                    cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                    appCurrencyProvider = Provider { appCurrency },
                    feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                    currencyWarning = currencyWarning,
                    validatorError = validation,
                    currencyCheck = currencyStatus,
                    isSubtractAvailable = isAmountSubtractAvailable,
                    feeError = feeError,
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
                value = reduceAmountTo,
            ),
        )
        onNotificationCancel(notification)
    }

    override fun onNotificationCancel(notification: Class<out NotificationUM>) {
        stateController.update(DismissStakingNotificationsStateTransformer(notification))
    }

    private fun awaitForAllowance(pendingAction: PendingAction?) {
        val approval = stakingApproval as? StakingApproval.Needed ?: return
        allowanceTaskScheduler.scheduleTask(
            scope = viewModelScope,
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
                    val amount = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                        ?: error("No amount provided")
                    if (allowance >= amount) {
                        getFee(pendingAction = pendingAction, pendingActions = null)
                        allowanceTaskScheduler.cancelTask()
                    }
                },
                onError = { /* no-op */ },
            ),
        )
    }

    override fun onExploreClick() {
        analyticsEventHandler.send(StakingAnalyticsEvents.ButtonExplore)
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

        analyticsEventHandler.send(StakingAnalyticsEvents.ButtonShare)
        if (txUrl != null) {
            vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
            stakingEventFactory.createShareDialog(txUrl = txUrl)
        }
    }

    override fun onFailedTxEmailClick(errorMessage: String) {
        viewModelScope.launch {
            val network = cryptoCurrencyStatus.currency.network

            val cardInfo = getCardInfoUseCase(userWallet.scanResponse).getOrElse { error("CardInfo must be not null") }
            val amountState = uiState.value.amountState as? AmountState.Data
            val confirmationState = uiState.value.confirmationState as? StakingStates.ConfirmationState.Data
            val validatorState = confirmationState?.validatorState as? ValidatorState.Content
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

            feedbackManager.sendEmail(email)
        }
    }

    override fun openTokenDetails(cryptoCurrency: CryptoCurrency) {
        innerRouter.openTokenDetails(userWalletId, cryptoCurrency)
    }

    fun setRouter(router: InnerStakingRouter, stateRouter: StakingStateRouter) {
        innerRouter = router
        this.stakingStateRouter = stateRouter
    }

    private fun setupApprovalNeeded() {
        stakingApproval = isApproveNeededUseCase(cryptoCurrencyStatus.currency).getOrElse { StakingApproval.Empty }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        getUserWalletUseCase(userWalletId).fold(
            ifRight = { wallet ->
                userWallet = wallet
            },
            ifLeft = {
                Timber.e(it.toString())
                stakingEventFactory.createGenericErrorAlert(it.toString())
                stateController.update(SetConfirmationStateResetAssentTransformer)
            },
        )
        getCurrencyStatusUpdatesUseCase(userWalletId, cryptoCurrencyId, false)
            .conflate()
            .distinctUntilChanged()
            .filter { value.currentStep == StakingStep.InitialInfo }
            .onEach { maybeStatus ->
                maybeStatus.fold(
                    ifRight = { status ->
                        if (!isInitialInfoAnalyticSent) {
                            isInitialInfoAnalyticSent = true
                            val balances = status.value.yieldBalance as? YieldBalance.Data
                            analyticsEventHandler.send(
                                StakingAnalyticsEvents.StakingInfoScreenOpened(
                                    validatorsCount = balances?.getValidatorsCount() ?: 0,
                                    token = status.currency.symbol,
                                ),
                            )
                        }

                        feeCryptoCurrencyStatus =
                            getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId, status).getOrNull()
                        cryptoCurrencyStatus = status
                        val isAnyTokenStaked = isAnyTokenStakedUseCase(userWalletId).getOrNull() ?: false

                        setupApprovalNeeded()
                        checkIfSubtractAvailable()

                        stateController.update(
                            transformer = SetInitialDataStateTransformer(
                                clickIntents = this@StakingViewModel,
                                yield = yield,
                                isAnyTokenStaked = isAnyTokenStaked,
                                isApprovalNeeded = stakingApproval is StakingApproval.Needed,
                                cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                                userWalletProvider = Provider { userWallet },
                                appCurrencyProvider = Provider { appCurrency },
                                balancesToShowProvider = Provider { balancesToShow },
                            ),
                        )
                    },
                    ifLeft = { error ->
                        Timber.e(error.toString())
                        stakingEventFactory.createGenericErrorAlert(error.toString())
                        stateController.update(SetConfirmationStateResetAssentTransformer)
                    },
                )
            }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
    }

    private fun subscribeOnBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                stateController.update(transformer = HideBalanceStateTransformer(it.isBalanceHidden))
            }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
    }

    private fun subscribeOnSelectedAppCurrency() {
        getSelectedAppCurrencyUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach { maybeAppCurrency ->
                appCurrency = maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
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
