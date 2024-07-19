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
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
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

    override fun onNextClick() {
        handleOnNextConfirmationClick()
        stakingStateRouter.onNextClick()
        if (isAssentState()) {
            initStaking()
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

                val actionType = when (value.routeType) {
                    RouteType.STAKE -> StakingActionCommonType.ENTER
                    RouteType.UNSTAKE -> StakingActionCommonType.EXIT
                    RouteType.CLAIM -> TODO()
                }

                val stakingTransaction = getStakingTransactionUseCase(
                    params = ActionParams(
                        actionCommonType = actionType,
                        integrationId = yield.id,
                        amount = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                            ?: error("No amount provided"),
                        address = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
                            ?: error("No available address"),
                        validatorAddress = validatorState.chosenValidator.address,
                        token = yield.token,
                    ),
                ).getOrElse {
                    error(it)
                }

                stakingTransaction.unsignedTransaction?.let {
                    sendStakingTransaction(
                        transactionId = stakingTransaction.id,
                        gasEstimate = stakingTransaction.gasEstimate ?: error("No gas estimate available"),
                        TransactionData.Compiled(value = it.hexToBytes()),
                    )
                } ?: error("No unsigned transaction available")
            }
        }
    }

    private fun initStaking() {
        viewModelScope.launch {
            stateController.update(
                SetConfirmationStateLoadingTransformer(
                    yield = yield,
                ),
            )

            val actionType = when (value.routeType) {
                RouteType.STAKE -> StakingActionCommonType.ENTER
                RouteType.UNSTAKE -> StakingActionCommonType.EXIT
                RouteType.CLAIM -> TODO()
            }

            val stakingGasEstimate = estimateGasUseCase(
                params = ActionParams(
                    actionCommonType = actionType,
                    integrationId = yield.id,
                    amount = (value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                        ?: error("No amount provided"),
                    address = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
                        ?: error("No available address"),
                    validatorAddress = yield.validators.getOrNull(0)?.address ?: error("No available validator"),
                    token = yield.token,
                ),
            ).getOrElse { error("Can't get fee info") }

            stateController.update(
                SetConfirmationStateAssentTransformer(
                    appCurrencyProvider = Provider { appCurrency },
                    cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                    stakingGasEstimate = stakingGasEstimate,
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
        stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, value))
    }

    override fun onAmountPasteTriggerDismiss() {
        stateController.update(AmountPasteDismissStateTransformer())
    }

    override fun onMaxValueClick() {
        stateController.update(AmountMaxValueStateTransformer(cryptoCurrencyStatus))
    }

    override fun onCurrencyChangeClick(isFiat: Boolean) {
        stateController.update(AmountCurrencyChangeStateTransformer(cryptoCurrencyStatus, isFiat))
    }

    override fun openValidators() = stakingStateRouter.showValidators()

    override fun onValidatorSelect(validator: Yield.Validator) {
        stateController.update(ValidatorSelectChangeTransformer(validator))
    }

    override fun openRewardsValidators() {
        stateController.update { it.copy(routeType = RouteType.CLAIM) }
        onNextClick()
    }

    override fun selectRewardValidator(rewardValue: String) {
        stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, rewardValue))
        onNextClick()
    }

    override fun onActiveStake(activeStake: BalanceState) {
        stateController.update { it.copy(routeType = RouteType.UNSTAKE) }
        stateController.update(AmountChangeStateTransformer(cryptoCurrencyStatus, activeStake.cryptoValue))
        onNextClick()
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
                    ),
                )
            },
            ifRight = { txHash ->
                submitHash(transactionId, txHash)

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

    private fun isAssentState(): Boolean {
        return value.currentStep == StakingStep.Confirmation &&
            (value.confirmationState as? StakingStates.ConfirmationState.Data)?.innerState ==
            InnerConfirmationStakingState.ASSENT
    }
}
