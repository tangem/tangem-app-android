package com.tangem.tap.features.tokens.presentation.states

import androidx.paging.LoadState
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * State holder for screen with list of tokens
 *
[REDACTED_AUTHOR]
 */
internal sealed interface TokensListStateHolder {

    /** Toolbar state */
    val toolbarState: TokensListToolbarState

    /** Tokens list */
    val tokens: Flow<PagingData<TokenItemState>>

    /** Callback to be invoked when [tokens] loading state is been changed */
    val onTokensLoadStateChanged: (LoadState) -> Unit

    /**
     * Util function that allow to make a copy
     *
     * @param toolbarState             toolbar state
     * @param tokens                   tokens list
     * @param onTokensLoadStateChanged callback to be invoked when tokens loading state is been changed
     */
    fun copySealed(
        toolbarState: TokensListToolbarState = this.toolbarState,
        tokens: Flow<PagingData<TokenItemState>> = this.tokens,
        onTokensLoadStateChanged: (LoadState) -> Unit = this.onTokensLoadStateChanged,
    ): TokensListStateHolder {
        return when (this) {
            is ManageContent -> copy(toolbarState, tokens, onTokensLoadStateChanged)
            is Loading -> copy(toolbarState, tokens, onTokensLoadStateChanged)
            is ReadContent -> copy(toolbarState, tokens, onTokensLoadStateChanged)
        }
    }

    /**
     * Loading state
     *
     * @property toolbarState             toolbar state
     * @property tokens                   tokens list
     * @property onTokensLoadStateChanged callback to be invoked when tokens loading state is been changed
     */
    data class Loading(
        override val toolbarState: TokensListToolbarState,
        override val tokens: Flow<PagingData<TokenItemState>>,
        override val onTokensLoadStateChanged: (LoadState) -> Unit,
    ) : TokensListStateHolder

    /**
     * State screen that is available only for read
     *
     * @property toolbarState             toolbar state
     * @property tokens                   tokens list
     * @property onTokensLoadStateChanged callback to be invoked when tokens loading state is been changed
     */
    data class ReadContent(
        override val toolbarState: TokensListToolbarState,
        override val tokens: Flow<PagingData<TokenItemState>>,
        override val onTokensLoadStateChanged: (LoadState) -> Unit,
    ) : TokensListStateHolder

    /**
     * State screen that is available for read and manage
     *
     * @property toolbarState             toolbar state
     * @property tokens                   tokens list
     * @property onTokensLoadStateChanged callback to be invoked when tokens loading state is been changed
     * @property onSaveButtonClick        callback to be invoked when SaveButton is being clicked
     */
    data class ManageContent(
        override val toolbarState: TokensListToolbarState,
        override val tokens: Flow<PagingData<TokenItemState>>,
        override val onTokensLoadStateChanged: (LoadState) -> Unit,
        val onSaveButtonClick: () -> Unit,
    ) : TokensListStateHolder
}