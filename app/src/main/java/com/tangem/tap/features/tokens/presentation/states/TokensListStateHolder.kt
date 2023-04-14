package com.tangem.tap.features.tokens.presentation.states

import androidx.paging.LoadState
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * State holder for screen with list of tokens
 *
* [REDACTED_AUTHOR]
 */
internal sealed interface TokensListStateHolder {

    /** Toolbar state */
    val toolbarState: TokensListToolbarState

    /** Loading state */
    val isLoading: Boolean

    /** Tokens list */
    val tokens: Flow<PagingData<TokenItemState>>

    /** Callback to be invoked when [tokens] loading state is been changed */
    val onTokensLoadStateChanged: (LoadState) -> Unit

    /**
     * Util function that allow to make a copy
     *
     * @param toolbarState             toolbar state
     * @param isLoading                loading state
     * @param tokens                   tokens list
     * @param onTokensLoadStateChanged callback to be invoked when tokens loading state is been changed
     */
    fun copySealed(
        toolbarState: TokensListToolbarState = this.toolbarState,
        isLoading: Boolean = this.isLoading,
        tokens: Flow<PagingData<TokenItemState>> = this.tokens,
        onTokensLoadStateChanged: (LoadState) -> Unit = this.onTokensLoadStateChanged,
    ): TokensListStateHolder {
        return when (this) {
            is ManageContent -> copy(toolbarState, isLoading, tokens, onTokensLoadStateChanged)
            is ReadContent -> copy(toolbarState, isLoading, tokens, onTokensLoadStateChanged)
        }
    }

    /**
     * State screen that is available only for read
     *
     * @property toolbarState             toolbar state
     * @property tokens                   tokens list
     * @property onTokensLoadStateChanged callback to be invoked when tokens loading state is been changed
     */
    data class ReadContent(
        override val toolbarState: TokensListToolbarState,
        override val isLoading: Boolean,
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
        override val isLoading: Boolean,
        override val tokens: Flow<PagingData<TokenItemState>>,
        override val onTokensLoadStateChanged: (LoadState) -> Unit,
        val onSaveButtonClick: () -> Unit,
    ) : TokensListStateHolder
}
