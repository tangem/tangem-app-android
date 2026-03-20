package com.tangem.feature.wallet.child.organizetokens.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.ApplyTokenListSortingUseCase
import com.tangem.domain.account.status.usecase.ToggleTokenListGroupingUseCase
import com.tangem.domain.account.status.usecase.ToggleTokenListSortingUseCase
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.TokensSortType
import com.tangem.feature.wallet.child.organizetokens.OrganizeTokensComponent
import com.tangem.feature.wallet.child.organizetokens.analytics.PortfolioOrganizeTokensAnalyticsEvent
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.feature.wallet.child.organizetokens.model.dnd.DragAndDropAdapter
import com.tangem.feature.wallet.child.organizetokens.model.transformer.OrganizeContentStateTransformer
import com.tangem.feature.wallet.child.organizetokens.model.transformer.OrganizeDisableBalanceSortingTransformer
import com.tangem.feature.wallet.child.organizetokens.model.transformer.OrganizeSortingProgressStateTransformer
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
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
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val toggleTokenListGroupingUseCase: ToggleTokenListGroupingUseCase,
    private val toggleTokenListSortingUseCase: ToggleTokenListSortingUseCase,
    private val applyTokenListSortingUseCase: ApplyTokenListSortingUseCase,
) : Model(), OrganizeTokensIntents {

    private val params: OrganizeTokensComponent.Params = paramsContainer.require()

    private val selectedAppCurrencyFlow = createSelectedAppCurrencyFlow()

    private val userWalletId = params.userWalletId

    private var cachedAccountStatusList: AccountStatusList? = null

    private var isAccountsModeEnabled: Boolean = false

    val uiState: StateFlow<OrganizeTokensUM>
        field = MutableStateFlow(getInitialState())

    val dragAndDropAdapter by lazy(LazyThreadSafetyMode.NONE) {
        DragAndDropAdapter(uiState)
    }

    init {
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ScreenOpened())

        getBalanceHidingSettingsUseCase()
            .onEach { balanceSettings ->
                uiState.update {
                    it.copy(
                        isBalanceHidden = balanceSettings.isBalanceHidden,
                    )
                }
            }
            .launchIn(modelScope)

        bootstrapTokenList()
        bootstrapDragAndDropUpdates()
    }

    override fun onBackClick() {
        params.callback.onDismiss()
    }

    override fun onSortClick() {
        val list = cachedAccountStatusList ?: return
        if (list.sortType == TokensSortType.BALANCE) return

        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.ByBalance())

        modelScope.launch {
            toggleTokenListSortingUseCase(list).fold(
                ifLeft = {
                    uiState.update {
                        it.copy(
                            organizeMenuUM = it.organizeMenuUM.copy(isEnabled = false),
                        )
                    }
                },
                ifRight = { accountStatusList ->
                    uiState.update(
                        OrganizeContentStateTransformer(
                            accountStatusList = accountStatusList,
                            isAccountsMode = isAccountsModeEnabled,
                            appCurrency = selectedAppCurrencyFlow.value,
                        ),
                    )
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
                ifLeft = {
                    uiState.update {
                        it.copy(
                            organizeMenuUM = it.organizeMenuUM.copy(isEnabled = false),
                        )
                    }
                },
                ifRight = { accountStatusList ->
                    uiState.update(
                        OrganizeContentStateTransformer(
                            accountStatusList = accountStatusList,
                            isAccountsMode = isAccountsModeEnabled,
                            appCurrency = selectedAppCurrencyFlow.value,
                        ),
                    )
                    cachedAccountStatusList = accountStatusList
                },
            )
        }
    }

    override fun onApplyClick() {
        modelScope.launch {
            uiState.update(OrganizeSortingProgressStateTransformer(true))
            val resolver = CryptoCurrenciesIdsResolver()
            val isSortedByBalance = uiState.value.organizeMenuUM.isSortedByBalance
            val isGroupedByNetwork = uiState.value.isGrouped
            val tokensListUM = uiState.value.tokenList

            sendAnalyticsEvent(
                isGroupedByNetwork = isGroupedByNetwork,
                isSortedByBalance = isSortedByBalance,
            )

            val result = applyTokenListSortingUseCase(
                sortedTokensIdsByAccount = resolver.resolve(tokensListUM, cachedAccountStatusList),
                isGroupedByNetwork = isGroupedByNetwork,
                isSortedByBalance = isSortedByBalance,
            )

            result.fold(
                ifLeft = {
                    uiState.update {
                        it.copy(
                            organizeMenuUM = it.organizeMenuUM.copy(isEnabled = false),
                        )
                    }
                },
                ifRight = {
                    onBackClick()
                    uiState.update(OrganizeSortingProgressStateTransformer(false))
                },
            )
        }
    }

    override fun onCancelClick() {
        analyticsEventsHandler.send(PortfolioOrganizeTokensAnalyticsEvent.Cancel())
        onBackClick()
    }

    private fun bootstrapTokenList() {
        modelScope.launch {
            val accountList = singleAccountStatusListSupplier.getSyncOrNull(
                SingleAccountStatusListProducer.Params(userWalletId),
            ) ?: return@launch

            isAccountsModeEnabled = isAccountsModeEnabledUseCase.invokeSync()

            uiState.update(
                transformer = OrganizeContentStateTransformer(
                    accountStatusList = accountList,
                    isAccountsMode = isAccountsModeEnabled,
                    appCurrency = selectedAppCurrencyFlow.value,
                ),
            )

            cachedAccountStatusList = accountList
        }
    }

    private fun bootstrapDragAndDropUpdates() {
        dragAndDropAdapter.dragAndDropUpdates
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { (type, updatedTokenList) ->
                disableSortingByBalanceIfListChanged(type)
                uiState.update { it.copy(tokenList = updatedTokenList) }
            }
            .launchIn(modelScope)
    }

    private fun disableSortingByBalanceIfListChanged(dragOperationType: DragAndDropAdapter.DragOperation.Type) {
        if (dragOperationType !is DragAndDropAdapter.DragOperation.Type.End) return

        if (uiState.value.organizeMenuUM.isSortedByBalance && dragOperationType.isItemsOrderChanged) {
            cachedAccountStatusList = cachedAccountStatusList?.copy(sortType = TokensSortType.NONE)
            uiState.update(OrganizeDisableBalanceSortingTransformer)
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

    private fun getInitialState(): OrganizeTokensUM {
        return OrganizeTokensUM(
            tokenList = persistentListOf(),
            organizeMenuUM = OrganizeTokensUM.OrganizeMenuUM(
                onSortClick = ::onSortClick,
                onGroupClick = ::onGroupClick,
            ),
            cancelButton = TangemButtonUM(
                text = resourceReference(R.string.common_cancel),
                onClick = ::onCancelClick,
                type = TangemButtonType.Secondary,
            ),
            applyButton = TangemButtonUM(
                text = resourceReference(R.string.common_apply),
                onClick = ::onApplyClick,
                type = TangemButtonType.Primary,
            ),
            scrollListToTop = consumedEvent(),
            isBalanceHidden = true,
            isGrouped = false,
            isAccountsMode = false,
        )
    }
}