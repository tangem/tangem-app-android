package com.tangem.features.txhistory.model

import androidx.compose.runtime.Stable
import arrow.core.Option
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.domain.txhistory.fetcher.AppTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.explorerHash
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.converter.ExpressTxToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryInfoToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUpdateListener
import com.tangem.features.txhistory.state.TxHistoryItemsSnapshot
import com.tangem.features.txhistory.state.TxHistoryStateController
import com.tangem.features.txhistory.utils.HistoryTxListManager
import com.tangem.features.txhistory.utils.TxHistoryListManager
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class TxHistoryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val urlOpener: UrlOpener,
    private val txHistoryUpdateListener: TxHistoryUpdateListener,
    private val stateController: TxHistoryStateController,
    private val designFeatureToggles: DesignFeatureToggles,
    private val txHistoryFeatureToggle: TxHistoryFeatureToggles,
    private val historyTxListManagerFactory: HistoryTxListManager.Factory,
    private val appTxHistoryFetcher: AppTxHistoryFetcher,
    repository: TxHistoryRepositoryV2,
    paramsContainer: ParamsContainer,
    multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    userWalletsListRepository: UserWalletsListRepository,
) : Model(), TxHistoryUiActions {

    private val params: TxHistoryComponent.Params = paramsContainer.require()

    private val lookupDataFlow: Flow<TxHistoryLookupContext> = if (designFeatureToggles.isRedesignEnabled) {
        combine(
            flow = multiAccountStatusListSupplier(),
            flow2 = isAccountsModeEnabledUseCase(),
            flow3 = userWalletsListRepository.userWallets.filterNotNull(),
            transform = ::Triple,
        )
            .map { (accountLists, modeEnabled, wallets) ->
                TxHistoryLookupContext(
                    ownAccountByAddress = buildOwnAccountAddressMap(
                        lists = accountLists,
                        networkRawId = params.currency.network.id.rawId,
                    ),
                    isAccountsModeEnabled = modeEnabled,
                    walletInfoById = wallets.associate { wallet ->
                        wallet.walletId to WalletInfo(
                            name = wallet.name,
                            deviceIconUM = walletIconUMConverter.convert(getWalletIconUseCase(wallet)),
                        )
                    },
                )
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
            .shareIn(modelScope, SharingStarted.WhileSubscribed(), replay = 1)
    } else {
        emptyFlow()
    }

    @RemoveWithToggle("APP_REDESIGN_ENABLED")
    private val legacyTxHistoryItemConverter =
        TxHistoryItemToTransactionStateConverter(currency = params.currency, txHistoryUiActions = this)

    @RemoveWithToggle("AND_15767_NEW_TX_HISTORY_ENABLED")
    private val txHistoryListManager: TxHistoryListManager? = if (!txHistoryFeatureToggle.isNewTxHistoryEnabled) {
        TxHistoryListManager(
            repository = repository,
            dispatchers = dispatchers,
            userWalletId = params.userWalletId,
            currency = params.currency,
            designFeatureToggles = designFeatureToggles,
            txHistoryUiActions = this,
            lookupDataFlow = lookupDataFlow,
            legacyTxHistoryItemConverter = legacyTxHistoryItemConverter,
        )
    } else {
        null
    }

    private val historyTxListManager: HistoryTxListManager? = if (txHistoryFeatureToggle.isNewTxHistoryEnabled) {
        historyTxListManagerFactory.create(
            userWalletId = params.userWalletId,
            currency = params.currency,
        )
    } else {
        null
    }

    val legacyUiState = stateController.legacyUiState
    val uiState = stateController.uiState

    init {
        stateController.setLoading(isBalanceHidden = true, onExploreClick = ::openExplorer)
        handleBalanceHiding()
        subscribeToUiItemChanges()
        initListManager()
        loadTxInfo()
        subscribeToUpdateListener()
        subscribeOnCurrencyStatusUpdates()
    }

    private fun subscribeToUiItemChanges() {
        txHistoryListManager
            ?.uiItems
            ?.onEach { snapshot -> stateController.setContent(
                snapshot = snapshot,
                loadMore = ::loadMoreItems,
                onExploreClick = ::openExplorer,
            ) }
            ?.launchIn(modelScope)
        txHistoryListManager
            ?.paginationStatus
            ?.onEach { paginationStatus -> handlePaginationStatus(paginationStatus) }
            ?.launchIn(modelScope)

        if (historyTxListManager != null) {
            combine(
                flow = historyTxListManager.items,
                flow2 = lookupDataFlow,
                transform = { merged, lookup -> merged to lookup },
            )
                .onEach { (merged, lookup) ->
                    stateController.setContent(
                        snapshot = TxHistoryItemsSnapshot.Items(buildUiItems(merged, lookup)),
                        loadMore = ::loadMoreItems,
                        onExploreClick = ::openExplorer,
                    )
                }
                .flowOn(dispatchers.default)
                .launchIn(modelScope)

            historyTxListManager.paginationStatus
                .onEach { paginationStatus -> handlePaginationStatus(paginationStatus) }
                .launchIn(modelScope)
        }
    }

    private fun buildUiItems(
        merged: List<TxHistoryInfo>,
        lookup: TxHistoryLookupContext,
    ): ImmutableList<TxHistoryItemsUM.TxHistoryItemUM> {
        val converter = TxHistoryInfoToTransactionItemUMConverter(
            txInfoConverter = TxHistoryItemToTransactionItemUMConverter(
                currency = params.currency,
                txHistoryUiActions = this,
                lookupContext = lookup,
            ),
            expressConverter = ExpressTxToTransactionItemUMConverter(
                currency = params.currency,
                txHistoryUiActions = this,
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

    private fun initListManager() {
        modelScope.launch {
            txHistoryListManager?.init()
            historyTxListManager?.init()
        }
    }

    private fun loadTxInfo() {
        stateController.setLoadingIfNotContent(onExploreClick = ::openExplorer)
        modelScope.launch {
            txHistoryItemsCountUseCase.invoke(userWalletId = params.userWalletId, currency = params.currency)
                .onLeft(::handleErrorState)
                .onRight {
                    txHistoryListManager?.startLoading()
                    historyTxListManager?.startLoading()
                }
        }
        if (txHistoryFeatureToggle.isNewTxHistoryEnabled) {
            val trigger = TxHistoryFetchTrigger.TokenDetailsOpen(
                walletId = params.userWalletId,
                currency = params.currency,
            )
            modelScope.launch { appTxHistoryFetcher.invoke(trigger) }
        }
    }

    fun reload() {
        if (stateController.isNotSupported) return

        stateController.setLoadingIfNotContent(onExploreClick = ::openExplorer)
        modelScope.launch {
            txHistoryItemsCountUseCase.invoke(userWalletId = params.userWalletId, currency = params.currency)
                .onLeft(::handleErrorState)
                .onRight {
                    txHistoryListManager?.reload()
                    historyTxListManager?.reload()
                }
            if (txHistoryFeatureToggle.isNewTxHistoryEnabled) {
                val trigger = TxHistoryFetchTrigger.TokenDetailsPTR(
                    walletId = params.userWalletId,
                    currency = params.currency,
                )
                modelScope.launch { appTxHistoryFetcher.invoke(trigger) }
            }
        }
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .map { it.isBalanceHidden }
            .distinctUntilChanged()
            .onEach(stateController::updateBalanceHidden)
            .launchIn(modelScope)
    }

    private fun loadMoreItems(): Boolean {
        modelScope.launch {
            txHistoryListManager?.loadMore(params.userWalletId, params.currency)
            historyTxListManager?.loadMore(params.userWalletId, params.currency)
        }
        return true
    }

    private fun handlePaginationStatus(status: PaginationStatus<*>) {
        when (status) {
            is PaginationStatus.InitialLoadingError -> stateController.setError(
                onReloadClick = ::reload,
                onExploreClick = ::openExplorer,
            )
            PaginationStatus.NextBatchLoading -> stateController.updateLoadingMore(isLoadingMore = true)
            PaginationStatus.EndOfPagination,
            is PaginationStatus.Paginating<*>,
            -> stateController.updateLoadingMore(isLoadingMore = false)
            PaginationStatus.InitialLoading,
            PaginationStatus.None,
            -> Unit
        }
    }

    private fun handleErrorState(error: TxHistoryStateError) {
        when (error) {
            is TxHistoryStateError.DataError -> stateController.setError(
                onReloadClick = ::reload,
                onExploreClick = ::openExplorer,
            )
            TxHistoryStateError.EmptyTxHistories -> stateController.setEmpty(onExploreClick = ::openExplorer)
            TxHistoryStateError.TxHistoryNotImplemented -> stateController.setNotSupported(
                onExploreClick = ::openExplorer,
            )
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        val statusFlow = singleAccountStatusListSupplier(params.userWalletId)
            .map { it.getCryptoCurrencyStatus(currency = params.currency) }
            .distinctUntilChanged()

        val combined: Flow<Pair<Option<CryptoCurrencyStatus>, TxHistoryLookupContext?>> =
            if (designFeatureToggles.isRedesignEnabled) {
                combine(statusFlow, lookupDataFlow) { status, lookup -> status to lookup }
            } else {
                statusFlow.map { it to null }
            }

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
                    )
                    pending.map(converter::convert).toPersistentList()
                },
                legacyPendingTxs = { pending.map(legacyTxHistoryItemConverter::convert).toPersistentList() },
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
        // manager is non-null only under the new tx-history toggle; on the legacy path every tap falls to the explorer.
        val manager = historyTxListManager
        if (manager != null && item.opensInAppDetails()) {
            params.onTxDetailsRequested(manager.txHistoryInfoFlow(item))
        } else {
            item.explorerHash?.let(::openTxInExplorer)
        }
    }
}

/** On-chain transfers/swaps and every express op open the in-app details sheet; everything else goes to the explorer. */
private fun TxHistoryInfo.opensInAppDetails(): Boolean = when (this) {
    is ExpressTx -> true
    is OnChainTx.BSDK -> txInfo.type is TxInfo.TransactionType.Transfer || txInfo.type is TxInfo.TransactionType.Swap
}