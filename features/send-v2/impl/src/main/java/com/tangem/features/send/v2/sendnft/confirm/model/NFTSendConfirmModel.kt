package com.tangem.features.send.v2.sendnft.confirm.model

import android.os.SystemClock
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.routing.AppRouter
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
import com.tangem.datasource.local.nft.converter.NFTSdkAssetConverter
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.settings.IsSendTapHelpEnabledUseCase
import com.tangem.domain.settings.NeverShowTapHelpUseCase
import com.tangem.domain.transaction.usecase.CreateNFTTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.nft.entity.NFTSendSuccessTrigger
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
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
import com.tangem.features.send.v2.sendnft.analytics.NFTSendAnalyticHelper
import com.tangem.features.send.v2.sendnft.confirm.NFTSendConfirmComponent
import com.tangem.features.send.v2.sendnft.confirm.model.transformers.NFTSendConfirmInitialStateTransformer
import com.tangem.features.send.v2.sendnft.confirm.model.transformers.NFTSendConfirmSendingStateTransformer
import com.tangem.features.send.v2.sendnft.confirm.model.transformers.NFTSendConfirmationNotificationsTransformerV2
import com.tangem.features.send.v2.sendnft.ui.state.NFTSendUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.stripZeroPlainString
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject
import com.tangem.features.send.v2.api.entity.FeeSelectorUM as FeeSelectorUMRedesigned

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class NFTSendConfirmModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val appRouter: AppRouter,
    private val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase,
    private val neverShowTapHelpUseCase: NeverShowTapHelpUseCase,
    private val createNFTTransferTransactionUseCase: CreateNFTTransferTransactionUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val notificationsUpdateTrigger: SendNotificationsUpdateTrigger,
    private val notificationsUpdateListener: SendNotificationsUpdateListener,
    private val feeSelectorCheckReloadTrigger: FeeSelectorCheckReloadTrigger,
    private val feeSelectorCheckReloadListener: FeeSelectorCheckReloadListener,
    private val alertFactory: SendConfirmAlertFactory,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val nftSendAnalyticHelper: NFTSendAnalyticHelper,
    private val nftSendSuccessTrigger: NFTSendSuccessTrigger,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    sendBalanceUpdaterFactory: SendBalanceUpdater.Factory,
) : Model(), NFTSendConfirmClickIntents, SendNotificationsComponent.ModelCallback, FeeSelectorModelCallback {

    private val params: NFTSendConfirmComponent.Params = paramsContainer.require()

    private val analyticsCategoryName = params.analyticsCategoryName
    private val userWallet = params.userWallet
    private val cryptoCurrencyStatus = params.cryptoCurrencyStatus
    private val cryptoCurrency = cryptoCurrencyStatus.currency

    private val _uiState = MutableStateFlow(params.state)
    val uiState = _uiState.asStateFlow()

    private val sendBalanceUpdater = sendBalanceUpdaterFactory.create(
        cryptoCurrency = cryptoCurrency,
        userWallet = userWallet,
    )

    private val destinationUM
        get() = uiState.value.destinationUM as? DestinationUM.Content

    val confirmData: ConfirmData
        get() = ConfirmData(
            enteredDestination = destinationUM?.addressTextField?.actualAddress,
            enteredMemo = destinationUM?.memoTextField?.value,
            fee = (uiState.value.feeSelectorUM as? FeeSelectorUMRedesigned.Content)?.selectedFeeItem?.fee,
            feeError = (uiState.value.feeSelectorUM as? FeeSelectorUMRedesigned.Error)?.error,
        )

    private var sendIdleTimer: Long = 0L

    init {
        configConfirmNavigation()
        subscribeOnNotificationsUpdateTrigger()
        subscribeOnCheckFeeResultUpdates()
        subscribeOnTapHelpUpdates()
        initialState()
    }

    fun updateState(nftSendUM: NFTSendUM) {
        _uiState.value = nftSendUM
        updateConfirmNotifications()
    }

    override fun onFeeResult(feeSelectorUM: FeeSelectorUMRedesigned) {
        sendIdleTimer = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(feeSelectorUM = feeSelectorUM) }
        updateConfirmNotifications()
    }

    fun onDestinationResult(destinationUM: DestinationUM) {
        _uiState.update { it.copy(destinationUM = destinationUM) }
        updateConfirmNotifications()
    }

    override fun onFeeReload() {
        modelScope.launch {
            feeSelectorReloadTrigger.triggerUpdate()
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

    override fun onSendClick() {
        _uiState.update(NFTSendConfirmSendingStateTransformer(isSending = true))
        if (SystemClock.elapsedRealtime() - sendIdleTimer < CHECK_FEE_UPDATE_DELAY) {
            verifyAndSendTransaction()
        } else {
            modelScope.launch {
                feeSelectorCheckReloadTrigger.triggerCheckUpdate()
            }
        }
    }

    override fun onExploreClick() {
        val confirmUM = uiState.value.confirmUM as? ConfirmUM.Success ?: return
        analyticsEventHandler.send(
            CommonSendAnalyticEvents.ExploreButtonClicked(analyticsCategoryName),
        )
        urlOpener.openUrl(confirmUM.txUrl)
    }

    override fun onShareClick() {
        val confirmUM = uiState.value.confirmUM as? ConfirmUM.Success ?: return
        analyticsEventHandler.send(
            CommonSendAnalyticEvents.ShareButtonClicked(analyticsCategoryName),
        )
        shareManager.shareText(confirmUM.txUrl)
    }

    override fun onFailedTxEmailClick(errorMessage: String) {
        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = errorMessage,
                blockchainId = cryptoCurrency.network.rawId,
                derivationPath = cryptoCurrency.network.derivationPath.value,
                destinationAddress = confirmData.enteredDestination.orEmpty(),
                tokenSymbol = null,
                amount = params.nftAsset.amount?.toString().orEmpty(),
                fee = confirmData.fee?.amount?.value?.stripZeroPlainString(),
            ),
        )

        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(walletMetaInfo = metaInfo))
        }
    }

    private fun initialState() {
        val confirmUM = uiState.value.confirmUM
        val isEmptyFee = uiState.value.feeSelectorUM !is FeeSelectorUMRedesigned.Content

        modelScope.launch {
            val isShowTapHelp = isSendTapHelpEnabledUseCase.invokeSync().getOrElse { false }
            if (confirmUM is ConfirmUM.Empty || isEmptyFee) {
                _uiState.update { state ->
                    state.copy(
                        confirmUM = NFTSendConfirmInitialStateTransformer(
                            isShowTapHelp = isShowTapHelp,
                            walletName = stringReference(userWallet.name),
                        ).transform(uiState.value.confirmUM),
                    )
                }
                updateConfirmNotifications()
            }
        }
    }

    private fun subscribeOnTapHelpUpdates() {
        isSendTapHelpEnabledUseCase().getOrNull()
            ?.onEach { showTapHelp ->
                _uiState.update { state ->
                    val confirmUM = state.confirmUM as? ConfirmUM.Content
                    state.copy(confirmUM = confirmUM?.copy(isShowTapHelp = showTapHelp) ?: state.confirmUM)
                }
            }?.launchIn(modelScope)
    }

    private fun subscribeOnNotificationsUpdateTrigger() {
        notificationsUpdateListener.hasErrorFlow
            .onEach { hasError ->
                _uiState.update { state ->
                    val isFeeNotNull = state.feeSelectorUM is FeeSelectorUMRedesigned.Content

                    state.copy(
                        confirmUM = (state.confirmUM as? ConfirmUM.Content)?.copy(
                            isPrimaryButtonEnabled = !hasError && isFeeNotNull,
                        ) ?: state.confirmUM,
                    )
                }
            }
            .launchIn(modelScope)
    }

    private fun verifyAndSendTransaction() {
        val destination = confirmData.enteredDestination ?: return
        val fee = confirmData.fee ?: return
        val ownerAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value ?: return

        val sdkNFTAsset = NFTSdkAssetConverter.convertBack(params.nftAsset)

        modelScope.launch {
            createNFTTransferTransactionUseCase(
                ownerAddress = ownerAddress,
                nftAsset = sdkNFTAsset.second,
                fee = fee,
                memo = confirmData.enteredMemo,
                destinationAddress = destination,
                userWalletId = userWallet.walletId,
                network = cryptoCurrency.network,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error)
                    _uiState.update(NFTSendConfirmSendingStateTransformer(isSending = false))
                    alertFactory.getGenericErrorState(
                        onFailedTxEmailClick = { onFailedTxEmailClick(error.localizedMessage.orEmpty()) },
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
            userWallet = params.userWallet,
            network = params.cryptoCurrencyStatus.currency.network,
        )

        _uiState.update(NFTSendConfirmSendingStateTransformer(isSending = false))

        result.fold(
            ifLeft = { error ->
                Timber.e(error.toString())
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
                        errorCode = error.getAnalyticsDescription(),
                    ),
                )
            },
            ifRight = {
                updateTransactionStatus(txData)
                sendBalanceUpdater.scheduleUpdates()
                nftSendAnalyticHelper.nftSendSuccessAnalytics(cryptoCurrency, uiState.value)
                params.callback.onResult(uiState.value)
                params.onSendTransaction()
            },
        )
    }

    private fun subscribeOnCheckFeeResultUpdates() {
        feeSelectorCheckReloadListener.checkReloadResultFlow.onEach { isFeeResultSuccess ->
            sendIdleTimer = SystemClock.elapsedRealtime()
            if (isFeeResultSuccess) {
                _uiState.update(NFTSendConfirmSendingStateTransformer(isSending = true))
                verifyAndSendTransaction()
            } else {
                _uiState.update(NFTSendConfirmSendingStateTransformer(isSending = false))
            }
        }.launchIn(modelScope)
    }

    private fun updateTransactionStatus(txData: TransactionData.Uncompiled) {
        val txUrl = getExplorerTransactionUrlUseCase(
            txHash = txData.hash.orEmpty(),
            networkId = cryptoCurrency.network.id,
            currency = cryptoCurrency,
        ).getOrElse { "" }
        _uiState.update(NFTSendConfirmSentStateTransformer(txData, txUrl))
    }

    private fun updateConfirmNotifications() {
        modelScope.launch {
            notificationsUpdateTrigger.triggerUpdate(
                data = NotificationData(
                    destinationAddress = confirmData.enteredDestination.orEmpty(),
                    memo = confirmData.enteredMemo,
                    amountValue = BigDecimal.ZERO,
                    reduceAmountBy = BigDecimal.ZERO,
                    isIgnoreReduce = false,
                    fee = confirmData.fee,
                    feeError = confirmData.feeError,
                    feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus, // TODO change when gasless implement
                ),
            )
        }
        _uiState.update { state ->
            state.copy(
                confirmUM = NFTSendConfirmationNotificationsTransformerV2(
                    feeSelectorUM = uiState.value.feeSelectorUM,
                    analyticsEventHandler = analyticsEventHandler,
                    cryptoCurrency = cryptoCurrencyStatus.currency,
                    appCurrency = params.appCurrency,
                    analyticsCategoryName = analyticsCategoryName,
                ).transform(uiState.value.confirmUM),
            )
        }
    }

    private fun configConfirmNavigation() {
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).onEach { (state, route) ->
            val confirmUM = state.confirmUM
            params.callback.onResult(
                state.copy(
                    navigationUM = NavigationUM.Content(
                        source = CommonSendRoute.Confirm.javaClass.simpleName,
                        title = resourceReference(R.string.nft_send),
                        subtitle = null,
                        backIconRes = when (confirmUM) {
                            is ConfirmUM.Success -> R.drawable.ic_close_24
                            else -> R.drawable.ic_back_24
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
                            router.pop()
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
                            ).takeUnless { (confirmUM as? ConfirmUM.Success)?.txUrl.isNullOrBlank() },
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
                    is ConfirmUM.Success -> {
                        modelScope.launch {
                            nftSendSuccessTrigger.triggerSuccessNFTSend()
                        }
                        appRouter.pop()
                    }
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

    private companion object {
        const val CHECK_FEE_UPDATE_DELAY = 10_000L
    }
}