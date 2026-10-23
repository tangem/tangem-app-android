package com.tangem.features.txhistory.model

import androidx.compose.runtime.Stable
import arrow.core.Option
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.txhistory.fetcher.AppTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.domain.txhistory.list.HistoryTxListManager
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.usecase.GetCachedTokenInfoUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.tokendetails.deeplink.ExpressDeepLinkListener
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.component.TxHistoryDetailsSlotConfig
import com.tangem.features.txhistory.converter.ExpressTxToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryInfoToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUpdateListener
import com.tangem.features.txhistory.joint.JointHistoryPoller
import com.tangem.features.txhistory.state.TxHistoryItemsSnapshot
import com.tangem.features.txhistory.state.TxHistoryStateController
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class TxHistoryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val urlOpener: UrlOpener,
    private val txHistoryUpdateListener: TxHistoryUpdateListener,
    private val stateController: TxHistoryStateController,
    private val historyTxListManagerFactory: HistoryTxListManager.Factory,
    private val appTxHistoryFetcher: AppTxHistoryFetcher,
    private val router: Router,
    private val cryptoCurrencyDeepLinkListener: ExpressDeepLinkListener,
    private val vibratorHapticManager: VibratorHapticManager,
    getCachedTokenInfoUseCase: GetCachedTokenInfoUseCase,
    paramsContainer: ParamsContainer,
    ownerLookupProducer: TxHistoryOwnerLookupProducer,
) : Model(), TxHistoryUiActions {

    private val params: TxHistoryComponent.Params = paramsContainer.require()
    val txDetailsNavigation = SlotNavigation<TxHistoryDetailsSlotConfig>()

    private val lookupDataFlow: Flow<TxHistoryLookupContext> = ownerLookupProducer(params.userWalletId)
        .flowOn(dispatchers.default)
        .shareIn(modelScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val networkTokensFlow: StateFlow<Set<CryptoCurrency.Token>> = flow {
        emit(getCachedTokenInfoUseCase(params.userWalletId, params.currency.network.rawId))
    }
        .flowOn(dispatchers.io)
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = emptySet())

    val historyTxListManager: HistoryTxListManager = historyTxListManagerFactory.create(
        userWalletId = params.userWalletId,
        currency = params.currency,
        modelScope = modelScope,
    )

    private val isResumed = MutableStateFlow(false)
    private val loadedPagesTracker = LoadedPagesTracker()

    val uiState = stateController.uiState

    init {
        stateController.setLoading(isBalanceHidden = true, onExploreClick = ::openExplorer)
        handleBalanceHiding()
        subscribeToUiItemChanges()
        loadTxInfo()
        subscribeToUpdateListener()
        JointHistoryPoller.launchIn(
            scope = modelScope,
            currency = params.currency,
            isResumed = isResumed,
            hasLoadedMorePages = loadedPagesTracker.hasLoadedMorePages,
            reload = historyTxListManager::reload,
        )
        subscribeOnCurrencyStatusUpdates()
        deepLinkListener()
    }

    /** Opens the given currency's Token Details on top of this screen (e.g. the refunded token from the tx details sheet). */
    fun openTokenDetails(currency: CryptoCurrency) {
        val route = AppRoute.CurrencyDetails(
            userWalletId = params.userWalletId,
            currency = currency,
        )
        router.push(route)
    }

    private fun subscribeToUiItemChanges() {
        combine(
            flow = historyTxListManager.state,
            flow2 = lookupDataFlow,
            flow3 = networkTokensFlow,
            transform = { state, lookup, tokens -> Triple(state, lookup, tokens) },
        )
            .onEach { (state, lookup, tokens) -> applyHistoryState(state, lookup, tokens) }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun applyHistoryState(
        state: HistoryTxListManager.HistoryState,
        lookup: TxHistoryLookupContext,
        networkTokens: Set<CryptoCurrency.Token>,
    ) {
        loadedPagesTracker.onState(state)
        when (state) {
            HistoryTxListManager.HistoryState.Loading ->
                stateController.setLoadingIfNotContent(onExploreClick = ::openExplorer)
            HistoryTxListManager.HistoryState.Unavailable ->
                stateController.setNotSupported(onExploreClick = ::openExplorer)
            HistoryTxListManager.HistoryState.Empty ->
                stateController.setEmpty(onExploreClick = ::openExplorer)
            HistoryTxListManager.HistoryState.Error ->
                stateController.setError(onReloadClick = ::reload, onExploreClick = ::openExplorer)
            is HistoryTxListManager.HistoryState.Content -> {
                stateController.setContent(
                    snapshot = TxHistoryItemsSnapshot.Items(buildUiItems(state.items, lookup, networkTokens)),
                    loadMore = ::loadMoreItems,
                    onExploreClick = ::openExplorer,
                )
                stateController.updateLoadingMore(isLoadingMore = state.isLoadingMore)
            }
        }
    }

    private fun buildUiItems(
        merged: List<TxHistoryInfo>,
        lookup: TxHistoryLookupContext,
        networkTokens: Set<CryptoCurrency.Token>,
    ): ImmutableList<TxHistoryItemsUM.TxHistoryItemUM> {
        val converter = TxHistoryInfoToTransactionItemUMConverter(
            txInfoConverter = TxHistoryItemToTransactionItemUMConverter(
                currency = params.currency,
                txHistoryUiActions = this,
                lookupContext = lookup,
                networkTokens = networkTokens,
            ),
            expressConverter = ExpressTxToTransactionItemUMConverter(
                currency = params.currency,
                txHistoryUiActions = this,
                lookup = lookup,
            ),
            txHistoryUiActions = this,
        )

        val items = mutableListOf<TxHistoryItemsUM.TxHistoryItemUM>()
        var lastDate: String? = null
        merged.forEach { tx ->
            val date = tx.timestampMillis.toDateFormatWithTodayYesterday()
            if (date != lastDate) {
                items += TxHistoryItemsUM.TxHistoryItemUM.GroupTitle(title = date, itemKey = "group-$date")
                lastDate = date
            }
            items += TxHistoryItemsUM.TxHistoryItemUM.Transaction(converter.convert(tx))
        }
        return items.toImmutableList()
    }

    private fun subscribeToUpdateListener() {
        txHistoryUpdateListener.updates
            .onEach { reload() }
            .launchIn(modelScope)
    }

    private fun deepLinkListener() {
        cryptoCurrencyDeepLinkListener.actionFlow
            .onEach { txId -> openHistoryDetailsById(txId) }
            .launchIn(modelScope)
    }

    private fun loadTxInfo() {
        stateController.setLoadingIfNotContent(onExploreClick = ::openExplorer)
        val trigger = TxHistoryFetchTrigger.TokenDetailsOpen(
            walletId = params.userWalletId,
            currency = params.currency,
        )
        appTxHistoryFetcher.invoke(trigger)
    }

    fun onResume() {
        isResumed.value = true
    }

    fun onPause() {
        isResumed.value = false
    }

    fun reload() {
        if (stateController.isNotSupported) return
        loadedPagesTracker.onReload()
        historyTxListManager.reload()

        val trigger = TxHistoryFetchTrigger.TokenDetailsPTR(
            walletId = params.userWalletId,
            currency = params.currency,
        )
        appTxHistoryFetcher.invoke(trigger)
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .map { it.isBalanceHidden }
            .distinctUntilChanged()
            .onEach(stateController::updateBalanceHidden)
            .launchIn(modelScope)
    }

    private fun loadMoreItems(): Boolean {
        loadedPagesTracker.onLoadMore(current = historyTxListManager.state.value)
        historyTxListManager.loadMore()
        return true
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        val statusFlow = singleAccountStatusListSupplier(params.userWalletId)
            .map { it.getCryptoCurrencyStatus(currency = params.currency) }
            .distinctUntilChanged()

        val combined: Flow<Pair<Option<CryptoCurrencyStatus>, TxHistoryLookupContext?>> =
            combine(statusFlow, lookupDataFlow) { status, lookup -> status to lookup }

        combined
            .onEach { (status, lookup) -> handlePendingTxsChanges(status, lookup) }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun handlePendingTxsChanges(
        maybeCurrencyStatus: Option<CryptoCurrencyStatus>,
        lookupContext: TxHistoryLookupContext?,
    ) {
        maybeCurrencyStatus.onSome { status ->
            val pending = status.value.pendingTransactions
            stateController.updatePendingTransactions(
                pendingTxs = {
                    val converter = TxHistoryItemToTransactionItemUMConverter(
                        currency = params.currency,
                        txHistoryUiActions = this,
                        lookupContext = lookupContext,
                        networkTokens = networkTokensFlow.value,
                    )
                    pending.map(converter::convert).toPersistentList()
                },
            )
        }
    }

    override fun openExplorer() {
        params.openExplorer()
    }

    override fun openTxInExplorer(txHash: String) {
        getExplorerTransactionUrlUseCase(
            txHash = txHash,
            currency = params.currency,
        ).fold(
            ifLeft = { TangemLogger.e(it.toString()) },
            ifRight = { urlOpener.openUrl(url = it) },
        )
    }

    override fun onTransactionClick(item: TxHistoryInfo) {
        vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Tick)
        txDetailsNavigation.activate(TxHistoryDetailsSlotConfig(item.txId))
    }

    private fun openHistoryDetailsById(txId: String) {
        txDetailsNavigation.activate(TxHistoryDetailsSlotConfig(txId))
    }
}