package com.tangem.feature.wallet.child.organizetokens.model

import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensListUM
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensState
import com.tangem.feature.wallet.child.organizetokens.model.converter.InProgressStateConverter
import com.tangem.feature.wallet.child.organizetokens.model.converter.TokenListToStateConverter
import com.tangem.feature.wallet.child.organizetokens.model.converter.error.TokenListSortingErrorConverter
import com.tangem.feature.wallet.child.organizetokens.model.dnd.DragAndDropAdapterLegacy
import com.tangem.utils.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class OrganizeTokensStateHolder(
    private val intents: OrganizeTokensIntents,
    private val dragAndDropAdapterLegacy: DragAndDropAdapterLegacy,
    private val appCurrencyProvider: Provider<AppCurrency>,
) {

    private val stateFlowInternal: MutableStateFlow<OrganizeTokensState> = MutableStateFlow(getInitialState())

    private val inProgressStateConverter by lazy { InProgressStateConverter() }

    private val tokenListSortingErrorConverter by lazy {
        TokenListSortingErrorConverter(Provider(stateFlowInternal::value), inProgressStateConverter)
    }

    val stateFlow: StateFlow<OrganizeTokensState> = stateFlowInternal

    fun updateStateWithAccountList(accountStatusList: AccountStatusList, isAccountsModeEnabled: Boolean) {
        updateState {
            TokenListToStateConverter(
                accountStatusList = accountStatusList,
                isAccountsMode = isAccountsModeEnabled,
                appCurrency = appCurrencyProvider(),
            ).transform(this)
        }
    }

    fun updateStateAfterTokenListSorting(accountStatusList: AccountStatusList, isAccountsModeEnabled: Boolean) {
        updateState {
            TokenListToStateConverter(
                accountStatusList = accountStatusList,
                isAccountsMode = isAccountsModeEnabled,
                appCurrency = appCurrencyProvider(),
            ).transform(this).copy(
                scrollListToTop = triggeredEvent(Unit, ::consumeScrollListToTopEvent),
            )
        }
    }

    fun updateStateToDisplayProgress() {
        updateState { inProgressStateConverter.convert(value = this) }
    }

    fun updateStateToHideProgress() {
        updateState { inProgressStateConverter.convertBack(value = this) }
    }

    fun updateStateWithManualSorting(tokenListUM: OrganizeTokensListUM) {
        updateState { copy(tokenListUM = tokenListUM) }
    }

    fun disableSortingByBalance() {
        updateState { copy(header = header.copy(isSortedByBalance = false)) }
    }

    fun updateHiddenState(isBalanceHidden: Boolean) {
        updateState { copy(isBalanceHidden = isBalanceHidden) }
    }

    fun updateStateWithError(error: TokenListSortingError) {
        updateState { tokenListSortingErrorConverter.convert(error) }
    }

    private fun getInitialState(): OrganizeTokensState {
        return OrganizeTokensState(
            onBackClick = intents::onBackClick,
            tokenListUM = OrganizeTokensListUM.EmptyList,
            header = OrganizeTokensState.HeaderConfig(
                onSortClick = intents::onSortClick,
                onGroupClick = intents::onGroupClick,
            ),
            actions = OrganizeTokensState.ActionsConfig(
                onApplyClick = intents::onApplyClick,
                onCancelClick = intents::onCancelClick,
            ),
            dndConfig = OrganizeTokensState.DragAndDropConfig(
                onItemDragged = dragAndDropAdapterLegacy::onItemDragged,
                onItemDragStart = dragAndDropAdapterLegacy::onItemDraggingStartLegacy,
                onItemDragEnd = dragAndDropAdapterLegacy::onItemDraggingEnd,
                canDragItemOver = dragAndDropAdapterLegacy::canDragItemOver,
            ),
            scrollListToTop = consumedEvent(),
            isBalanceHidden = true,
        )
    }

    private inline fun updateState(block: OrganizeTokensState.() -> OrganizeTokensState) {
        stateFlowInternal.update(block)
    }

    private fun consumeScrollListToTopEvent() {
        updateState { copy(scrollListToTop = consumedEvent()) }
    }
}