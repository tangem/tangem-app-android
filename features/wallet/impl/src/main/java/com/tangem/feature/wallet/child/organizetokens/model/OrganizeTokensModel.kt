package com.tangem.feature.wallet.child.organizetokens.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.ApplyTokenListSortingUseCaseV2
import com.tangem.domain.account.status.usecase.ToggleTokenListGroupingUseCaseV2
import com.tangem.domain.account.status.usecase.ToggleTokenListSortingUseCaseV2
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.tokens.ApplyTokenListSortingUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.ToggleTokenListGroupingUseCase
import com.tangem.domain.tokens.ToggleTokenListSortingUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.child.organizetokens.OrganizeTokensComponent
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensIntents
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensStateHolder
import com.tangem.feature.wallet.presentation.organizetokens.analytics.PortfolioOrganizeTokensAnalyticsEvent
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.CryptoCurrenciesIdsResolver
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.disableSortingByBalance
import com.tangem.feature.wallet.presentation.organizetokens.utils.dnd.DragAndDropAdapter
import com.tangem.feature.wallet.presentation.organizetokens.utils.dnd.DragAndDropAdapterV2
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
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val toggleTokenListGroupingUseCaseV2: ToggleTokenListGroupingUseCaseV2,
    private val toggleTokenListSortingUseCaseV2: ToggleTokenListSortingUseCaseV2,
    private val applyTokenListSortingUseCaseV2: ApplyTokenListSortingUseCaseV2,
) : Model(), OrganizeTokensIntents {

    private val selectedAppCurrencyFlow = createSelectedAppCurrencyFlow()

    private var isBalanceHidden = true

    private val dragAndDropAdapter = DragAndDropAdapter(
        listStateProvider = Provider { uiState.value.itemsState },
    )

    private val dragAndDropAdapterV2 = DragAndDropAdapterV2(
        tokenListUMProvider = Provider { uiState.value.tokenListUM },
    )

    private val stateHolder = OrganizeTokensStateHolder(
        intents = this,
        dragAndDropIntents = dragAndDropAdapter,
        dragAndDropAdapterV2 = dragAndDropAdapterV2,
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        accountsFeatureToggles = accountsFeatureToggles,
    )

    private val userWalletId = paramsContainer.require<OrganizeTokensComponent.Params>().userWalletId

    private var cachedTokenList: TokenList? = null
    private var cachedAccountStatusList: AccountStatusList? = null

    private var isAccountsModeEnabled: Boolean = false

    val uiState: StateFlow<OrganizeTokensState> = stateHolder.stateFlow

    val onBack = MutableSharedFlow<Unit>()

    init {
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ScreenOpened())

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
        if (accountsFeatureToggles.isFeatureEnabled) {
            val list = cachedAccountStatusList ?: return
            if (list.sortType == TokensSortType.BALANCE) return

            analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ByBalance())

            modelScope.launch {
                toggleTokenListSortingUseCaseV2(list).fold(
                    ifLeft = stateHolder::updateStateWithError,
                    ifRight = {
                        stateHolder.updateStateAfterTokenListSortingV2(it, isAccountsModeEnabled)
                        cachedAccountStatusList = it
                    },
                )
            }
        } else {
            val list = cachedTokenList ?: return
            if (list.sortedBy == TokensSortType.BALANCE) return

            analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ByBalance())

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
    }

    override fun onGroupClick() {
        if (accountsFeatureToggles.isFeatureEnabled) {
            val list = cachedAccountStatusList ?: return

            analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Group())

            modelScope.launch {
                toggleTokenListGroupingUseCaseV2(list).fold(
                    ifLeft = stateHolder::updateStateWithError,
                    ifRight = {
                        stateHolder.updateStateAfterTokenListSortingV2(it, isAccountsModeEnabled)
                        cachedAccountStatusList = it
                    },
                )
            }
        } else {
            val list = cachedTokenList ?: return

            analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Group())

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
    }

    override fun onApplyClick() {
        modelScope.launch {
            stateHolder.updateStateToDisplayProgress()
            val resolver = CryptoCurrenciesIdsResolver()
            val isSortedByBalance = uiState.value.header.isSortedByBalance

            val result = if (accountsFeatureToggles.isFeatureEnabled) {
                val tokensListUM = uiState.value.tokenListUM

                val isGroupedByNetwork = tokensListUM.isGrouped

                sendAnalyticsEvent(
                    isGroupedByNetwork = isGroupedByNetwork,
                    isSortedByBalance = isSortedByBalance,
                )

                applyTokenListSortingUseCaseV2(
                    sortedTokensIdsByAccount = resolver.resolveV2(tokensListUM, cachedAccountStatusList),
                    isGroupedByNetwork = isGroupedByNetwork,
                    isSortedByBalance = isSortedByBalance,
                )
            } else {
                val listState = uiState.value.itemsState

                val isGroupedByNetwork = listState is OrganizeTokensListState.GroupedByNetwork

                sendAnalyticsEvent(
                    isGroupedByNetwork = isGroupedByNetwork,
                    isSortedByBalance = isSortedByBalance,
                )

                applyTokenListSortingUseCase(
                    userWalletId = userWalletId,
                    sortedTokensIds = resolver.resolve(listState, cachedTokenList),
                    isGroupedByNetwork = isGroupedByNetwork,
                    isSortedByBalance = isSortedByBalance,
                )
            }

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
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Cancel())

        modelScope.launch { onBack.emit(Unit) }
    }

    private fun bootstrapTokenList() {
        modelScope.launch {
            if (accountsFeatureToggles.isFeatureEnabled) {
                val accountList = singleAccountStatusListSupplier.getSyncOrNull(
                    SingleAccountStatusListProducer.Params(userWalletId),
                ) ?: return@launch

                isAccountsModeEnabled = isAccountsModeEnabledUseCase.invokeSync()

                stateHolder.updateStateWithAccountList(
                    accountStatusList = accountList,
                    isAccountsModeEnabled = isAccountsModeEnabled,
                )

                cachedAccountStatusList = accountList
            } else {
                val tokenList = getTokenList() ?: return@launch
                stateHolder.updateStateWithTokenList(tokenList)
                cachedTokenList = tokenList
            }
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
        if (accountsFeatureToggles.isFeatureEnabled) {
            dragAndDropAdapterV2.dragAndDropUpdates
                .distinctUntilChanged()
                .onEach { (type, updatedListState) ->
                    disableSortingByBalanceIfListChangedV2(type)

                    stateHolder.updateStateWithManualSortingV2(updatedListState)
                }
                .launchIn(modelScope)
        } else {
            dragAndDropAdapter.dragAndDropUpdates
                .distinctUntilChanged()
                .onEach { (type, updatedListState) ->
                    disableSortingByBalanceIfListChanged(type)

                    stateHolder.updateStateWithManualSorting(updatedListState)
                }
                .launchIn(modelScope)
        }
    }

    private fun disableSortingByBalanceIfListChanged(dragOperationType: DragAndDropAdapter.DragOperation.Type) {
        if (dragOperationType !is DragAndDropAdapter.DragOperation.Type.End) return

        if (uiState.value.header.isSortedByBalance && dragOperationType.isItemsOrderChanged) {
            cachedTokenList = cachedTokenList?.disableSortingByBalance()
            stateHolder.disableSortingByBalance()
        }
    }

    private fun disableSortingByBalanceIfListChangedV2(dragOperationType: DragAndDropAdapterV2.DragOperation.Type) {
        if (dragOperationType !is DragAndDropAdapterV2.DragOperation.Type.End) return

        if (uiState.value.header.isSortedByBalance && dragOperationType.isItemsOrderChanged) {
            cachedAccountStatusList = cachedAccountStatusList?.copy(sortType = TokensSortType.NONE)
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