package com.tangem.tap.features.tokens.presentation.states

import kotlinx.collections.immutable.ImmutableList

/**
 * State holder for screen with list of tokens
 *
[REDACTED_AUTHOR]
 */
internal sealed interface TokensListStateHolder {

    /** Toolbar state */
    val toolbarState: TokensListToolbarState

    /**
     * Util function that allow to make a copy
     *
     * @param toolbarState toolbar state
     */
    fun copySealed(toolbarState: TokensListToolbarState): TokensListStateHolder {
        return when (this) {
            is ManageAccess -> copy(toolbarState = toolbarState)
            is Loading -> copy(toolbarState = toolbarState)
            is ReadAccess -> copy(toolbarState = toolbarState)
        }
    }

    /**
     * Loading state
     *
     * @property toolbarState toolbar state
     */
    data class Loading(override val toolbarState: TokensListToolbarState) : TokensListStateHolder

    /**
     * State screen that is available only for read
     *
     * @property toolbarState toolbar state
     * @property tokens       tokens list
     */
    data class ReadAccess(
        override val toolbarState: TokensListToolbarState,
        override val tokens: ImmutableList<TokenItemState.ReadAccess>,
    ) : TokensListStateHolder, TokensListVisibility

    /**
     * State screen that is available for read and edit
     *
     * @property toolbarState      toolbar state
     * @property tokens            tokens list
     * @property onSaveButtonClick callback to be invoked when SaveButton is being clicked
     */
    data class ManageAccess(
        override val toolbarState: TokensListToolbarState,
        override val tokens: ImmutableList<TokenItemState.ManageAccess>,
        val onSaveButtonClick: () -> Unit,
    ) : TokensListStateHolder, TokensListVisibility
}