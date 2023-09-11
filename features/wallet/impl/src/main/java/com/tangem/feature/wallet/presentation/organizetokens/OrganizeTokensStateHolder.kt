package com.tangem.feature.wallet.presentation.organizetokens

import com.tangem.common.Provider
import com.tangem.core.ui.event.consumed
import com.tangem.core.ui.event.triggered
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.InProgressStateConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.TokenListToStateConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.error.TokenListErrorConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.error.TokenListSortingErrorConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items.CryptoCurrencyToDraggableItemConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items.NetworkGroupToDraggableItemsConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items.TokenListToListStateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class OrganizeTokensStateHolder(
    private val intents: OrganizeTokensIntents,
    private val dragAndDropIntents: DragAndDropIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val onSubscription: () -> Unit,
    stateFlowScope: CoroutineScope,
) {

    private val stateFlowInternal: MutableStateFlow<OrganizeTokensState> = MutableStateFlow(getInitialState())

    private val tokenListConverter by lazy {
        val tokensConverter = CryptoCurrencyToDraggableItemConverter(appCurrencyProvider)
        val itemsConverter = TokenListToListStateConverter(
            tokensConverter = tokensConverter,
            groupsConverter = NetworkGroupToDraggableItemsConverter(tokensConverter),
        )

        TokenListToStateConverter(Provider(stateFlowInternal::value), itemsConverter)
    }

    private val inProgressStateConverter by lazy {
        InProgressStateConverter()
    }

    private val tokenListErrorConverter by lazy {
        TokenListErrorConverter(Provider(stateFlowInternal::value), inProgressStateConverter)
    }

    private val tokenListSortingErrorConverter by lazy {
        TokenListSortingErrorConverter(Provider(stateFlowInternal::value), inProgressStateConverter)
    }

    val stateFlow: StateFlow<OrganizeTokensState> = stateFlowInternal
        .onSubscription { onSubscription() }
        .stateIn(
            scope = stateFlowScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = getInitialState(),
        )

    fun updateStateWithTokenList(tokenList: TokenList) {
        updateState { tokenListConverter.convert(tokenList) }
    }

    fun updateStateAfterTokenListSorting(tokenList: TokenList) {
        updateState {
            tokenListConverter.convert(tokenList).copy(
                scrollListToTop = triggered(::consumeScrollListToTopEvent),
            )
        }
    }

    fun updateStateToDisplayProgress() {
        updateState { inProgressStateConverter.convert(value = this) }
    }

    fun updateStateToHideProgress() {
        updateState { inProgressStateConverter.convertBack(value = this) }
    }

    fun updateStateWithManualSorting(itemsState: OrganizeTokensListState) {
        updateState {
            copy(
                header = header.copy(isSortedByBalance = false),
                itemsState = itemsState,
            )
        }
    }

    fun updateStateWithError(error: TokenListError) {
        updateState { tokenListErrorConverter.convert(error) }
    }

    fun updateStateWithError(error: TokenListSortingError) {
        updateState { tokenListSortingErrorConverter.convert(error) }
    }

    private fun getInitialState(): OrganizeTokensState {
        return OrganizeTokensState(
            onBackClick = intents::onBackClick,
            itemsState = OrganizeTokensListState.Empty,
            header = OrganizeTokensState.HeaderConfig(
                onSortClick = intents::onSortClick,
                onGroupClick = intents::onGroupClick,
            ),
            actions = OrganizeTokensState.ActionsConfig(
                onApplyClick = intents::onApplyClick,
                onCancelClick = intents::onCancelClick,
            ),
            dndConfig = OrganizeTokensState.DragAndDropConfig(
                onItemDragged = dragAndDropIntents::onItemDragged,
                onItemDragStart = dragAndDropIntents::onItemDraggingStart,
                onItemDragEnd = dragAndDropIntents::onItemDraggingEnd,
                canDragItemOver = dragAndDropIntents::canDragItemOver,
            ),
            scrollListToTop = consumed,
        )
    }

    private fun updateState(block: OrganizeTokensState.() -> OrganizeTokensState) {
        stateFlowInternal.update(block)
    }

    private fun consumeScrollListToTopEvent() {
        updateState { copy(scrollListToTop = consumed) }
    }
}
