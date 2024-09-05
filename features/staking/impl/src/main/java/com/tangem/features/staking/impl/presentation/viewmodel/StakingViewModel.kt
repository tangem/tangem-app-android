package com.tangem.features.staking.impl.presentation.viewmodel

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
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
import com.tangem.domain.staking.*
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.events.StakingEventFactory
import com.tangem.features.staking.impl.presentation.state.transformers.*
import com.tangem.features.staking.impl.presentation.state.transformers.amount.*
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalBottomSheetInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetConfirmationStateAssentApprovalTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.ShowApprovalBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.features.staking.impl.presentation.state.utils.checkAndCalculateSubtractedAmount
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import com.tangem.utils.extensions.isSingleItem
import com.tangem.utils.extensions.orZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private val getCryptoCurrencyStatusSyncUseCase: GetCryptoCurrencyStatusSyncUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getStakingTransactionUseCase: GetStakingTransactionUseCase,
    private val getConstructedStakingTransactionUseCase: GetConstructedStakingTransactionUseCase,
    private val estimateGasUseCase: EstimateGasUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val sendMultipleTransactionUseCase: SendMultipleTransactionUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val saveUnsubmittedHashUseCase: SaveUnsubmittedHashUseCase,
    private val submitHashUseCase: SubmitHashUseCase,
    private val stakingYieldBalanceUseCase: FetchStakingYieldBalanceUseCase,
    private val updateDelayedNetworkStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase,
    private val getTxHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val getTxHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getAllowanceUseCase: GetAllowanceUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val isApproveNeededUseCase: IsApproveNeededUseCase,
    private val vibratorHapticManager: VibratorHapticManager,
    private val feedbackManager: FeedbackManager,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
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

    private val stakingEventFactory: StakingEventFactory
        get() = StakingEventFactory(
            stateController = stateController,
            popBackStack = stakingStateRouter::onBackClick,
            onFailedTxEmailClick = ::onFailedTxEmailClick,
        )

    private var stakingApproval: StakingApproval = StakingApproval.Empty
    private var isAmountSubtractAvailable: Boolean = false
    private val allowanceTaskScheduler = SingleTaskScheduler<BigDecimal>()

    private val transactionsInProgress: CopyOnWriteArrayList<StakingTransaction> = CopyOnWriteArrayList()

    private var approvalJobHolder: JobHolder = JobHolder()

    init {
        subscribeOnSelectedAppCurrency()
        subscribeOnBalanceHiding()
        subscribeOnCurrencyStatusUpdates()
    }

    override fun onBackClick() {
        stakingStateRouter.onBackClick()
    }

    override fun onNextClick(actionTypeToOverwrite: StakingActionCommonType?, pendingAction: PendingAction?) {
        if (actionTypeToOverwrite != null) {
            stateController.update { it.copy(actionType = actionTypeToOverwrite) }
            stateController.update {
                val confirmationState = it.confirmationState as? StakingStates.ConfirmationState.Data
                it.copy(
                    confirmationState = confirmationState?.copy(pendingAction = pendingAction)
                        ?: StakingStates.ConfirmationState.Empty(),
                )
            }
        }
        stakingStateRouter.onNextClick()
        when {
            isInitState() -> stateController.update(SetConfirmationStateLoadingTransformer(yield, appCurrency))
            isAssentState() -> getFee(pendingAction)
        }
    }

    override fun onActionClick() {
        handleOnNextConfirmationClick()
    }

    override fun getFee(pendingAction: PendingAction?) {
        viewModelScope.launch {
            stateController.update(SetConfirmationStateLoadingTransformer(yield, appCurrency))
            val cryptoCurrencyValue = cryptoCurrencyStatus.value
            val confirmationState = value.confirmationState as? StakingStates.ConfirmationState.Data
                ?: error("No confirmation state")
            val validatorState = confirmationState.validatorState as? ValidatorState.Content
                ?: error("No validator provided")

            val amount = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                ?: error("No amount provided")
            val sourceAddress = cryptoCurrencyValue.networkAddress?.defaultAddress?.value
                ?: error("No available address")
            val validatorAddress = validatorState.chosenValidator.address

            val approval = stakingApproval as? StakingApproval.Needed
            if (approval != null) {
                val allowance = getAllowanceUseCase(
                    userWalletId = userWalletId,
                    cryptoCurrency = cryptoCurrencyStatus.currency,
                    spenderAddress = approval.spenderAddress,
                ).getOrElse { BigDecimal.ZERO }

                if (allowance < amount) {
                    getApproveFee(
                        amount = amount,
                        validatorAddress = validatorAddress,
                    )
                } else {
                    estimateGas(
                        pendingAction = pendingAction,
                        amount = amount,
                        sourceAddress = sourceAddress,
                        validatorAddress = validatorAddress,
                    )
                }
            } else {
                estimateGas(
                    pendingAction = pendingAction,
                    amount = amount,
                    sourceAddress = sourceAddress,
                    validatorAddress = validatorAddress,
                )
            }
        }
    }

    private fun handleOnNextConfirmationClick() {
        if (isAssentState()) {
            viewModelScope.launch {
                stateController.update(SetConfirmationStateInProgressTransformer())

                val confirmationState =
                    value.confirmationState as? StakingStates.ConfirmationState.Data ?: error("No confirmation state")
                val validatorState = confirmationState.validatorState as? ValidatorState.Content
                    ?: error("No validator provided")
                val amountState = value.amountState as? AmountState.Data ?: error("No amount provided")

                val fee = (confirmationState.feeState as? FeeState.Content)?.fee ?: error("No fee provided")
                val defaultAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
                    ?: error("No available address")
                val pendingAction = confirmationState.pendingAction

                val stakingTransactions = getStakingTransactionUseCase(
                    userWalletId = userWalletId,
                    network = cryptoCurrencyStatus.currency.network,
                    params = ActionParams(
                        actionCommonType = value.actionType,
                        integrationId = yield.id,
                        amount = getAmount(amountState, fee, confirmationState.reduceAmountBy),
                        address = defaultAddress,
                        validatorAddress = validatorState.chosenValidator.address,
                        token = yield.token,
                        passthrough = pendingAction?.passthrough,
                        type = pendingAction?.type,
                    ),
                ).getOrElse {
                    Timber.e(it.toString())
                    stakingEventFactory.createStakingErrorAlert(it)
                    stateController.update(SetConfirmationStateResetAssentTransformer)
                    return@launch
                }

                val fullTransactionsData = stakingTransactions
                    .filterNot { it.type == StakingTransactionType.APPROVAL }
                    .map { transaction ->
                        val (constructedTransaction, transactionData) = getConstructedStakingTransactionUseCase(
                            networkId = cryptoCurrencyStatus.currency.network.id.value,
                            fee = fee,
                            transactionId = transaction.id,
                        ).getOrElse {
                            Timber.e(it.toString())
                            stakingEventFactory.createStakingErrorAlert(it)
                            stateController.update(SetConfirmationStateResetAssentTransformer)
                            return@launch
                        }

                        FullTransactionData(
                            stakeKitTransaction = constructedTransaction,
                            tangemTransaction = transactionData,
                        )
                    }

                transactionsInProgress.addAll(fullTransactionsData.map { it.stakeKitTransaction })

                sendStakingTransaction(
                    fullTransactionsData = fullTransactionsData,
                    fee = fee,
                    pendingAction = confirmationState.pendingAction,
                )
            }
        }
    }

    private suspend fun estimateGas(
        pendingAction: PendingAction?,
        amount: BigDecimal,
        sourceAddress: String,
        validatorAddress: String,
    ) {
        val gasEstimate = estimateGasUseCase(
            userWalletId = userWalletId,
            network = cryptoCurrencyStatus.currency.network,
            params = ActionParams(
                actionCommonType = value.actionType,
                integrationId = yield.id,
                amount = amount,
                address = sourceAddress,
                validatorAddress = validatorAddress,
                token = yield.token,
                passthrough = pendingAction?.passthrough,
                type = pendingAction?.type,
            ),
        ).getOrElse {
            stateController.update(AddStakingErrorTransformer(it))
            return
        }
        stateController.update(
            SetConfirmationStateAssentTransformer(
                appCurrencyProvider = Provider { appCurrency },
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                fee = Fee.Common(
                    Amount(
                        currencySymbol = gasEstimate.token.symbol,
                        value = gasEstimate.amount,
                        decimals = gasEstimate.token.decimals,
                    ),
                ),
                action = pendingAction,
            ),
        )
        updateNotifications()
    }

    private suspend fun getApproveFee(amount: BigDecimal, validatorAddress: String) {
        getFeeUseCase(
            amount = amount,
            destination = validatorAddress,
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrencyStatus.currency,
        ).fold(
            ifRight = { fee ->
                stateController.update(
                    SetConfirmationStateAssentApprovalTransformer(
                        appCurrencyProvider = Provider { appCurrency },
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                        fee = fee,
                    ),
                )
                updateNotifications()
            },
            ifLeft = {
                stateController.update(AddStakingErrorTransformer())
                updateNotifications(it)
            },
        )
    }

    override fun onPrevClick() {
        stakingStateRouter.onPrevClick()
    }

    override fun onInitialInfoBannerClick() {
        innerRouter.openUrl(WHAT_IS_STAKING_ARTICLE_URL)
    }

    override fun onInfoClick(infoType: InfoType) {
        stateController.update(
            ShowInfoBottomSheetStateTransformer(infoType) {
                stateController.update(DismissBottomSheetStateTransformer())
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
        stateController.update(AmountMaxValueStateTransformer(cryptoCurrencyStatus, yield))
    }

    override fun onCurrencyChangeClick(isFiat: Boolean) {
        stateController.update(AmountCurrencyChangeStateTransformer(cryptoCurrencyStatus, isFiat))
    }

    override fun openValidators() = stakingStateRouter.showValidators()

    override fun onValidatorSelect(validator: Yield.Validator) {
        stateController.update(ValidatorSelectChangeTransformer(validator))
    }

    override fun openRewardsValidators() {
        val rewardsValidators =
            stateController.value.rewardsValidatorsState as? StakingStates.RewardsValidatorsState.Data
        val rewards = rewardsValidators?.rewards
        if (rewards != null && rewards.isSingleItem()) {
            onActiveStake(rewards.first())
        } else {
            onNextClick(actionTypeToOverwrite = StakingActionCommonType.PENDING_REWARDS)
        }
    }

    override fun onActiveStake(activeStake: BalanceState) {
        if (activeStake.pendingActions.size > 1) {
            stateController.update(
                ShowActionSelectorBottomSheetTransformer(
                    pendingActions = activeStake.pendingActions,
                    onActionSelect = {
                        stateController.update(ValidatorSelectChangeTransformer(activeStake.validator))
                        stateController.update(
                            AmountChangeStateTransformer(
                                cryptoCurrencyStatus,
                                activeStake.cryptoValue,
                                yield,
                            ),
                        )
                        onNextClick(actionTypeToOverwrite = StakingActionCommonType.PENDING_OTHER, pendingAction = it)
                        stateController.update(DismissBottomSheetStateTransformer())
                    },
                    onDismiss = {
                        stateController.update(DismissBottomSheetStateTransformer())
                    },
                ),
            )
        } else {
            stateController.update(ActionTypeActiveStakeTransformer(cryptoCurrencyStatus, activeStake))
            stateController.update(ValidatorSelectChangeTransformer(activeStake.validator))
            stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, activeStake.cryptoValue, yield))
            onNextClick(null, activeStake.pendingActions.firstOrNull())
        }
    }

    override fun showApprovalBottomSheet() {
        stateController.update(
            ShowApprovalBottomSheetTransformer(
                appCurrencyProvider = Provider { appCurrency },
                cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            ) {
                stateController.update(DismissBottomSheetStateTransformer())
            },
        )
    }

    override fun onApprovalClick() {
        viewModelScope.launch {
            stateController.update(
                SetApprovalBottomSheetInProgressTransformer {
                    stateController.update(DismissBottomSheetStateTransformer())
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
                    stateController.update(SetApprovalInProgressTransformer)
                    stateController.update(DismissBottomSheetStateTransformer())
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
                        getFee(pendingAction)
                        allowanceTaskScheduler.cancelTask()
                    }
                },
                onError = { /* no-op */ },
            ),
        )
    }

    override fun onExploreClick() {
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

        if (txUrl != null) {
            vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
            stakingEventFactory.createShareDialog(txUrl = txUrl)
        }

        // TODO staking [REDACTED_TASK_KEY]
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
        viewModelScope.launch {
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
            getCryptoCurrencyStatusSyncUseCase(userWalletId, cryptoCurrencyId).fold(
                ifRight = {
                    feeCryptoCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId, it).getOrNull()
                    cryptoCurrencyStatus = it

                    setupApprovalNeeded()
                    checkIfSubtractAvailable()

                    stateController.update(
                        transformer = SetInitialDataStateTransformer(
                            clickIntents = this@StakingViewModel,
                            yield = yield,
                            isApprovalNeeded = stakingApproval is StakingApproval.Needed,
                            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                            userWalletProvider = Provider { userWallet },
                            appCurrencyProvider = Provider { appCurrency },
                        ),
                    )
                },
                ifLeft = {
                    Timber.e(it.toString())
                    stakingEventFactory.createGenericErrorAlert(it.toString())
                    stateController.update(SetConfirmationStateResetAssentTransformer)
                },
            )
        }
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

    private suspend fun sendStakingTransaction(
        fullTransactionsData: List<FullTransactionData>,
        fee: Fee,
        pendingAction: PendingAction?,
    ) {
        sendMultipleTransactionUseCase(
            txsData = fullTransactionsData.map { it.tangemTransaction },
            userWallet = userWallet,
            network = cryptoCurrencyStatus.currency.network,
        ).fold(
            ifLeft = { error ->
                Timber.e(error.toString())
                stateController.update(
                    SetConfirmationStateAssentTransformer(
                        appCurrencyProvider = Provider { appCurrency },
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                        fee = fee,
                        action = pendingAction,
                    ),
                )
                stakingEventFactory.createSendTransactionErrorAlert(error)
                stateController.update(SetConfirmationStateResetAssentTransformer)
            },
            ifRight = { transactionHashes ->
                transactionsInProgress.clear()
                submitHash(
                    transactionIds = fullTransactionsData.map { it.stakeKitTransaction.id },
                    transactionHashes = transactionHashes,
                )
                scheduleUpdates()
                val txUrl = getExplorerTransactionUrlUseCase(
                    txHash = transactionHashes.last(),
                    networkId = cryptoCurrencyStatus.currency.network.id,
                ).getOrElse { "" }

                stateController.update(
                    SetConfirmationStateCompletedTransformer(
                        appCurrencyProvider = Provider { appCurrency },
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                        fee = fee,
                        txUrl = txUrl,
                    ),
                )
            },
        )
    }

    private suspend fun submitHash(transactionIds: List<String>, transactionHashes: List<String>) {
        transactionIds
            .zip(transactionHashes)
            .forEach { (transactionId, transactionHash) ->
                submitHashUseCase.submitHash(
                    transactionId = transactionId,
                    transactionHash = transactionHash,
                )
                    .onLeft {
                        saveUnsubmittedHashUseCase.invoke(
                            transactionId = transactionId,
                            transactionHash = transactionHash,
                        )
                    }.onRight {
                        Timber.d("Successful hash submission")
                    }
            }
    }

    private fun scheduleUpdates() {
        coroutineScope.launch {
            listOf(
                // we should update network to find pending tx after 1 sec
                async {
                    fetchPendingTransactionsUseCase(userWallet.walletId, setOf(cryptoCurrencyStatus.currency.network))
                },
                // we should update tx history and network for new balances
                async {
                    updateStakeBalance()
                },
                async {
                    updateTxHistory()
                },
                async {
                    updateNetworkStatuses()
                },
            ).awaitAll()
        }
    }

    private suspend fun updateNetworkStatuses() {
        updateDelayedNetworkStatusUseCase(
            userWalletId = userWalletId,
            network = cryptoCurrencyStatus.currency.network,
            delayMillis = BALANCE_UPDATE_DELAY,
            refresh = true,
        )
    }

    private suspend fun updateStakeBalance() {
        stakingYieldBalanceUseCase(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrencyStatus.currency,
            refresh = true,
        )
    }

    private suspend fun updateTxHistory() {
        delay(BALANCE_UPDATE_DELAY)
        val txHistoryItemsCountEither = getTxHistoryItemsCountUseCase(
            userWalletId = userWalletId,
            currency = cryptoCurrencyStatus.currency,
        )

        txHistoryItemsCountEither.onRight {
            getTxHistoryItemsUseCase(
                userWalletId = userWalletId,
                currency = cryptoCurrencyStatus.currency,
                refresh = true,
            )
        }
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

    private fun getAmount(amountState: AmountState.Data, fee: Fee, reduceAmountBy: BigDecimal?): BigDecimal {
        val amountValue = amountState.amountTextField.cryptoAmount.value ?: error("No amount value")
        val feeValue = fee.amount.value ?: error("No fee value")
        val isEnterAction = stateController.value.actionType == StakingActionCommonType.ENTER

        return checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable && isEnterAction,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy.orZero(),
        )
    }

    private data class FullTransactionData(
        val stakeKitTransaction: StakingTransaction,
        val tangemTransaction: TransactionData.Compiled,
    )

    private companion object {
        const val WHAT_IS_STAKING_ARTICLE_URL = "https://tangem.com/en/blog/post/how-to-stake-cryptocurrency/"
        const val ALLOWANCE_UPDATE_DELAY = 10_000L
        const val BALANCE_UPDATE_DELAY = 11_000L
    }
}