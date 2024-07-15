package com.tangem.feature.wallet.presentation.organizetokens

import androidx.lifecycle.*
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.ToggleTokenListGroupingUseCase
import com.tangem.domain.tokens.ToggleTokenListSortingUseCase
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.organizetokens.analytics.PortfolioOrganizeTokensAnalyticsEvent
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.CryptoCurrenciesIdsResolver
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.disableSortingByBalance
import com.tangem.feature.wallet.presentation.organizetokens.utils.dnd.DragAndDropAdapter
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.router.WalletRoute
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
internal class OrganizeTokensViewModel @Inject constructor(
    private val getTokenListUseCase: GetTokenListUseCase,
    private val toggleTokenListGroupingUseCase: ToggleTokenListGroupingUseCase,
    private val toggleTokenListSortingUseCase: ToggleTokenListSortingUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, OrganizeTokensIntents {

    lateinit var router: InnerWalletRouter

    private val selectedAppCurrencyFlow = createSelectedAppCurrencyFlow()

    private var isBalanceHidden = true

    private val dragAndDropAdapter = DragAndDropAdapter(
        listStateProvider = Provider { uiState.value.itemsState },
    )

    private val stateHolder = OrganizeTokensStateHolder(
        intents = this,
        dragAndDropIntents = dragAndDropAdapter,
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
    )

    private val userWalletId: UserWalletId by lazy {
        val userWalletIdValue: String = checkNotNull(savedStateHandle[WalletRoute.userWalletIdKey])

        UserWalletId(userWalletIdValue)
    }

    private var cachedTokenList: TokenList? = null

    val uiState: StateFlow<OrganizeTokensState> = stateHolder.stateFlow

    override fun onCreate(owner: LifecycleOwner) {
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ScreenOpened)

        getBalanceHidingSettingsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach {
                isBalanceHidden = it.isBalanceHidden
                stateHolder.updateHiddenState(isBalanceHidden)
            }
            .launchIn(viewModelScope)

        bootstrapTokenList()
        bootstrapDragAndDropUpdates()
    }

    override fun onBackClick() {
        router.popBackStack()
    }

    override fun onSortClick() {
        val list = cachedTokenList ?: return
        if (list.sortedBy == TokenList.SortType.BALANCE) return

        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ByBalance)

        viewModelScope.launch(dispatchers.default) {
            toggleTokenListSortingUseCase(list).fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = {
                    stateHolder.updateStateAfterTokenListSorting(it)
                    cachedTokenList = it
                },
            )
        }
    }

    override fun onGroupClick() {
        val list = cachedTokenList ?: return

        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Group)

        viewModelScope.launch(dispatchers.default) {
            toggleTokenListGroupingUseCase(list).fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = {
                    stateHolder.updateStateAfterTokenListSorting(it)
                    cachedTokenList = it
                },
            )
        }
    }

    override fun onApplyClick() {
        viewModelScope.launch(dispatchers.default) {
            stateHolder.updateStateToDisplayProgress()

            val listState = uiState.value.itemsState
            val resolver = CryptoCurrenciesIdsResolver()

            val isGroupedByNetwork = listState is OrganizeTokensListState.GroupedByNetwork
            val isSortedByBalance = uiState.value.header.isSortedByBalance

            sendAnalyticsEvent(
                isGroupedByNetwork = isGroupedByNetwork,
                isSortedByBalance = isSortedByBalance,
            )

            val result = applyTokenListSortingUseCase(
                userWalletId = userWalletId,
                sortedTokensIds = resolver.resolve(listState, cachedTokenList),
                isGroupedByNetwork = isGroupedByNetwork,
                isSortedByBalance = isSortedByBalance,
            )

            result.fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = {
                    stateHolder.updateStateToHideProgress()
                    withContext(dispatchers.main) { router.popBackStack() }
                },
            )
        }
    }

    override fun onCancelClick() {
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Cancel)

        router.popBackStack()
    }

    private fun bootstrapTokenList() {
        viewModelScope.launch(dispatchers.default) {
            val tokenList = getTokenList() ?: return@launch

            stateHolder.updateStateWithTokenList(tokenList)
            cachedTokenList = tokenList
        }
    }

    private suspend fun getTokenList(): TokenList? {
        val tokenList = getTokenListUseCase.launch(userWalletId)
            .transform { maybeTokenList ->
                val tokenList = maybeTokenList.getOrElse(
                    ifLoading = { return@transform },
                    ifError = { error ->
                        stateHolder.updateStateWithError(error)

                        return@transform
                    },
                )

                emit(tokenList)
            }

        return tokenList.firstOrNull()
    }

    private fun bootstrapDragAndDropUpdates() {
        dragAndDropAdapter.dragAndDropUpdates
            .distinctUntilChanged()
            .onEach { (type, updatedListState) ->
                disableSortingByBalanceIfListChanged(type)

                stateHolder.updateStateWithManualSorting(updatedListState)
            }
            .launchIn(viewModelScope)
    }

    private fun disableSortingByBalanceIfListChanged(dragOperationType: DragAndDropAdapter.DragOperation.Type) {
        if (dragOperationType !is DragAndDropAdapter.DragOperation.Type.End) return

        if (uiState.value.header.isSortedByBalance && dragOperationType.isItemsOrderChanged) {
            cachedTokenList = cachedTokenList?.disableSortingByBalance()
            stateHolder.disableSortingByBalance()
        }
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { maybeAppCurrency ->
                maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppCurrency.Default,
            )
    }

    private fun sendAnalyticsEvent(isGroupedByNetwork: Boolean, isSortedByBalance: Boolean) {
        analyticsEventsHandler.send(
            PortfolioOrganizeTokensAnalyticsEvent.Apply(
                grouping = if (isGroupedByNetwork) {
                    AnalyticsParam.OnOffState.On
                } else {
                    AnalyticsParam.OnOffState.Off
                },
                organizeSortType = if (isSortedByBalance) {
                    AnalyticsParam.OrganizeSortType.ByBalance
                } else {
                    AnalyticsParam.OrganizeSortType.Manually
                },
            ),
        )
    }
}