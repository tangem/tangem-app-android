package com.tangem.features.txhistory.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.GetCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.txhistory.entity.TxHistoryUpdateListener
import com.tangem.features.txhistory.utils.TxHistoryListManager
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TxHistoryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val urlOpener: UrlOpener,
    private val txHistoryUpdateListener: TxHistoryUpdateListener,
    repository: TxHistoryRepositoryV2,
    paramsContainer: ParamsContainer,
) : Model(), TxHistoryUiActions {

    private val params: TxHistoryComponent.Params = paramsContainer.require()
    private val txHistoryItemConverter =
        TxHistoryItemToTransactionStateConverter(currency = params.currency, txHistoryUiActions = this)
    private val txHistoryListManager = TxHistoryListManager(
        repository = repository,
        dispatchers = dispatchers,
        userWalletId = params.userWalletId,
        currency = params.currency,
        txHistoryItemConverter = txHistoryItemConverter,
        txHistoryUiActions = this,
    )
    private val _uiState: MutableStateFlow<TxHistoryUM> =
        MutableStateFlow(TxHistoryUM.Loading(isBalanceHidden = true, onExploreClick = ::openExplorer))
    val uiState: StateFlow<TxHistoryUM> = _uiState.asStateFlow()

    init {
        handleBalanceHiding()
        subscribeToUiItemChanges()
        initListManager()
        loadTxInfo()
        subscribeToUpdateListener()
        subscribeOnCurrencyStatusUpdates()
    }

    private fun subscribeToUiItemChanges() {
        txHistoryListManager.uiItems
            .onEach { updateState(it) }
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
        _uiState.update { state -> getLoadingState(state.isBalanceHidden) }
        modelScope.launch {
            txHistoryItemsCountUseCase.invoke(userWalletId = params.userWalletId, currency = params.currency)
                .onLeft(::handleErrorState)
                .onRight { txHistoryListManager.startLoading() }
        }
    }

    fun reload() {
        // fast exit
        if (uiState.value is TxHistoryUM.NotSupported) return

        _uiState.update { state ->
            if (state !is TxHistoryUM.Content) getLoadingState(state.isBalanceHidden) else state
        }
        modelScope.launch {
            txHistoryItemsCountUseCase.invoke(userWalletId = params.userWalletId, currency = params.currency)
                .onLeft(::handleErrorState)
                .onRight { txHistoryListManager.reload() }
        }
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .onEach { _uiState.update { state -> state.copySealed(isBalanceHidden = it.isBalanceHidden) } }
            .launchIn(modelScope)
    }

    private fun loadMoreItems(): Boolean {
        modelScope.launch { txHistoryListManager.loadMore(params.userWalletId, params.currency) }
        return true
    }

    private fun updateState(items: ImmutableList<TxHistoryUM.TxHistoryItemUM>) {
        _uiState.update { state ->
            if (state is TxHistoryUM.Content) {
                state.copy(items = items)
            } else {
                TxHistoryUM.Content(
                    items = items,
                    isBalanceHidden = state.isBalanceHidden,
                    loadMore = ::loadMoreItems,
                )
            }
        }
    }

    private fun handlePaginationStatus(status: PaginationStatus<*>) {
        _uiState.update { state ->
            when (status) {
                is PaginationStatus.InitialLoadingError -> getErrorState(state.isBalanceHidden)
                PaginationStatus.EndOfPagination,
                PaginationStatus.InitialLoading,
                PaginationStatus.NextBatchLoading,
                PaginationStatus.None,
                is PaginationStatus.Paginating<*>,
                -> state
            }
        }
    }

    private fun handleErrorState(error: TxHistoryStateError) {
        _uiState.update { state ->
            when (error) {
                is TxHistoryStateError.DataError -> getErrorState(isBalanceHidden = state.isBalanceHidden)
                TxHistoryStateError.EmptyTxHistories -> TxHistoryUM.Empty(
                    isBalanceHidden = state.isBalanceHidden,
                    onExploreClick = ::openExplorer,
                )
                TxHistoryStateError.TxHistoryNotImplemented -> TxHistoryUM.NotSupported(
                    isBalanceHidden = state.isBalanceHidden,
                    pendingTransactions = persistentListOf(),
                    onExploreClick = ::openExplorer,
                )
            }
        }
    }

    private fun getErrorState(isBalanceHidden: Boolean): TxHistoryUM.Error {
        return TxHistoryUM.Error(
            isBalanceHidden = isBalanceHidden,
            onReloadClick = ::reload,
            onExploreClick = ::openExplorer,
        )
    }

    private fun getLoadingState(isBalanceHidden: Boolean): TxHistoryUM.Loading {
        return TxHistoryUM.Loading(isBalanceHidden = isBalanceHidden, onExploreClick = ::openExplorer)
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        val userWallet: UserWallet = requireNotNull(getUserWalletUseCase(params.userWalletId).getOrNull()) {
            "User wallet not found"
        }
        getCurrencyStatusUpdatesUseCase(
            userWalletId = params.userWalletId,
            currencyId = params.currency.id,
            isSingleWalletWithTokens = userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
        )
            .distinctUntilChanged()
            .onEach(::handlePendingTxsChanges)
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun handlePendingTxsChanges(maybeCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>) {
        maybeCurrencyStatus.onRight { status ->
            val pendingTxs = status.value.pendingTransactions
                .map(txHistoryItemConverter::convert)
                .toPersistentList()
            _uiState.update { state ->
                if (state is TxHistoryUM.NotSupported) {
                    state.copy(pendingTransactions = pendingTxs)
                } else {
                    state
                }
            }
        }
    }

    override fun openExplorer() {
        params.openExplorer()
    }

    override fun openTxInExplorer(txHash: String) {
        getExplorerTransactionUrlUseCase(
            txHash = txHash,
            networkId = params.currency.network.id,
        ).fold(
            ifLeft = { Timber.e(it.toString()) },
            ifRight = { urlOpener.openUrl(url = it) },
        )
    }
}