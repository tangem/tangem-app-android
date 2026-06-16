package com.tangem.features.txhistory.model

import androidx.compose.runtime.Stable
import arrow.core.Option
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.features.txhistory.entity.TxHistoryUpdateListener
import com.tangem.features.txhistory.state.TxHistoryStateController
import com.tangem.features.txhistory.utils.TxHistoryListManager
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

@Suppress("LongParameterList")
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
                    ownAccountByAddress = buildOwnAccountAddressMap(accountLists),
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

    private val legacyTxHistoryItemConverter =
        TxHistoryItemToTransactionStateConverter(currency = params.currency, txHistoryUiActions = this)
    private val txHistoryListManager = TxHistoryListManager(
        repository = repository,
        dispatchers = dispatchers,
        userWalletId = params.userWalletId,
        currency = params.currency,
        designFeatureToggles = designFeatureToggles,
        txHistoryUiActions = this,
        lookupDataFlow = lookupDataFlow,
        legacyTxHistoryItemConverter = legacyTxHistoryItemConverter,
    )

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

    private fun buildOwnAccountAddressMap(lists: List<AccountStatusList>): Map<String, Account.CryptoPortfolio> {
        val networkRawId = params.currency.network.id.rawId
        val map = mutableMapOf<String, Account.CryptoPortfolio>()
        lists.forEach { accountList ->
            accountList.accountStatuses
                .filterCryptoPortfolio()
                .forEach { status: AccountStatus.CryptoPortfolio ->
                    status.flattenCurrencies().forEach { currencyStatus ->
                        if (currencyStatus.currency.network.id.rawId != networkRawId) return@forEach
                        val address = currencyStatus.value.networkAddress?.defaultAddress?.value ?: return@forEach
                        map[address] = status.account
                    }
                }
        }
        return map
    }

    private fun subscribeToUiItemChanges() {
        txHistoryListManager.uiItems
            .onEach { snapshot ->
                stateController.setContent(
                    snapshot = snapshot,
                    loadMore = ::loadMoreItems,
                    onExploreClick = ::openExplorer,
                )
            }
            .launchIn(modelScope)
        txHistoryListManager.paginationStatus
            .onEach { paginationStatus -> handlePaginationStatus(paginationStatus) }
            .launchIn(modelScope)
    }

    private fun subscribeToUpdateListener() {
        txHistoryUpdateListener.updates
            .onEach { reload() }
            .launchIn(modelScope)
    }

    private fun initListManager() {
        modelScope.launch { txHistoryListManager.init() }
    }

    private fun loadTxInfo() {
        stateController.setLoadingIfNotContent(onExploreClick = ::openExplorer)
        modelScope.launch {
            txHistoryItemsCountUseCase.invoke(userWalletId = params.userWalletId, currency = params.currency)
                .onLeft(::handleErrorState)
                .onRight { txHistoryListManager.startLoading() }
        }
    }

    fun reload() {
        if (stateController.isNotSupported) return

        stateController.setLoadingIfNotContent(onExploreClick = ::openExplorer)
        modelScope.launch {
            txHistoryItemsCountUseCase.invoke(userWalletId = params.userWalletId, currency = params.currency)
                .onLeft(::handleErrorState)
                .onRight { txHistoryListManager.reload() }
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
        modelScope.launch { txHistoryListManager.loadMore(params.userWalletId, params.currency) }
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
}