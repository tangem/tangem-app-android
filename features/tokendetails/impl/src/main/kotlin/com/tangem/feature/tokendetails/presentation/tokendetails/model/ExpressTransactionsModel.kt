package com.tangem.feature.tokendetails.presentation.tokendetails.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.tokendetails.impl.R
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.ExpressStateFactory
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express.ExpressStatusFactory
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.features.tokendetails.ExpressTransactionsEvent
import com.tangem.features.tokendetails.ExpressTransactionsEventListener
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongParameterList", "PropertyUsedBeforeDeclaration")
@Stable
@ModelScoped
internal class ExpressTransactionsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    paramsContainer: ParamsContainer,
    expressStatusFactory: ExpressStatusFactory.Factory,
    getUserWalletUseCase: GetUserWalletUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val router: InnerTokenDetailsRouter,
    private val getAccountCryptoCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val expressTransactionsEventListener: ExpressTransactionsEventListener,
    private val updateDelayedNetworkStatusUseCase: UpdateDelayedNetworkStatusUseCase,
) : Model(), ExpressTransactionsClickIntents {

    private val params = paramsContainer.require<ExpressTransactionsComponent.Params>()
    private val userWalletId: UserWalletId = params.userWalletId
    private val cryptoCurrency: CryptoCurrency = params.currency

    private val userWallet: UserWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: error("UserWallet not found")

    private val marketPriceJobHolder = JobHolder()
    private val expressTxJobHolder = JobHolder()
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null
    private var account: Account.CryptoPortfolio? = null
    private val expressTxStatusTaskScheduler = SingleTaskScheduler<PersistentList<ExpressTransactionStateUM>>()

    private val waitForFirstExpressStatusEmit = MutableStateFlow(false)

    private val currentStateProvider: Provider<ExpressTransactionsBlockState> = Provider { internalUiState.value }

    private val stateFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        ExpressStateFactory(
            currentStateProvider = currentStateProvider,
        )
    }

    private val expressStatusFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        expressStatusFactory.create(
            clickIntents = this,
            appCurrencyProvider = Provider { selectedAppCurrencyFlow.value },
            currentStateProvider = currentStateProvider,
            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
        )
    }

    private val internalUiState = MutableStateFlow(stateFactory.getInitialState())
    val uiState: StateFlow<ExpressTransactionsBlockState> = internalUiState

    init {
        subscribeOnExternalEvents()
        subscribeOnCurrencyStatusUpdates()
        subscribeOnExpressTransactionsUpdates()
    }

    fun onResume() {
        subscribeOnExpressTransactionsUpdates()
    }

    fun onPause() {
        clear()
    }

    override fun onExpressTransactionClick(txId: String) {
        val expressTxState = internalUiState.value.transactionsToDisplay.firstOrNull { it.info.txId == txId }
            ?: return
        internalUiState.value = expressStatusFactory.getStateWithExpressStatusBottomSheet(expressTxState)
        if (expressTxState is ExchangeUM) {
            expressTxState.info.txExternalId?.let { txExternalId ->
                params.onRatingRequested?.invoke(
                    txExternalId,
                    expressTxState.provider.name,
                    expressTxState.info.txExternalUrl.orEmpty(),
                    expressTxState.fromUserWalletId.stringValue,
                )
            }
        }
    }

    override fun onGoToProviderClick(url: String) {
        router.openUrl(url)
    }

    override fun onGoToRefundedTokenClick(cryptoCurrency: CryptoCurrency) {
        router.openTokenDetails(userWalletId, cryptoCurrency)
    }

    override fun onOpenUrlClick(url: String) {
        router.openUrl(url)
    }

    override fun onReadAboutCrossChainBridgesClick() {
        modelScope.launch {
            router.openUrl(TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.AboutCrossChainBridges))
        }
    }

    override fun onConfirmDisposeExpressStatus() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.express_status_hide_dialog_title),
                message = resourceReference(R.string.express_status_hide_dialog_text),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_hide),
                        onClick = {
                            onDisposeExpressStatus()
                        },
                    )
                },
                secondActionBuilder = { cancelAction() },
            ),
        )
    }

    override fun onDisposeExpressStatus() {
        val bottomSheetState = internalUiState.value.bottomSheetSlot?.config?.content
        if (bottomSheetState is ExpressStatusBottomSheetConfig) {
            modelScope.launch {
                expressStatusFactory.removeTransactionOnBottomSheetClosed(
                    expressState = bottomSheetState.value,
                    isForceDispose = true,
                )
            }
        }
        params.onRatingDismiss?.invoke()
        internalUiState.value = stateFactory.getStateWithClosedBottomSheet()
    }

    override fun onDismissBottomSheet() {
        when (val bsContent = internalUiState.value.bottomSheetSlot?.config?.content) {
            is ExpressStatusBottomSheetConfig -> {
                modelScope.launch(dispatchers.mainImmediate) {
                    expressStatusFactory.removeTransactionOnBottomSheetClosed(bsContent.value)
                }
            }
        }
        params.onRatingDismiss?.invoke()
        internalUiState.value = stateFactory.getStateWithClosedBottomSheet()
    }

    override fun onDestroy() {
        clear()
        super.onDestroy()
    }

    private fun subscribeOnExternalEvents() {
        modelScope.launch {
            expressTransactionsEventListener.event.collect { event ->
                when (event) {
                    ExpressTransactionsEvent.Update -> subscribeOnExpressTransactionsUpdates()
                    ExpressTransactionsEvent.Clear -> clear()
                    is ExpressTransactionsEvent.OpenTx -> openTxOnFirstEmit(event.txId)
                }
            }
        }
    }

    private fun openTxOnFirstEmit(txId: String) {
        modelScope.launch {
            waitForFirstExpressStatusEmit.first { it }
            onExpressTransactionClick(txId)
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        getAccountCryptoCurrencyStatusUseCase(userWalletId, cryptoCurrency)
            .onEach { account = it.account }
            .distinctUntilChanged()
            .onEach { cryptoCurrencyStatus = it.status }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun subscribeOnExpressTransactionsUpdates() {
        expressTxStatusTaskScheduler.cancelTask()
        expressStatusFactory.getExpressStatuses()
            .distinctUntilChanged()
            .onEach { waitForFirstExpressStatusEmit.value = true }
            .onEach { expressTxs ->
                internalUiState.value = expressStatusFactory.getStateWithUpdatedExpressTxs(
                    expressTxs = expressTxs,
                    updateBalance = ::updateNetworkToSwapBalance,
                )
                expressTxStatusTaskScheduler.scheduleTask(
                    scope = modelScope,
                    task = PeriodicTask(
                        delay = EXPRESS_STATUS_UPDATE_DELAY,
                        task = {
                            try {
                                Result.success(
                                    expressStatusFactory.getUpdatedExpressStatuses(internalUiState.value.transactions),
                                )
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                Result.failure(exception)
                            }
                        },
                        onSuccess = { updatedTxs ->
                            internalUiState.value = expressStatusFactory.getStateWithUpdatedExpressTxs(
                                expressTxs = updatedTxs,
                                updateBalance = ::updateNetworkToSwapBalance,
                            )
                        },
                        onError = { /* no-op */ },
                    ),
                )
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(expressTxJobHolder)
    }

    private fun updateNetworkToSwapBalance(toCryptoCurrency: CryptoCurrency) {
        modelScope.launch {
            updateDelayedNetworkStatusUseCase(
                userWalletId = userWalletId,
                network = toCryptoCurrency.network,
            )
        }
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { maybeAppCurrency ->
                maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppCurrency.Default,
            )
    }

    private fun clear() {
        expressTxStatusTaskScheduler.cancelTask()
        expressTxJobHolder.cancel()
    }

    private companion object {
        const val EXPRESS_STATUS_UPDATE_DELAY = 10_000L
    }
}