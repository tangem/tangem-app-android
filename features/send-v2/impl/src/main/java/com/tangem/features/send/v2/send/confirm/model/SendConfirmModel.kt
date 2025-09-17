package com.tangem.features.send.v2.send.confirm.model

import android.os.SystemClock
import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.settings.IsSendTapHelpEnabledUseCase
import com.tangem.domain.settings.NeverShowTapHelpUseCase
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeNonce
import com.tangem.features.send.v2.api.params.FeeSelectorParams.FeeStateConfiguration
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateListener
import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.SendBalanceUpdater
import com.tangem.features.send.v2.common.SendConfirmAlertFactory
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.analytics.SendAnalyticHelper
import com.tangem.features.send.v2.send.confirm.SendConfirmComponent
import com.tangem.features.send.v2.send.confirm.model.transformers.*
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.subcomponents.amount.SendAmountReduceTrigger
import com.tangem.features.send.v2.subcomponents.fee.SendFeeCheckReloadListener
import com.tangem.features.send.v2.subcomponents.fee.SendFeeCheckReloadTrigger
import com.tangem.features.send.v2.subcomponents.fee.SendFeeData
import com.tangem.features.send.v2.subcomponents.fee.SendFeeReloadTrigger
import com.tangem.features.send.v2.subcomponents.fee.model.checkAndCalculateSubtractedAmount
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.extensions.stripZeroPlainString
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject
import com.tangem.features.send.v2.api.entity.FeeSelectorUM as FeeSelectorUMRedesigned

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class SendConfirmModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appRouter: AppRouter,
    private val router: Router,
    private val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase,
    private val neverShowTapHelpUseCase: NeverShowTapHelpUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val sendFeeCheckReloadTrigger: SendFeeCheckReloadTrigger,
    private val sendFeeCheckReloadListener: SendFeeCheckReloadListener,
    private val feeSelectorCheckReloadListener: FeeSelectorCheckReloadListener,
    private val feeSelectorCheckReloadTrigger: FeeSelectorCheckReloadTrigger,
    private val notificationsUpdateTrigger: SendNotificationsUpdateTrigger,
    private val notificationsUpdateListener: SendNotificationsUpdateListener,
    private val alertFactory: SendConfirmAlertFactory,
    private val sendAnalyticHelper: SendAnalyticHelper,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    private val feeReloadTrigger: SendFeeReloadTrigger,
    private val sendAmountReduceTrigger: SendAmountReduceTrigger,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    sendBalanceUpdaterFactory: SendBalanceUpdater.Factory,
) : Model(), SendConfirmClickIntents, FeeSelectorModelCallback, SendNotificationsComponent.ModelCallback {

    private val params: SendConfirmComponent.Params = paramsContainer.require()

    private val analyticsCategoryName = params.analyticsCategoryName
    private val userWallet = params.userWallet
    private val appCurrency = params.appCurrency
    private val cryptoCurrencyStatus = params.cryptoCurrencyStatus
    private val cryptoCurrency = cryptoCurrencyStatus.currency

    private val sendBalanceUpdater = sendBalanceUpdaterFactory.create(cryptoCurrency, userWallet)

    private val _uiState = MutableStateFlow(params.state)
    val uiState = _uiState.asStateFlow()

    val isBalanceHiddenFlow: StateFlow<Boolean>
    field = MutableStateFlow(false)

    private val amountState
        get() = uiState.value.amountUM as? AmountState.Data
    private val destinationUM
        get() = uiState.value.destinationUM as? DestinationUM.Content
    private val feeUM
        get() = uiState.value.feeUM as? FeeUM.Content
    private val feeSelectorUM
        get() = feeUM?.feeSelectorUM as? FeeSelectorUM.Content
    private val feeUMV2
        get() = uiState.value.feeSelectorUM as? FeeSelectorUMRedesigned.Content

    val confirmData: ConfirmData
        get() = ConfirmData(
            enteredAmount = amountState?.amountTextField?.cryptoAmount?.value,
            enteredMemo = destinationUM?.memoTextField?.value,
            reduceAmountBy = amountState?.reduceAmountBy.orZero(),
            isIgnoreReduce = amountState?.isIgnoreReduce == true,
            enteredDestination = destinationUM?.addressTextField?.actualAddress,
            fee = if (uiState.value.isRedesignEnabled) {
                feeUMV2?.selectedFeeItem?.fee
            } else {
                feeSelectorUM?.selectedFee
            },
            feeError = if (uiState.value.isRedesignEnabled) {
                (uiState.value.feeSelectorUM as? FeeSelectorUMRedesigned.Error)?.error
            } else {
                (feeUM?.feeSelectorUM as? FeeSelectorUM.Error)?.error
            },
        )

    private var sendIdleTimer: Long = 0L
    private var isAmountSubtractAvailable = false
    internal var feeStateConfiguration: FeeStateConfiguration = FeeStateConfiguration.None

    init {
        modelScope.launch {
            isAmountSubtractAvailable =
                isAmountSubtractAvailableUseCase(userWallet.walletId, cryptoCurrency).getOrElse { false }
        }
        configConfirmNavigation()
        subscribeOnNotificationsUpdateTrigger()
        subscribeOnCheckFeeResultUpdates()
        initialState()
        subscribeOnBalanceHidden()
        subscribeOnTapHelpUpdates()
    }

    fun updateState(state: SendUM) {
        _uiState.value = state
        updateConfirmNotifications()
    }

    fun onFeeResult(feeUM: FeeUM) {
        sendIdleTimer = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(feeUM = feeUM) }
        updateConfirmNotifications()
    }

    fun onAmountResult(amountUM: AmountState) {
        _uiState.update { it.copy(amountUM = amountUM) }
        updateConfirmNotifications()
    }

    fun onDestinationResult(destinationUM: DestinationUM) {
        _uiState.update { it.copy(destinationUM = destinationUM) }
        updateConfirmNotifications()
    }

    override fun onFeeReload() {
        modelScope.launch {
            if (uiState.value.isRedesignEnabled) {
                feeSelectorReloadTrigger.triggerUpdate()
            } else {
                feeReloadTrigger.triggerUpdate(
                    feeData = SendFeeData(
                        amount = confirmData.enteredAmount,
                        destinationAddress = confirmData.enteredDestination,
                    ),
                )
            }
        }
    }

    override fun onAmountReduceTo(reduceTo: BigDecimal) {
        modelScope.launch {
            sendAmountReduceTrigger.triggerReduceTo(reduceTo)
        }
    }

    override fun onAmountReduceBy(reduceBy: BigDecimal, reduceByDiff: BigDecimal) {
        modelScope.launch {
            sendAmountReduceTrigger.triggerReduceBy(
                AmountReduceByTransformer.ReduceByData(
                    reduceAmountBy = reduceBy,
                    reduceAmountByDiff = reduceByDiff,
                ),
            )
        }
    }

    override fun onAmountIgnore() {
        modelScope.launch {
            sendAmountReduceTrigger.triggerIgnoreReduce()
        }
    }

    override fun showEditDestination() {
        modelScope.launch {
            neverShowTapHelpUseCase()
            analyticsEventHandler.send(
                CommonSendAnalyticEvents.ScreenReopened(
                    categoryName = analyticsCategoryName,
                    source = SendScreenSource.Address,
                ),
            )
            router.push(CommonSendRoute.Destination(isEditMode = true))
        }
    }

    override fun showEditAmount() {
        modelScope.launch {
            neverShowTapHelpUseCase()
            analyticsEventHandler.send(
                CommonSendAnalyticEvents.ScreenReopened(
                    categoryName = analyticsCategoryName,
                    source = SendScreenSource.Amount,
                ),
            )
            router.push(CommonSendRoute.Amount(isEditMode = true))
        }
    }

    override fun showEditFee() {
        modelScope.launch {
            neverShowTapHelpUseCase()
            analyticsEventHandler.send(
                CommonSendAnalyticEvents.ScreenReopened(
                    categoryName = analyticsCategoryName,
                    source = SendScreenSource.Fee,
                ),
            )
            router.push(CommonSendRoute.Fee)
        }
    }

    override fun onSendClick() {
        _uiState.update(SendConfirmSendingStateTransformer(isSending = true))
        if (SystemClock.elapsedRealtime() - sendIdleTimer < CHECK_FEE_UPDATE_DELAY) {
            verifyAndSendTransaction()
        } else {
            modelScope.launch {
                if (uiState.value.isRedesignEnabled) {
                    feeSelectorCheckReloadTrigger.triggerCheckUpdate()
                } else {
                    sendFeeCheckReloadTrigger.triggerCheckUpdate()
                }
            }
        }
    }

    override fun onExploreClick() {
        val confirmUM = uiState.value.confirmUM as? ConfirmUM.Success ?: return
        analyticsEventHandler.send(CommonSendAnalyticEvents.ExploreButtonClicked(analyticsCategoryName))
        urlOpener.openUrl(confirmUM.txUrl)
    }

    override fun onShareClick() {
        val confirmUM = uiState.value.confirmUM as? ConfirmUM.Success ?: return
        analyticsEventHandler.send(CommonSendAnalyticEvents.ShareButtonClicked(analyticsCategoryName))
        shareManager.shareText(confirmUM.txUrl)
    }

    override fun onFailedTxEmailClick(errorMessage: String) {
        val amountValue = amountState?.amountTextField?.cryptoAmount?.value
        val feeValue = confirmData.fee?.amount?.value

        val receivingAmount = if (amountValue != null && feeValue != null) {
            checkAndCalculateSubtractedAmount(
                isAmountSubtractAvailable = isAmountSubtractAvailable,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                amountValue = confirmData.enteredAmount.orZero(),
                feeValue = feeValue,
                reduceAmountBy = confirmData.reduceAmountBy,
            )
        } else {
            null
        }

        val amount = receivingAmount?.convertToSdkAmount(cryptoCurrency)

        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = errorMessage,
                blockchainId = cryptoCurrency.network.rawId,
                derivationPath = cryptoCurrency.network.derivationPath.value,
                destinationAddress = confirmData.enteredDestination.orEmpty(),
                tokenSymbol = if (amount?.type is AmountType.Token) {
                    amount.currencySymbol
                } else {
                    ""
                },
                amount = amount?.value?.stripZeroPlainString() ?: "unknown",
                fee = feeValue?.convertToSdkAmount(cryptoCurrency)
                    ?.value?.stripZeroPlainString() ?: "unknown",
            ),
        )

        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase.invoke(userWallet.walletId).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(walletMetaInfo = metaInfo))
        }
    }

    private fun initialState() {
        val confirmUM = uiState.value.confirmUM

        modelScope.launch {
            val isShowTapHelp = isSendTapHelpEnabledUseCase.invokeSync().getOrElse { false }
            if (confirmUM is ConfirmUM.Empty) {
                _uiState.update {
                    it.copy(
                        confirmUM = SendConfirmInitialStateTransformer(
                            isShowTapHelp = isShowTapHelp,
                            walletName = stringReference(userWallet.name),
                        ).transform(uiState.value.confirmUM),
                        confirmData = confirmData,
                    )
                }
                updateConfirmNotifications()
            }
        }
    }

    private fun subscribeOnTapHelpUpdates() {
        isSendTapHelpEnabledUseCase().getOrNull()
            ?.onEach { showTapHelp ->
                _uiState.update {
                    val confirmUM = it.confirmUM as? ConfirmUM.Content
                    it.copy(confirmUM = confirmUM?.copy(showTapHelp = showTapHelp) ?: it.confirmUM)
                }
            }?.launchIn(modelScope)
    }

    private fun subscribeOnNotificationsUpdateTrigger() {
        notificationsUpdateListener.hasErrorFlow
            .onEach { hasError ->
                _uiState.update {
                    if (_uiState.value.isRedesignEnabled) {
                        val feeUM = it.feeSelectorUM as? FeeSelectorUMRedesigned.Content
                        it.copy(
                            confirmUM = (it.confirmUM as? ConfirmUM.Content)?.copy(
                                isPrimaryButtonEnabled = !hasError && feeUM != null,
                            ) ?: it.confirmUM,
                        )
                    } else {
                        val feeUM = it.feeUM as? FeeUM.Content
                        val feeSelectorUM = feeUM?.feeSelectorUM as? FeeSelectorUM.Content
                        it.copy(
                            confirmUM = (it.confirmUM as? ConfirmUM.Content)?.copy(
                                isPrimaryButtonEnabled = !hasError && feeSelectorUM != null,
                            ) ?: it.confirmUM,
                        )
                    }
                }
            }
            .launchIn(modelScope)
    }

    private fun verifyAndSendTransaction() {
        val amountValue = amountState?.amountTextField?.cryptoAmount?.value ?: return
        val destination = destinationUM?.addressTextField?.actualAddress ?: return
        val memo = destinationUM?.memoTextField?.value
        val isRedesignEnabled = uiState.value.isRedesignEnabled
        val fee = if (isRedesignEnabled) {
            feeUMV2?.selectedFeeItem?.fee
        } else {
            feeSelectorUM?.selectedFee
        }
        val nonce = if (isRedesignEnabled) {
            (feeUMV2?.feeNonce as? FeeNonce.Nonce)?.nonce
        } else {
            feeSelectorUM?.nonce
        }
        val feeValue = fee?.amount?.value ?: return

        val receivingAmount = checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = confirmData.reduceAmountBy.orZero(),
        )

        modelScope.launch {
            createTransferTransactionUseCase(
                amount = receivingAmount.convertToSdkAmount(cryptoCurrency),
                fee = fee,
                memo = memo,
                nonce = nonce,
                destination = destination,
                userWalletId = userWallet.walletId,
                network = cryptoCurrency.network,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error)
                    _uiState.update(SendConfirmSendingStateTransformer(isSending = false))
                    alertFactory.getGenericErrorState(
                        onFailedTxEmailClick = {
                            onFailedTxEmailClick(error.localizedMessage.orEmpty())
                        },
                    )
                },
                ifRight = { txData ->
                    sendTransaction(txData)
                },
            )
        }
    }

    private suspend fun sendTransaction(txData: TransactionData.Uncompiled) {
        val result = sendTransactionUseCase(
            txData = txData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        )

        _uiState.update(SendConfirmSendingStateTransformer(isSending = false))

        result.fold(
            ifLeft = { error ->
                alertFactory.getSendTransactionErrorState(
                    error = error,
                    popBack = appRouter::pop,
                    onFailedTxEmailClick = ::onFailedTxEmailClick,
                )
                analyticsEventHandler.send(
                    CommonSendAnalyticEvents.TransactionError(
                        categoryName = analyticsCategoryName,
                        token = cryptoCurrency.symbol,
                        blockchain = cryptoCurrency.network.name,
                    ),
                )
            },
            ifRight = {
                updateTransactionStatus(txData)
                addTokenToWalletIfNeeded()
                sendBalanceUpdater.scheduleUpdates()
                sendAnalyticHelper.sendSuccessAnalytics(cryptoCurrency, uiState.value)
                if (uiState.value.isRedesignEnabled) {
                    params.callback.onResult(uiState.value)
                    params.onSendTransaction()
                }
            },
        )
    }

    private fun addTokenToWalletIfNeeded() {
        if (cryptoCurrency !is CryptoCurrency.Token) return
        val wallets = destinationUM?.wallets ?: return

        val receivingUserWallet = wallets
            .firstOrNull { it.address == confirmData.enteredDestination }
            ?: return

        val userWalletId = receivingUserWallet.userWalletId ?: return
        val network = receivingUserWallet.network ?: return

        modelScope.launch {
            addCryptoCurrenciesUseCase(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                network = network,
            )
        }
    }

    private fun updateTransactionStatus(txData: TransactionData.Uncompiled) {
        val txUrl = getExplorerTransactionUrlUseCase(
            txHash = txData.hash.orEmpty(),
            networkId = cryptoCurrency.network.id,
        ).getOrElse { "" }
        _uiState.update(SendConfirmSentStateTransformer(txData, txUrl))
    }

    private fun subscribeOnCheckFeeResultUpdates() {
        sendFeeCheckReloadListener.checkReloadResultFlow.onEach { isFeeResultSuccess ->
            sendIdleTimer = SystemClock.elapsedRealtime()
            if (isFeeResultSuccess) {
                _uiState.update(SendConfirmSendingStateTransformer(isSending = true))
                verifyAndSendTransaction()
            } else {
                _uiState.update(SendConfirmSendingStateTransformer(isSending = false))
            }
        }.launchIn(modelScope)
        feeSelectorCheckReloadListener.checkReloadResultFlow.onEach { isFeeResultSuccess ->
            sendIdleTimer = SystemClock.elapsedRealtime()
            if (isFeeResultSuccess) {
                _uiState.update(SendConfirmSendingStateTransformer(isSending = true))
                verifyAndSendTransaction()
            } else {
                _uiState.update(SendConfirmSendingStateTransformer(isSending = false))
            }
        }.launchIn(modelScope)
    }

    private fun subscribeOnBalanceHidden() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach { balanceHidingSettings ->
                isBalanceHiddenFlow.update { balanceHidingSettings.isBalanceHidden }
            }
            .launchIn(modelScope)
    }

    private fun updateConfirmNotifications() {
        modelScope.launch {
            notificationsUpdateTrigger.triggerUpdate(
                data = NotificationData(
                    destinationAddress = confirmData.enteredDestination.orEmpty(),
                    memo = confirmData.enteredMemo,
                    amountValue = confirmData.enteredAmount.orZero(),
                    reduceAmountBy = confirmData.reduceAmountBy.orZero(),
                    isIgnoreReduce = confirmData.isIgnoreReduce,
                    fee = confirmData.fee,
                    feeError = confirmData.feeError,
                ),
            )
            _uiState.update {
                it.copy(
                    confirmUM = if (uiState.value.isRedesignEnabled) {
                        SendConfirmationNotificationsTransformerV2(
                            feeSelectorUM = uiState.value.feeSelectorUM,
                            amountUM = uiState.value.amountUM,
                            analyticsEventHandler = analyticsEventHandler,
                            cryptoCurrency = cryptoCurrencyStatus.currency,
                            appCurrency = appCurrency,
                            analyticsCategoryName = params.analyticsCategoryName,
                        ).transform(uiState.value.confirmUM)
                    } else {
                        SendConfirmationNotificationsTransformer(
                            feeUM = uiState.value.feeUM,
                            amountUM = uiState.value.amountUM,
                            analyticsEventHandler = analyticsEventHandler,
                            cryptoCurrency = cryptoCurrencyStatus.currency,
                            appCurrency = appCurrency,
                            analyticsCategoryName = params.analyticsCategoryName,
                        ).transform(uiState.value.confirmUM)
                    },
                )
            }
        }
    }

    @Suppress("LongMethod")
    private fun configConfirmNavigation() {
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).filter {
            it.second is CommonSendRoute.Confirm
        }.onEach { (state, _) ->
            val amountUM = state.amountUM as? AmountState.Data
            val confirmUM = state.confirmUM
            params.callback.onResult(
                state.copy(
                    navigationUM = NavigationUM.Content(
                        title = if (state.isRedesignEnabled) {
                            resourceReference(id = R.string.common_send)
                        } else {
                            resourceReference(
                                id = R.string.send_summary_title,
                                formatArgs = wrappedList(params.cryptoCurrencyStatus.currency.name),
                            )
                        },
                        subtitle = if (uiState.value.isRedesignEnabled) {
                            null
                        } else {
                            amountUM?.title
                        },
                        backIconRes = if (state.isRedesignEnabled) {
                            when (confirmUM) {
                                is ConfirmUM.Success -> R.drawable.ic_close_24
                                else -> R.drawable.ic_back_24
                            }
                        } else {
                            R.drawable.ic_close_24
                        },
                        backIconClick = {
                            analyticsEventHandler.send(
                                CommonSendAnalyticEvents.CloseButtonClicked(
                                    categoryName = analyticsCategoryName,
                                    source = SendScreenSource.Confirm,
                                    isFromSummary = true,
                                    isValid = confirmUM.isPrimaryButtonEnabled,
                                ),
                            )
                            if (state.isRedesignEnabled) {
                                router.pop()
                            } else {
                                appRouter.pop()
                            }
                        },
                        primaryButton = primaryButtonUM(),
                        prevButton = null,
                        secondaryPairButtonsUM = (
                            NavigationButton(
                                textReference = resourceReference(R.string.common_explore),
                                iconRes = R.drawable.ic_web_24,
                                onClick = ::onExploreClick,
                            ) to NavigationButton(
                                textReference = resourceReference(R.string.common_share),
                                iconRes = R.drawable.ic_share_24,
                                onClick = ::onShareClick,
                            )
                            ).takeIf { confirmUM is ConfirmUM.Success },
                    ),
                ),
            )
        }.launchIn(modelScope)
    }

    private fun primaryButtonUM(): NavigationButton {
        val confirmUM = uiState.value.confirmUM
        val isReadyToSend = confirmUM is ConfirmUM.Content && !confirmUM.isSending
        return NavigationButton(
            textReference = when (confirmUM) {
                is ConfirmUM.Success -> resourceReference(R.string.common_close)
                is ConfirmUM.Content -> if (confirmUM.isSending) {
                    resourceReference(R.string.send_sending)
                } else {
                    resourceReference(R.string.common_send)
                }
                else -> resourceReference(R.string.common_send)
            },
            iconRes = walletInterationIcon(userWallet),
            isIconVisible = isReadyToSend,
            isEnabled = confirmUM.isPrimaryButtonEnabled,
            isHapticClick = isReadyToSend,
            onClick = {
                when (confirmUM) {
                    is ConfirmUM.Success -> appRouter.pop()
                    is ConfirmUM.Content -> if (confirmUM.isSending) {
                        return@NavigationButton
                    } else {
                        onSendClick()
                    }
                    else -> return@NavigationButton
                }
            },
        )
    }

    override fun onFeeResult(feeSelectorUM: FeeSelectorUMRedesigned) {
        sendIdleTimer = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(feeSelectorUM = feeSelectorUM) }
        updateConfirmNotifications()
    }

    private companion object {
        const val CHECK_FEE_UPDATE_DELAY = 10_000L
    }
}