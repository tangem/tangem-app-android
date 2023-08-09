package com.tangem.feature.wallet.presentation.organizetokens

import com.tangem.common.Provider
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.updateSorting
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.InProgressStateConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.TokenListToStateConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.error.TokenListErrorConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.error.TokenListSortingErrorConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items.CryptoCurrencyToDraggableItemConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items.NetworkGroupToDraggableItemsConverter
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.items.TokenListToListStateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

@Suppress("unused", "MemberVisibilityCanBePrivate") // TODO: Will be used in next MR
internal class OrganizeTokensStateHolder(
    private val intents: OrganizeTokensIntents,
    private val fiatCurrencyCode: String,
    private val fiatCurrencySymbol: String,
    private val onSubscription: () -> Unit,
    scope: CoroutineScope,
) {

    private val stateFlowInternal: MutableStateFlow<OrganizeTokensState> = MutableStateFlow(getInitialState())

    private val tokenListConverter by lazy {
        val tokensConverter = CryptoCurrencyToDraggableItemConverter(
            fiatCurrencyCode = fiatCurrencyCode,
            fiatCurrencySymbol = fiatCurrencySymbol,
        )
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
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = getInitialState(),
        )

    var tokenList: TokenList? = null
        private set

    fun updateStateWithTokenList(tokenList: TokenList) {
        updateState { tokenListConverter.convert(tokenList) }
        this.tokenList = tokenList
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
        tokenList = tokenList?.updateSorting(isSortedByBalance = false)
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
                onItemDragged = intents::onItemDragged,
                onDragStart = intents::onItemDraggingStart,
                onItemDragEnd = intents::onItemDraggingEnd,
                canDragItemOver = intents::canDragItemOver,
            ),
        )
    }

    private fun updateState(block: OrganizeTokensState.() -> OrganizeTokensState) {
        stateFlowInternal.update(block)
    }
}