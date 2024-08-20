package com.tangem.features.staking.impl.presentation.viewmodel

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.staking.*
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetAllowanceUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.transformers.*
import com.tangem.features.staking.impl.presentation.state.transformers.amount.AmountChangeStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.amount.AmountCurrencyChangeStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.amount.AmountMaxValueStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.amount.AmountPasteDismissStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalBottomSheetInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetApprovalInProgressTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.SetConfirmationStateAssentApprovalTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.approval.ShowApprovalBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LargeClass", "LongParameterList")
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
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val saveUnsubmittedHashUseCase: SaveUnsubmittedHashUseCase,
    private val submitHashUseCase: SubmitHashUseCase,
    private val isStakeMoreAvailableUseCase: IsStakeMoreAvailableUseCase,
    private val stakingYieldBalanceUseCase: FetchStakingYieldBalanceUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getAllowanceUseCase: GetAllowanceUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val isApproveNeededUseCase: IsApproveNeededUseCase,
    private val clipboardManager: ClipboardManager,
    private val vibratorHapticManager: VibratorHapticManager,
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

    private var stakingApproval: StakingApproval = StakingApproval.Empty
    private val allowanceTaskScheduler = SingleTaskScheduler<BigDecimal>()

    private var approvalJobHolder: JobHolder = JobHolder()

    init {
        subscribeOnSelectedAppCurrency()
        subscribeOnBalanceHiding()
        subscribeOnCurrencyStatusUpdates()
    }

    override fun onBackClick() {
        stakingStateRouter.onBackClick()
    }

    override fun onNextClick(actionType: StakingActionCommonType?, pendingActions: ImmutableList<PendingAction>) {
        if (actionType != null) {
            stateController.update { it.copy(actionType = actionType) }
        }
        stakingStateRouter.onNextClick()
        if (isAssentState()) {
            getFee(pendingActions)
        }
    }

    override fun onActionClick(pendingAction: PendingAction?) {
        handleOnNextConfirmationClick(pendingAction)
        stakingStateRouter.onNextClick()
    }

    private fun handleOnNextConfirmationClick(pendingAction: PendingAction?) {
        if (isAssentState()) {
            viewModelScope.launch {
                stateController.update(SetConfirmationStateInProgressTransformer(pendingAction))

                val confirmationState =
                    value.confirmationState as? StakingStates.ConfirmationState.Data ?: error("No confirmation state")
                val validatorState = confirmationState.validatorState as? ValidatorState.Content
                    ?: error("No validator provided")
                val amountState = value.amountState as? AmountState.Data ?: error("No amount provided")
                val amountValue = amountState.amountTextField.cryptoAmount.value ?: error("No amount value")
                val fee = (confirmationState.feeState as? FeeState.Content)?.fee ?: error("No fee provided")

                val stakingTransaction = getStakingTransactionUseCase(
                    userWalletId = userWalletId,
                    network = cryptoCurrencyStatus.currency.network,
                    params = ActionParams(
                        actionCommonType = value.actionType,
                        integrationId = yield.id,
                        amount = amountValue,
                        address = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
                            ?: error("No available address"),
                        validatorAddress = validatorState.chosenValidator.address,
                        token = yield.token,
                        passthrough = pendingAction?.passthrough,
                        type = pendingAction?.type,
                    ),
                ).getOrElse {
                    error(it)
                }

                stakingTransaction
                    .filterNot { it.type == StakingTransactionType.APPROVAL }
                    .forEach { transaction ->
                        val (constructedTransaction, transactionData) = getConstructedStakingTransactionUseCase(
                            networkId = cryptoCurrencyStatus.currency.network.id.value,
                            fee = fee,
                            transactionId = transaction.id,
                        ).getOrNull() ?: error("No constructed transaction")

                        sendStakingTransaction(
                            transactionId = constructedTransaction.id,
                            gasEstimate = constructedTransaction.gasEstimate ?: error("No gas estimate available"),
                            txData = transactionData,
                            pendingActionList = confirmationState.pendingActions,
                        )
                    }
            }
        }
    }

    private fun getFee(pendingActions: ImmutableList<PendingAction>) {
        viewModelScope.launch {
            stateController.update(
                SetConfirmationStateLoadingTransformer(
                    yield = yield,
                ),
            )
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
                        pendingActions = pendingActions,
                        amount = amount,
                        sourceAddress = sourceAddress,
                        validatorAddress = validatorAddress,
                    )
                }
            } else {
                estimateGas(
                    pendingActions = pendingActions,
                    amount = amount,
                    sourceAddress = sourceAddress,
                    validatorAddress = validatorAddress,
                )
            }
        }
    }

    private suspend fun estimateGas(
        pendingActions: ImmutableList<PendingAction>,
        amount: BigDecimal,
        sourceAddress: String,
        validatorAddress: String,
    ) {
        val pendingAction = pendingActions.firstOrNull()
        val stakingGasEstimate = estimateGasUseCase(
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
                stakingGasEstimate = stakingGasEstimate,
                pendingActionList = pendingActions,
            ),
        )
    }

    private suspend fun getApproveFee(amount: BigDecimal, validatorAddress: String) {
        val approvalFee = getFeeUseCase(
            amount = amount,
            destination = validatorAddress,
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrencyStatus.currency,
        ).getOrElse {
            // TODO staking error
            return
        }

        stateController.update(
            SetConfirmationStateAssentApprovalTransformer(
                appCurrencyProvider = Provider { appCurrency },
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                fee = approvalFee,
            ),
        )
    }

    override fun onPrevClick() {
        stakingStateRouter.onPrevClick()
    }

    override fun onInitialInfoBannerClick() {
        // innerRouter.openUrl(WHAT_IS_STAKING_ARTICLE_URL)
    }

    override fun onInfoClick(infoType: InfoType) {
        stateController.update(
            ShowInfoBottomSheetStateTransformer(infoType) {
                stateController.update(DismissBottomSheetStateTransformer())
            },
        )
    }

    override fun onAmountValueChange(value: String) {
        stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, yield, value))
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

    override fun openRewardsValidators() = onNextClick(actionType = StakingActionCommonType.PENDING_REWARDS)

    override fun onActiveStake(activeStake: BalanceState) {
        val actionType = if (activeStake.pendingActions.isEmpty()) {
            StakingActionCommonType.EXIT
        } else {
            StakingActionCommonType.PENDING_OTHER
        }
        stateController.update(ValidatorSelectChangeTransformer(activeStake.validator))
        stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, yield, activeStake.cryptoValue))
        onNextClick(actionType, activeStake.pendingActions)
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
                    // TODO staking error
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
                    // TODO staking error
                },
                ifRight = {
                    stateController.update(SetApprovalInProgressTransformer)
                    stateController.update(DismissBottomSheetStateTransformer())
                    awaitForAllowance(confirmationState.pendingActions)
                },
            )
        }.saveIn(approvalJobHolder)
    }

    private fun awaitForAllowance(pendingActions: ImmutableList<PendingAction>) {
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
                        getFee(pendingActions)
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
            clipboardManager.setText(text = txUrl)
        }

        // TODO staking [REDACTED_TASK_KEY]
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
                    // TODO staking error
                },
            )
            getCryptoCurrencyStatusSyncUseCase(userWalletId, cryptoCurrencyId).fold(
                ifRight = {
                    feeCryptoCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId, it).getOrNull()
                    cryptoCurrencyStatus = it

                    setupApprovalNeeded()

                    val networkId = cryptoCurrencyStatus.currency.network.id
                    val isStakeMoreAvailable = isStakeMoreAvailableUseCase(networkId)
                    stateController.update(
                        transformer = SetInitialDataStateTransformer(
                            clickIntents = this@StakingViewModel,
                            yield = yield,
                            isStakeMoreAvailable = isStakeMoreAvailable.getOrElse { false },
                            isApprovalNeeded = stakingApproval is StakingApproval.Needed,
                            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                            userWalletProvider = Provider { userWallet },
                            appCurrencyProvider = Provider { appCurrency },
                        ),
                    )
                },
                ifLeft = {
                    // TODO staking error
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
        transactionId: String,
        gasEstimate: StakingGasEstimate,
        txData: TransactionData,
        pendingActionList: ImmutableList<PendingAction>,
    ) {
        sendTransactionUseCase(
            txData = txData,
            userWallet = userWallet,
            network = cryptoCurrencyStatus.currency.network,
        ).fold(
            ifLeft = { error ->
                Timber.e(error.toString())
                stateController.update(
                    SetConfirmationStateAssentTransformer(
                        appCurrencyProvider = Provider { appCurrency },
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                        stakingGasEstimate = gasEstimate,
                        pendingActionList = pendingActionList,
                    ),
                )
                // todo add error dialog
            },
            ifRight = { txHash ->
                submitHash(transactionId, txHash)
                updateStakeBalance()
                val txUrl = getExplorerTransactionUrlUseCase(
                    txHash = txHash,
                    networkId = cryptoCurrencyStatus.currency.network.id,
                ).getOrElse { "" }

                stateController.update(
                    SetConfirmationStateCompletedTransformer(
                        appCurrencyProvider = Provider { appCurrency },
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                        stakingGasEstimate = gasEstimate,
                        txUrl = txUrl,
                    ),
                )
            },
        )
    }

    private suspend fun submitHash(transactionId: String, transactionHash: String) {
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

    private fun updateStakeBalance() {
        viewModelScope.launch {
            stakingYieldBalanceUseCase(
                userWalletId = userWalletId,
                address = CryptoCurrencyAddress(
                    cryptoCurrencyStatus.currency,
                    cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value.orEmpty(),
                ),
                refresh = true,
            )
        }
    }

    private fun isAssentState(): Boolean {
        return value.currentStep == StakingStep.Confirmation &&
            (value.confirmationState as? StakingStates.ConfirmationState.Data)?.innerState ==
            InnerConfirmationStakingState.ASSENT
    }

    private companion object {
        const val WHAT_IS_STAKING_ARTICLE_URL = "TODO staking"
        const val ALLOWANCE_UPDATE_DELAY = 10_000L
    }
}