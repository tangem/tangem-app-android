package com.tangem.feature.tokendetails.presentation.tokendetails.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.ExpressStateFactory
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
    paramsContainer: ParamsContainer,
    expressStatusFactory: ExpressStatusFactory.Factory,
    getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val router: InnerTokenDetailsRouter,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val getAccountCryptoCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val expressTransactionsEventListener: ExpressTransactionsEventListener,
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

    private val waitForFirstExpressStatusEmmit = MutableStateFlow(false)

    private val currentStateProvider: Provider<ExpressTransactionsBlockState> = Provider { internalUiState.value }

    private val stateFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        ExpressStateFactory(
            currentStateProvider = currentStateProvider,
            expressTransactionsClickIntents = this,
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

    override fun onExpressTransactionClick(txId: String) {
        val expressTxState = internalUiState.value.transactionsToDisplay.firstOrNull { it.info.txId == txId }
            ?: return
        internalUiState.value = expressStatusFactory.getStateWithExpressStatusBottomSheet(expressTxState)
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

    override fun onConfirmDisposeExpressStatus() {
        internalUiState.value = stateFactory.getStateWithConfirmHideExpressStatus()
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
        internalUiState.value = stateFactory.getStateWithClosedBottomSheet()
    }

    override fun onDismissBottomSheet() {
        when (val bsContent = internalUiState.value.bottomSheetSlot?.config?.content) {
            is ExpressStatusBottomSheetConfig -> {
                modelScope.launch(dispatchers.main) {
                    expressStatusFactory.removeTransactionOnBottomSheetClosed(bsContent.value)
                }
            }
        }
        internalUiState.value = stateFactory.getStateWithClosedBottomSheet()
    }

    override fun onDismissDialog() {
        internalUiState.value = stateFactory.getStateWithClosedDialog()
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
                }
            }
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        if (accountsFeatureToggles.isFeatureEnabled) {
            getAccountCryptoCurrencyStatusUseCase(userWalletId, cryptoCurrency)
                .onEach { account = it.account }
                .map { it.status.right() }
        } else {
            getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
                userWalletId = userWalletId,
                currencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = userWallet is UserWallet.Cold &&
                    userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
        }
            .distinctUntilChanged()
            .onEach { maybeCurrencyStatus ->
                maybeCurrencyStatus.onRight { status -> cryptoCurrencyStatus = status }
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun subscribeOnExpressTransactionsUpdates() {
        expressTxStatusTaskScheduler.cancelTask()
        expressStatusFactory.getExpressStatuses()
            .distinctUntilChanged()
            .onEach { waitForFirstExpressStatusEmmit.value = true }
            .onEach { expressTxs ->
                internalUiState.value = expressStatusFactory.getStateWithUpdatedExpressTxs(
                    expressTxs = expressTxs,
                    updateBalance = { /* no-op */ },
                )
                expressTxStatusTaskScheduler.scheduleTask(
                    scope = modelScope,
                    task = PeriodicTask(
                        isDelayFirst = false,
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
                                updateBalance = { /* no-op */ },
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