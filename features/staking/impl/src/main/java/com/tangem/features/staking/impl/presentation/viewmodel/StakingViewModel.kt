package com.tangem.features.staking.impl.presentation.viewmodel

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.staking.*
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
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
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LargeClass", "LongParameterList")
@HiltViewModel
internal class StakingViewModel @Inject constructor(
    private val stateController: StakingStateController,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getCryptoCurrencyStatusSyncUseCase: GetCryptoCurrencyStatusSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getStakingTransactionUseCase: GetStakingTransactionUseCase,
    private val estimateGasUseCase: EstimateGasUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val saveUnsubmittedHashUseCase: SaveUnsubmittedHashUseCase,
    private val submitHashUseCase: SubmitHashUseCase,
    private val isStakeMoreAvailableUseCase: IsStakeMoreAvailableUseCase,
    private val stakingYieldBalanceUseCase: FetchStakingYieldBalanceUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, StakingClickIntents {

    val uiState: StateFlow<StakingUiState> = stateController.uiState
    val value: StakingUiState get() = uiState.value

    var stakingStateRouter: StakingStateRouter by Delegates.notNull()
        private set

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

    private var innerRouter: InnerStakingRouter by Delegates.notNull()
    private var userWallet: UserWallet by Delegates.notNull()
    private var appCurrency: AppCurrency by Delegates.notNull()

    init {
        subscribeOnSelectedAppCurrency()
        subscribeOnBalanceHiding()
        subscribeOnCurrencyStatusUpdates()
    }

    override fun onBackClick() {
        stakingStateRouter.onBackClick()
    }

    override fun onNextClick(pendingActions: ImmutableList<PendingAction>) {
        stakingStateRouter.onNextClick()
        if (isAssentState()) {
            estimateGas(pendingActions)
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

                val stakingTransaction = getStakingTransactionUseCase(
                    userWalletId = userWalletId,
                    network = cryptoCurrencyStatus.currency.network,
                    params = ActionParams(
                        actionCommonType = value.actionType,
                        integrationId = yield.id,
                        amount = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                            ?: error("No amount provided"),
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

                stakingTransaction.unsignedTransaction?.let {
                    sendStakingTransaction(
                        transactionId = stakingTransaction.id,
                        gasEstimate = stakingTransaction.gasEstimate ?: error("No gas estimate available"),
                        txData = TransactionData.Compiled(value = it.hexToBytes()),
                        pendingActionList = confirmationState.pendingActions,
                    )
                } ?: error("No unsigned transaction available")
            }
        }
    }

    private fun estimateGas(pendingActions: ImmutableList<PendingAction>) {
        viewModelScope.launch {
            stateController.update(
                SetConfirmationStateLoadingTransformer(
                    yield = yield,
                ),
            )
            val cryptoCurrencyValue = cryptoCurrencyStatus.value

            val pendingAction = pendingActions.firstOrNull()
            val stakingGasEstimate = estimateGasUseCase(
                userWalletId = userWalletId,
                network = cryptoCurrencyStatus.currency.network,
                params = ActionParams(
                    actionCommonType = value.actionType,
                    integrationId = yield.id,
                    amount = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                        ?: error("No amount provided"),
                    address = cryptoCurrencyValue.networkAddress?.defaultAddress?.value
                        ?: error("No available address"),
                    validatorAddress = yield.validators.getOrNull(0)?.address ?: error("No available validator"),
                    token = yield.token,
                    passthrough = pendingAction?.passthrough,
                    type = pendingAction?.type,
                ),
            ).getOrElse {
                stateController.update(AddStakingErrorTransformer(it))
                return@launch
            }

            stateController.update(
                SetConfirmationStateAssentTransformer(
                    appCurrencyProvider = Provider { appCurrency },
                    cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                    stakingGasEstimate = stakingGasEstimate,
                    pendingActionList = pendingActions,
                ),
            )
        }
    }

    override fun onPrevClick() {
        stakingStateRouter.onPrevClick()
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

    override fun openRewardsValidators() {
        stateController.update { it.copy(actionType = StakingActionCommonType.PENDING_REWARDS) }
        onNextClick()
    }

    override fun onActiveStake(activeStake: BalanceState) {
        val actionType = if (activeStake.pendingActions.isEmpty()) {
            StakingActionCommonType.EXIT
        } else {
            StakingActionCommonType.PENDING_OTHER
        }
        stateController.update { it.copy(actionType = actionType) }
        stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, yield, activeStake.cryptoValue))
        onNextClick(activeStake.pendingActions)
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
// [REDACTED_TODO_COMMENT]
    }

    fun setRouter(router: InnerStakingRouter, stateRouter: StakingStateRouter) {
        innerRouter = router
        this.stakingStateRouter = stateRouter
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        viewModelScope.launch {
            getUserWalletUseCase(userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet
                },
                ifLeft = {
// [REDACTED_TODO_COMMENT]
                },
            )
            getCryptoCurrencyStatusSyncUseCase(userWalletId, cryptoCurrencyId).fold(
                ifRight = {
                    cryptoCurrencyStatus = it

                    val networkId = cryptoCurrencyStatus.currency.network.id
                    val isStakeMoreAvailable = isStakeMoreAvailableUseCase(networkId)
                    stateController.update(
                        transformer = SetInitialDataStateTransformer(
                            clickIntents = this@StakingViewModel,
                            yield = yield,
                            isStakeMoreAvailable = isStakeMoreAvailable.getOrElse { false },
                            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                            userWalletProvider = Provider { userWallet },
                            appCurrencyProvider = Provider { appCurrency },
                        ),
                    )
                },
                ifLeft = {
// [REDACTED_TODO_COMMENT]
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
                        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                        stakingGasEstimate = gasEstimate,
                        pendingActionList = pendingActionList,
                    ),
                )
// [REDACTED_TODO_COMMENT]
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
                        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
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
}
