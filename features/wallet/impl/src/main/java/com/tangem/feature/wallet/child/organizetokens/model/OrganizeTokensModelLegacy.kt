package com.tangem.feature.wallet.child.organizetokens.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.ApplyTokenListSortingUseCase
import com.tangem.domain.account.status.usecase.ToggleTokenListGroupingUseCase
import com.tangem.domain.account.status.usecase.ToggleTokenListSortingUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.TokensSortType
import com.tangem.feature.wallet.child.organizetokens.OrganizeTokensComponentLegacy
import com.tangem.feature.wallet.child.organizetokens.analytics.PortfolioOrganizeTokensAnalyticsEvent
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensState
import com.tangem.feature.wallet.child.organizetokens.model.dnd.DragAndDropAdapterLegacy
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class OrganizeTokensModelLegacy @Inject constructor(
    paramsContainer: ParamsContainer,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val toggleTokenListGroupingUseCase: ToggleTokenListGroupingUseCase,
    private val toggleTokenListSortingUseCase: ToggleTokenListSortingUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
) : Model(), OrganizeTokensIntents {

    private val selectedAppCurrencyFlow = createSelectedAppCurrencyFlow()

    private var isBalanceHidden = true

    @Suppress("PropertyUsedBeforeDeclaration")
    private val dragAndDropAdapterLegacy = DragAndDropAdapterLegacy(
        tokenListUMProvider = Provider { uiState.value.tokenListUM },
    )

    private val stateHolder = OrganizeTokensStateHolder(
        intents = this,
        dragAndDropAdapterLegacy = dragAndDropAdapterLegacy,
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
    )

    private val userWalletId = paramsContainer.require<OrganizeTokensComponentLegacy.Params>().userWalletId

    private var cachedAccountStatusList: AccountStatusList? = null

    private var isAccountsModeEnabled: Boolean = false

    val uiState: StateFlow<OrganizeTokensState> = stateHolder.stateFlow

    val onBack = MutableSharedFlow<Unit>()

    init {
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ScreenOpened())

        getBalanceHidingSettingsUseCase()
            .onEach { balanceHidingSettings ->
                isBalanceHidden = balanceHidingSettings.isBalanceHidden
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
        val list = cachedAccountStatusList ?: return
        if (list.sortType == TokensSortType.BALANCE) return

        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ByBalance())

        modelScope.launch {
            toggleTokenListSortingUseCase(list).fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = { accountStatusList ->
                    stateHolder.updateStateAfterTokenListSorting(accountStatusList, isAccountsModeEnabled)
                    cachedAccountStatusList = accountStatusList
                },
            )
        }
    }

    override fun onGroupClick() {
        val list = cachedAccountStatusList ?: return

        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Group())

        modelScope.launch {
            toggleTokenListGroupingUseCase(list).fold(
                ifLeft = stateHolder::updateStateWithError,
                ifRight = { accountStatusList ->
                    stateHolder.updateStateAfterTokenListSorting(accountStatusList, isAccountsModeEnabled)
                    cachedAccountStatusList = accountStatusList
                },
            )
        }
    }

    override fun onApplyClick() {
        modelScope.launch {
            stateHolder.updateStateToDisplayProgress()
            val resolver = CryptoCurrenciesIdsResolver()
            val isSortedByBalance = uiState.value.header.isSortedByBalance
            val tokensListUM = uiState.value.tokenListUM

            val isGroupedByNetwork = tokensListUM.isGrouped

            sendAnalyticsEvent(
                isGroupedByNetwork = isGroupedByNetwork,
                isSortedByBalance = isSortedByBalance,
            )

            val result = applyTokenListSortingUseCase(
                sortedTokensIdsByAccount = resolver.resolveLegacy(tokensListUM, cachedAccountStatusList),
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
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Cancel())

        modelScope.launch { onBack.emit(Unit) }
    }

    private fun bootstrapTokenList() {
        modelScope.launch {
            val accountList = singleAccountStatusListSupplier.getSyncOrNull(
                SingleAccountStatusListProducer.Params(userWalletId),
            ) ?: return@launch

            isAccountsModeEnabled = isAccountsModeEnabledUseCase.invokeSync()

            stateHolder.updateStateWithAccountList(
                accountStatusList = accountList,
                isAccountsModeEnabled = isAccountsModeEnabled,
            )

            cachedAccountStatusList = accountList
        }
    }

    private fun bootstrapDragAndDropUpdates() {
        dragAndDropAdapterLegacy.dragAndDropUpdates
            .distinctUntilChanged()
            .onEach { (type, updatedListState) ->
                disableSortingByBalanceIfListChanged(type)

                stateHolder.updateStateWithManualSorting(updatedListState)
            }
            .launchIn(modelScope)
    }

    private fun disableSortingByBalanceIfListChanged(dragOperationType: DragAndDropAdapterLegacy.DragOperation.Type) {
        if (dragOperationType !is DragAndDropAdapterLegacy.DragOperation.Type.End) return

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