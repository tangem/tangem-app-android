package com.tangem.features.staking.impl.presentation.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
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
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.*
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.*
import com.tangem.domain.staking.analytics.StakeScreenSource
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.*
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.utils.getValidatorsCount
import com.tangem.domain.tokens.*
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.analytics.StakingParamsInterceptor
import com.tangem.features.staking.impl.analytics.utils.StakingAnalyticSender
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.events.StakingAlertUM
import com.tangem.features.staking.impl.presentation.state.events.StakingEvent
import com.tangem.features.staking.impl.presentation.state.events.StakingEventFactory
import com.tangem.features.staking.impl.presentation.state.helpers.StakingBalanceUpdater
import com.tangem.features.staking.impl.presentation.state.helpers.StakingFeeLoader
import com.tangem.features.staking.impl.presentation.state.helpers.StakingOperationsFactory
import com.tangem.features.staking.impl.presentation.state.helpers.StakingTransactionSender
import com.tangem.features.staking.impl.presentation.state.transformers.*
import com.tangem.features.staking.impl.presentation.state.transformers.amount.*
import com.tangem.features.staking.impl.presentation.state.transformers.approval.*
import com.tangem.features.staking.impl.presentation.state.transformers.confirmation.SetUpdatedAllowanceTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.AddStakingNotificationsTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.DismissStakingNotificationsStateTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.CompleteInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.SetFeeErrorToTonInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.SetFeeToTonInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.ton.ShowTonInitializeBottomSheetTransformer
import com.tangem.features.staking.impl.presentation.state.transformers.validator.ValidatorSelectChangeTransformer
import com.tangem.features.staking.impl.presentation.state.utils.checkAndCalculateSubtractedAmount
import com.tangem.features.staking.impl.presentation.state.utils.isSingleAction
import com.tangem.features.staking.impl.presentation.state.utils.withStubUnstakeAction
import com.tangem.lib.crypto.BlockchainUtils.isTon
import com.tangem.utils.Provider
import com.tangem.utils.TangemBlogUrlBuilder.RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP
import com.tangem.utils.coroutines.*
import com.tangem.utils.extensions.isSingleItem
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getAllowanceUseCase: GetAllowanceUseCase,
    private val vibratorHapticManager: VibratorHapticManager,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val isAnyTokenStakedUseCase: IsAnyTokenStakedUseCase,
    private val invalidatePendingTransactionsUseCase: InvalidatePendingTransactionsUseCase,
    private val stakingOperationsFactory: StakingOperationsFactory,
    private val stakingBalanceUpdater: StakingBalanceUpdater.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getActionsUseCase: GetActionsUseCase,
    private val getYieldUseCase: GetYieldUseCase,
    private val p2pEthPoolRepository: P2PEthPoolRepository,
    private val checkAccountInitializedUseCase: CheckAccountInitializedUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getNetworkAddressesUseCase: GetNetworkAddressesUseCase,
    private val getActionRequirementAmountUseCase: GetActionRequirementAmountUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val paramsInterceptorHolder: ParamsInterceptorHolder,
    private val shareManager: ShareManager,
    private val urlOpener: UrlOpener,
    @DelayedWork private val coroutineScope: CoroutineScope,
    private val innerRouter: InnerStakingRouter,
    private val messageSender: UiMessageSender,
    private val accountsFeatureToggles: AccountsFeatureToggles,
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

    private val cryptoCurrencyId: CryptoCurrency.ID = params.cryptoCurrency.id
    private val userWalletId: UserWalletId = params.userWalletId
    private val integration: StakingIntegration = runBlocking {
        when (val integrationId = params.integrationId) {
            is StakingIntegrationID.StakeKit -> {
                val yield = getYieldUseCase(integrationId.value).getOrElse {
                    error("yield must be not null")
                }
                StakeKitIntegration(integrationId, yield)
            }
            StakingIntegrationID.P2PEthPool -> {
                val vaults = p2pEthPoolRepository.getVaultsSync()
                P2PEthPoolIntegration(integrationId, vaults)
            }
        }
    }

    private var isAccountInitialized: Boolean = true
    private var isTonHeatupCase: Boolean = false
    private lateinit var cryptoCurrencyStatus: CryptoCurrencyStatus
    private var stakingActions: List<StakingAction> = emptyList()
    private var feeCryptoCurrencyStatus: CryptoCurrencyStatus? = null
    private var minimumTransactionAmount: EnterAmountBoundary? = null
    private val isBalanceHiddenFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private var tonAccountInitializeTransaction: TransactionData.Uncompiled? = null

    private val userWallet by lazy {
        requireNotNull(
            getUserWalletUseCase(userWalletId).getOrNull(),
        ) { "No wallet found for id: $userWalletId" }
    }
    private var appCurrency: AppCurrency by Delegates.notNull()

    private val balancesToShow: List<StakingBalanceEntry>
        get() {
            val stakingBalance = cryptoCurrencyStatus.value.stakingBalance
            return when (stakingBalance) {
                is StakingBalance.Data.StakeKit -> {
                    val invalidatedItems = invalidatePendingTransactionsUseCase(
                        balanceItems = stakingBalance.balance.items,
                        stakingActions = stakingActions,
                        token = integration.token,
                    ).getOrElse { emptyList() }
                    invalidatedItems.toStakingBalanceEntries()
                }
                is StakingBalance.Data.P2PEthPool -> stakingBalance.entries
                else -> emptyList()
            }
        }

    private var isInitialInfoAnalyticSent: Boolean = false
    private var isBalanceUpdatedAfterStart: Boolean = false

    private val balanceUpdater by lazy(LazyThreadSafetyMode.NONE) {
        stakingBalanceUpdater.create(
            cryptoCurrencyStatus,
            userWallet,
            integration,
        )
    }

    private val feeLoader: StakingFeeLoader by lazy(LazyThreadSafetyMode.NONE) {
        stakingOperationsFactory.createFeeLoader(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWallet = userWallet,
            integration = integration,
        )
    }

    private val transactionSender: StakingTransactionSender by lazy(LazyThreadSafetyMode.NONE) {
        stakingOperationsFactory.createTransactionSender(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWallet = userWallet,
            integration = integration,
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

    private var isAccountsModeEnabled: Boolean = true
    private var account: Account.CryptoPortfolio? = null

    private val transactionsInProgress: CopyOnWriteArrayList<StakingTransaction> = CopyOnWriteArrayList()

    private var actionsJobHolder: JobHolder = JobHolder()
    private var approvalJobHolder: JobHolder = JobHolder()
    private var feeJobHolder: JobHolder = JobHolder()
    private var sendTransactionJobHolder = JobHolder()
    private var stepChangesJobHolder = JobHolder()

    init {
        subscribeOnSelectedAppCurrency()
        subscribeOnCurrencyStatusUpdates()
        stateController.initializeWithUserWallet(userWallet)
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
            val hasNoYieldBalanceData = cryptoCurrencyStatus.value.stakingBalance !is StakingBalance.Data.StakeKit

            when {
                isInitialInfoStep && noBalanceState && integration.areAllTargetsFull && hasNoYieldBalanceData -> {
                    stakingEventFactory.createStakingValidatorsUnavailableAlert()
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
                            integration = integration,
                        ).let(::add)
                        if (integration.isPartialAmountDisabled) {
                            ValidatorSelectChangeTransformer(
                                selectedTarget = integration.preferredTargets.firstOrNull(),
                                integration = integration,
                            ).let(::add)
                            SetAmountDataTransformer(
                                clickIntents = this@StakingModel,
                                cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                                userWalletProvider = Provider { userWallet },
                                appCurrencyProvider = Provider { appCurrency },
                                isBalanceHidden = isBalanceHiddenFlow.value,
                                isAccountsModeEnabled = isAccountsModeEnabled,
                                account = account,
                            ).let(::add)
                            AmountMaxValueStateTransformer(
                                cryptoCurrencyStatus = cryptoCurrencyStatus,
                                minimumTransactionAmount = minimumTransactionAmount,
                                actionType = uiState.value.actionType,
                                integration = integration,
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
                integration = integration,
                appCurrency = appCurrency,
                cryptoCurrency = cryptoCurrencyStatus.currency,
            ),
        )
        modelScope.launch {
            feeLoader.getFee(
                onStakingFee = { gasEstimate, isFeeApproximate ->
                    stateController.update(
                        SetConfirmationStateAssentTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = gasEstimate,
                            isFeeApproximate = isFeeApproximate,
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
                    analyticsEventHandler.send(
                        StakingAnalyticsEvent.TransactionError(
                            errorCode = "GetFeeError",
                        ),
                    )
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

                if (integration is P2PEthPoolIntegration) {
                    checkFeeAndSendP2PTransaction()
                } else {
                    sendTransaction()
                }
            }.saveIn(sendTransactionJobHolder)
        }
    }

    private suspend fun checkFeeAndSendP2PTransaction() {
        val confirmationState = value.confirmationState as? StakingStates.ConfirmationState.Data
        val currentFeeState = confirmationState?.feeState as? FeeState.Content
        val currentFee = currentFeeState?.fee

        var actualFee: Fee? = null
        feeLoader.getFee(
            onStakingFee = { fee, _ ->
                actualFee = fee
            },
            onStakingFeeError = { error ->
                stakingEventFactory.createStakingErrorAlert(error)
                stateController.update(SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus))
            },
            onApprovalFee = { },
            onFeeError = { error ->
                stateController.update(SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus))
                updateNotifications(error)
            },
        )

        val newFee = actualFee ?: return

        val currentFeeAmount = when (currentFee) {
            is Fee.Common -> currentFee.amount.value ?: BigDecimal.ZERO
            is Fee.Ethereum -> currentFee.amount.value ?: BigDecimal.ZERO
            else -> BigDecimal.ZERO
        }

        val newFeeAmount = when (newFee) {
            is Fee.Common -> newFee.amount.value ?: BigDecimal.ZERO
            is Fee.Ethereum -> newFee.amount.value ?: BigDecimal.ZERO
            else -> BigDecimal.ZERO
        }

        if (newFeeAmount > currentFeeAmount) {
            stateController.update(SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus))
            stakingEventFactory.createNetworkFeeUpdatedAlert(
                onConfirm = {
                    modelScope.launch {
                        stateController.update(UpdateConfirmationFeeTransformer(newFee))
                        stateController.update(SetConfirmationStateInProgressTransformer())
                        sendTransaction()
                    }
                },
            )
        } else {
            sendTransaction()
        }
    }

    private suspend fun sendTransaction() {
        transactionSender.send(
            StakingTransactionSender.Callbacks(
                onConstructSuccess = { constructedTransactions ->
                    transactionsInProgress.addAll(constructedTransactions)
                },
                onConstructError = { error ->
                    stakingEventFactory.createStakingErrorAlert(error)
                    stateController.update(SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus))
                },
                onSendSuccess = { txUrl ->
                    stakingAnalyticSender.sendTransactionStakingAnalytics(
                        stateController.value,
                        cryptoCurrencyStatus,
                    )
                    transactionsInProgress.clear()
                    stateController.update(
                        SetConfirmationStateCompletedTransformer(txUrl, cryptoCurrencyStatus),
                    )
                },
                onSendError = { error ->
                    analyticsEventHandler.send(
                        StakingAnalyticsEvent.TransactionError(
                            errorCode = error.getAnalyticsDescription(),
                        ),
                    )
                    stakingEventFactory.createSendTransactionErrorAlert(error)
                    stateController.update(SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus))
                },
                onFeeIncreased = { increasedFee, isFeeApproximate ->
                    stateController.updateAll(
                        SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus),
                        SetConfirmationStateAssentTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = increasedFee,
                            isFeeApproximate = isFeeApproximate,
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
                onTransactionExpired = {
                    stateController.update(SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus))
                    getFee()
                },
            ),
        )
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
        analyticsEventHandler.send(StakingAnalyticsEvent.WhatIsStaking())
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
        if (integration.preferredTargets.isEmpty()) {
            stateController.updateEvent(
                StakingEvent.ShowAlert(StakingAlertUM.NoAvailableValidators),
            )
        } else {
            if (uiState.value.actionType is StakingActionCommonType.Enter) {
                stateController.updateAll(
                    ValidatorSelectChangeTransformer(
                        selectedTarget = null,
                        integration = integration,
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
                integration = integration,
            ),
        )
    }

    override fun onAmountPasteTriggerDismiss() {
        stateController.update(AmountPasteDismissStateTransformer())
    }

    override fun onMaxValueClick() {
        analyticsEventHandler.send(StakingAnalyticsEvent.ButtonMax())
        stateController.update(
            AmountMaxValueStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                minimumTransactionAmount = minimumTransactionAmount,
                actionType = uiState.value.actionType,
                integration = integration,
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

    override fun onTargetSelect(target: StakingTarget) {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.ValidatorChosen(
                validator = target.name,
            ),
        )
        stateController.update(
            ValidatorSelectChangeTransformer(
                selectedTarget = target,
                integration = integration,
            ),
        )
    }

    override fun openRewardsValidators() {
        analyticsEventHandler.send(StakingAnalyticsEvent.ButtonRewards())
        val rewardsValidators =
            stateController.value.rewardsValidatorsState as? StakingStates.RewardsValidatorsState.Data

        val initialInfoState = stateController.value.initialInfoState as? StakingStates.InitialInfoState.Data
        val yieldBalance = initialInfoState?.yieldBalance as? InnerYieldBalanceState.Data
        val rewardBlockType = yieldBalance?.reward?.rewardBlockType
        val rewardPendingActionConstraints = yieldBalance?.reward?.rewardConstraints

        if (rewardBlockType is RewardBlockType.RewardsRequirementsError) {
            val minimumAmount = rewardPendingActionConstraints?.amountArg?.minimum
            // Temporary fix, until StakeKit adds minimum requirement amount to balance response
            val minimumAmountValue = if (minimumAmount == null && yieldBalance.integrationId != null) {
                getActionRequirementAmountUseCase.invoke(
                    integrationId = yieldBalance.integrationId,
                    actionType = StakingActionType.CLAIM_REWARDS,
                )
            } else {
                minimumAmount
            }

            stakingEventFactory.createStakingRewardsMinimumRequirementsErrorAlert(
                cryptoCurrencyName = cryptoCurrencyStatus.currency.name,
                cryptoAmountValue = minimumAmountValue?.format {
                    crypto(cryptoCurrencyStatus.currency)
                }.orEmpty(),
            )
        } else {
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
    }

    override fun onActiveStake(activeStake: BalanceState) {
        val networkId = cryptoCurrencyStatus.currency.network.rawId
        val preferredValidators = (integration as? StakeKitIntegration)?.targets
            ?.filterIsInstance<StakingTarget.Validator>()
            ?.filter { it.delegate.preferred }
            .orEmpty()
        val pendingActions = activeStake.pendingActions.mapNotNull { action ->
            if (action.type in listOf(StakingActionType.RESTAKE, StakingActionType.STAKE) &&
                preferredValidators.isSingleItem()
            ) {
                null
            } else {
                action
            }
        }.toImmutableList()
        if (isSingleAction(networkId, pendingActions)) {
            prepareForConfirmation(
                balanceType = activeStake.type,
                pendingActions = pendingActions,
                balanceState = activeStake,
                target = activeStake.target,
                amountValue = activeStake.cryptoValue,
            )
            onNextClick(activeStake)
        } else {
            stateController.update(
                ShowActionSelectorBottomSheetTransformer(
                    pendingActions = withStubUnstakeAction(networkId, pendingActions, activeStake),
                    onActionSelect = { action ->
                        prepareForConfirmation(
                            balanceType = activeStake.type,
                            pendingAction = action,
                            balanceState = activeStake,
                            target = activeStake.target,
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
                userWallet = userWallet,
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
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                userWalletId = userWalletId,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error.toString())
                    analyticsEventHandler.send(
                        StakingAnalyticsEvent.TransactionError(
                            errorCode = "CreateApprovalTxError",
                        ),
                    )
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
                    analyticsEventHandler.send(
                        StakingAnalyticsEvent.TransactionError(
                            errorCode = error.getAnalyticsDescription(),
                        ),
                    )
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
                feeCurrencyStatus = feeCryptoCurrencyStatus,
                amount = amount,
                fee = fee,
                feeCurrencyBalanceAfterTransaction = balanceAfterTransaction,
            )
            stateController.update(
                AddStakingNotificationsTransformer(
                    cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                    appCurrencyProvider = Provider { appCurrency },
                    isAccountInitializedProvider = Provider { isAccountInitialized },
                    feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                    currencyWarning = currencyWarning,
                    currencyCheck = currencyStatus,
                    isSubtractAvailable = isAmountSubtractAvailable,
                    feeError = feeError,
                    stakingError = stakingError,
                    integration = integration,
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
        analyticsEventHandler.send(StakingAnalyticsEvent.ButtonExplore())
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

        analyticsEventHandler.send(StakingAnalyticsEvent.ButtonShare())
        if (txUrl != null) {
            vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
            shareManager.shareText(txUrl)
        }
    }

    override fun onFailedTxEmailClick(errorMessage: String) {
        modelScope.launch {
            val network = cryptoCurrencyStatus.currency.network

            val metaInfo =
                getWalletMetaInfoUseCase(userWallet.walletId).getOrElse { error("CardInfo must be not null") }
            val amountState = uiState.value.amountState as? AmountState.Data
            val confirmationState = uiState.value.confirmationState as? StakingStates.ConfirmationState.Data
            val validatorState = uiState.value.validatorState as? StakingStates.ValidatorState.Data
            val feeState = confirmationState?.feeState as? FeeState.Content

            val target = validatorState?.chosenTarget
            val feeAmount = feeState?.fee?.amount
            val amount = amountState?.amountTextField?.cryptoAmount
            saveBlockchainErrorUseCase(
                error = BlockchainErrorInfo(
                    errorMessage = errorMessage,
                    blockchainId = network.rawId,
                    derivationPath = network.derivationPath.value,
                    destinationAddress = target?.address.orEmpty(),
                    tokenSymbol = (cryptoCurrencyStatus.currency as? CryptoCurrency.Token)?.symbol,
                    amount = amount?.run { value?.toPlainString() + currencySymbol }.orEmpty(),
                    fee = feeAmount?.run { value?.toPlainString() + currencySymbol }.orEmpty(),
                ),
            )

            val email = FeedbackEmailType.StakingProblem(
                walletMetaInfo = metaInfo,
                validatorName = target?.name,
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

    override fun onOpenLearnMoreAboutApproveClick() {
        urlOpener.openUrl(RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP)
    }

    override fun onActivateTonAccountNotificationClick() {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.UninitializedAddressScreen(
                token = cryptoCurrencyStatus.currency.symbol,
            ),
        )
        modelScope.launch {
            stateController.update(
                ShowTonInitializeBottomSheetTransformer(
                    onDismiss = { stateController.update(DismissBottomSheetStateTransformer) },
                ),
            )

            val tonAmount = BigDecimal(1)
            val destination = getNetworkAddressesUseCase.invokeSync(
                userWalletId = userWalletId,
                network = cryptoCurrencyStatus.currency.network,
            ).getOrNull(0)?.address ?: return@launch

            val transaction = createTransferTransactionUseCase(
                amount = tonAmount.convertToSdkAmount(cryptoCurrencyStatus),
                destination = destination,
                userWalletId = userWalletId,
                network = cryptoCurrencyStatus.currency.network,
                memo = null,
            )
            tonAccountInitializeTransaction = transaction.getOrElse {
                stateController.update(
                    SetFeeErrorToTonInitializeBottomSheetTransformer(),
                )
                return@launch
            }

            val transactionFee = getFeeUseCase(
                userWallet = userWallet,
                network = cryptoCurrencyStatus.currency.network,
                transactionData = tonAccountInitializeTransaction!!,
            )

            transactionFee.fold(
                ifLeft = {
                    stateController.update(
                        SetFeeErrorToTonInitializeBottomSheetTransformer(),
                    )
                },
                ifRight = {
                    stateController.update(
                        SetFeeToTonInitializeBottomSheetTransformer(
                            appCurrencyProvider = Provider { appCurrency },
                            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                            fee = it.normal,
                            isFeeApproximate = false,
                        ),
                    )
                },
            )
        }
    }

    override fun onActivateTonAccountNotificationShow() {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.UninitializedAddress(
                token = cryptoCurrencyStatus.currency.symbol,
            ),
        )
    }

    override fun onNotEnoughFeeNotificationShow() {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.NotEnoughFee(
                token = cryptoCurrencyStatus.currency.symbol,
            ),
        )
    }

    override fun onActivateTonAccountClick() {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.ButtonActivate(
                token = cryptoCurrencyStatus.currency.symbol,
            ),
        )
        modelScope.launch {
            tonAccountInitializeTransaction?.let { transaction ->
                sendTransactionUseCase.invoke(
                    txData = transaction,
                    userWallet = userWallet,
                    network = cryptoCurrencyStatus.currency.network,
                ).fold(
                    ifLeft = {
                        messageSender.send(
                            DialogMessage(
                                title = resourceReference(id = R.string.send_alert_transaction_failed_title),
                                message = resourceReference(id = R.string.common_unknown_error),
                            ),
                        )
                    },
                    ifRight = {
                        stateController.update(
                            CompleteInitializeBottomSheetTransformer(
                                cryptoCurrencyStatus = cryptoCurrencyStatus,
                                minimumTransactionAmount = minimumTransactionAmount,
                            ),
                        )

                        balanceUpdater.partialUpdateWithDelay()
                        stateController.update(DismissBottomSheetStateTransformer)
                    },
                )
            }
        }
    }

    override fun onAmountReduceByFeeClick(reduceAmount: BigDecimal, notification: Class<out NotificationUM>) {
        stateController.update(
            AmountReduceByStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                value = ReduceByData(reduceAmount, reduceAmount),
                minimumTransactionAmount = null,
            ),
        )
        onNotificationCancel(notification)
    }

    private suspend fun setupApprovalNeeded() {
        val approval = StakingIntegrationID.create(currencyId = cryptoCurrencyStatus.currency.id)?.approval
            ?: StakingApproval.Empty

        stakingApproval = approval

        if (approval is StakingApproval.Needed) {
            stakingAllowance = getAllowanceUseCase(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrencyStatus.currency,
                spenderAddress = approval.spenderAddress,
            ).getOrElse { BigDecimal.ZERO }
        }
    }

    private suspend fun setupIsAnyTokenStaked() {
        isAnyTokenStaked = isAnyTokenStakedUseCase(userWalletId).getOrNull() == true
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        if (accountsFeatureToggles.isFeatureEnabled) {
            getAccountCurrencyStatusUseCase(
                userWalletId = params.userWalletId,
                currency = params.cryptoCurrency,
            ).conflate().distinctUntilChanged()
                .filter {
                    value.currentStep == StakingStep.InitialInfo || isTopHeatupCase()
                }.onEach { (maybeAccount, maybeStatus) ->
                    isAccountsModeEnabled = isAccountsModeEnabledUseCase.invokeSync()
                    account = maybeAccount
                    onDataLoaded(maybeStatus)
                }.flowOn(dispatchers.main)
                .launchIn(modelScope)
        } else {
            getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
                userWalletId = userWalletId,
                currencyId = cryptoCurrencyId,
                isSingleWalletWithTokens = false,
            ).conflate().distinctUntilChanged()
                .filter {
                    value.currentStep == StakingStep.InitialInfo || isTopHeatupCase()
                }
                .onEach { maybeStatus ->
                    maybeStatus.fold(
                        ifRight = { onDataLoaded(it) },
                        ifLeft = { error ->
                            stakingEventFactory.createGenericErrorAlert(error.toString())
                            stateController.update(
                                SetConfirmationStateResetAssentTransformer(cryptoCurrencyStatus = cryptoCurrencyStatus),
                            )
                        },
                    )
                }.flowOn(dispatchers.main)
                .launchIn(modelScope)
        }
    }

    private suspend fun onDataLoaded(status: CryptoCurrencyStatus) {
        if (!isInitialInfoAnalyticSent) {
            isInitialInfoAnalyticSent = true
            val balances = status.value.stakingBalance as? StakingBalance.Data.StakeKit
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

        checkForTonHeatupCase()
        setupApprovalNeeded()
        setupIsAnyTokenStaked()
        checkIfSubtractAvailable()
        subscribeOnActionsUpdates(status)
        subscribeOnStepChanges(status)
        subscribeOnBalanceHiding()
    }

    private fun subscribeOnBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                isBalanceHiddenFlow.value = it.isBalanceHidden
                stateController.update(
                    transformer = HideBalanceStateTransformer(
                        isBalanceHidden = it.isBalanceHidden,
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        appCurrency = appCurrency,
                    ),
                )
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

                        if (!isBalanceUpdatedAfterStart) {
                            isBalanceUpdatedAfterStart = true
                            balanceUpdater.partialUpdate()
                        }
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
                    stakingActions = actions
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
                integration = integration,
                isAnyTokenStaked = isAnyTokenStaked,
                cryptoCurrencyStatus = status,
                userWalletProvider = Provider { userWallet },
                appCurrencyProvider = Provider { appCurrency },
                balancesToShowProvider = Provider { balancesToShow },
                isAccountsModeEnabled = isAccountsModeEnabled,
                account = account,
                isBalanceHidden = isBalanceHiddenFlow.value,
            ),
            SetConfirmationStateEmptyTransformer,
        )
    }

    private fun prepareForConfirmation(
        balanceType: BalanceType,
        balanceState: BalanceState,
        pendingActions: ImmutableList<PendingAction> = persistentListOf(),
        pendingAction: PendingAction? = pendingActions.firstOrNull(),
        target: StakingTarget?,
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
                integration = integration,
            ),
            ValidatorSelectChangeTransformer(
                selectedTarget = target,
                integration = integration,
            ),
            SetAmountDataTransformer(
                clickIntents = this,
                cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
                userWalletProvider = Provider { userWallet },
                appCurrencyProvider = Provider { appCurrency },
                isAccountsModeEnabled = isAccountsModeEnabled,
                isBalanceHidden = isBalanceHiddenFlow.value,
                account = account,
            ),
            AmountChangeStateTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                value = amountValue,
                minimumTransactionAmount = minimumTransactionAmount,
                integration = integration,
            ),
        )
    }

    private suspend fun checkForTonHeatupCase() {
        val isAccountInitializedNewValue = checkAccountInitializedUseCase.invoke(
            userWalletId = userWalletId,
            network = cryptoCurrencyStatus.currency.network,
        ).getOrElse {
            Timber.e(it)
            false
        }

        if (!isAccountInitializedNewValue && isAccountInitialized) {
            isTonHeatupCase = true
        }

        isAccountInitialized = isAccountInitializedNewValue

        if (isTopHeatupCase()) {
            updateNotifications()
        }
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
        isAmountSubtractAvailable = isAmountSubtractAvailableUseCase(
            userWalletId = userWalletId,
            currency = cryptoCurrencyStatus.currency,
        ).getOrElse { false }
    }

    private fun isTopHeatupCase(): Boolean {
        if (!::cryptoCurrencyStatus.isInitialized) return false
        return value.currentStep == StakingStep.Confirmation &&
            isTon(cryptoCurrencyStatus.currency.network.rawId) &&
            isTonHeatupCase
    }

    private companion object {
        const val WHAT_IS_STAKING_ARTICLE_URL = "https://tangem.com/en/blog/post/how-to-stake-cryptocurrency/"
        const val ALLOWANCE_UPDATE_DELAY = 10_000L
    }
}