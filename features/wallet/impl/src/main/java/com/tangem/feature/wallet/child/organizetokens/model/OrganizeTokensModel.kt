package com.tangem.feature.wallet.child.organizetokens.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.ToggleTokenListGroupingUseCase
import com.tangem.domain.tokens.ToggleTokenListSortingUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.child.organizetokens.OrganizeTokensComponent
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensIntents
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensStateHolder
import com.tangem.feature.wallet.presentation.organizetokens.analytics.PortfolioOrganizeTokensAnalyticsEvent
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.CryptoCurrenciesIdsResolver
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.disableSortingByBalance
import com.tangem.feature.wallet.presentation.organizetokens.utils.dnd.DragAndDropAdapter
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class OrganizeTokensModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val toggleTokenListGroupingUseCase: ToggleTokenListGroupingUseCase,
    private val toggleTokenListSortingUseCase: ToggleTokenListSortingUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val analyticsEventsHandler: AnalyticsEventHandler,
) : Model(), OrganizeTokensIntents {

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

    private val userWalletId = paramsContainer.require<OrganizeTokensComponent.Params>().userWalletId

    private var cachedTokenList: TokenList? = null

    val uiState: StateFlow<OrganizeTokensState> = stateHolder.stateFlow

    val onBack = MutableSharedFlow<Unit>()

    init {
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ScreenOpened)

        getBalanceHidingSettingsUseCase()
            .onEach {
                isBalanceHidden = it.isBalanceHidden
                stateHolder.updateHiddenState(isBalanceHidden)
            }
            .launchIn(modelScope)

        bootstrapTokenList()
        bootstrapDragAndDropUpdates()
    }

    override fun onBackClick() {
        modelScope.launch { onBack.emit(Unit) }
    }

    override fun onSortClick() {
        val list = cachedTokenList ?: return
        if (list.sortedBy == TokensSortType.BALANCE) return

        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ByBalance)

        modelScope.launch {
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

        modelScope.launch {
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
        modelScope.launch {
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
                    modelScope.launch { onBack.emit(Unit) }
                    stateHolder.updateStateToHideProgress()
                },
            )
        }
    }

    override fun onCancelClick() {
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Cancel)

        modelScope.launch { onBack.emit(Unit) }
    }

    private fun bootstrapTokenList() {
        modelScope.launch {
            val tokenList = getTokenList() ?: return@launch

            stateHolder.updateStateWithTokenList(tokenList)
            cachedTokenList = tokenList
        }
    }

    private suspend fun getTokenList(): TokenList? {
        val maybeTokenList = getTokenListUseCase.launch(userWalletId)
            .filterNot(Lce<TokenListError, TokenList>::isLoading)
            .firstOrNull()
            ?: return null

        return maybeTokenList
            .onError(stateHolder::updateStateWithError)
            .getOrNull(isPartialContentAccepted = false)
    }

    private fun bootstrapDragAndDropUpdates() {
        dragAndDropAdapter.dragAndDropUpdates
            .distinctUntilChanged()
            .onEach { (type, updatedListState) ->
                disableSortingByBalanceIfListChanged(type)

                stateHolder.updateStateWithManualSorting(updatedListState)
            }
            .launchIn(modelScope)
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
                scope = modelScope,
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