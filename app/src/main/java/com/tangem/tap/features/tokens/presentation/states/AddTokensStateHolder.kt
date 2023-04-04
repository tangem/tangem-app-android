package com.tangem.tap.features.tokens.presentation.states

/**
 * State holder for adding token screen
 *
[REDACTED_AUTHOR]
 */
internal sealed interface AddTokensStateHolder {

    /** Toolbar state */
    val toolbarState: AddTokensToolbarState

    /**
     * Util function that allow to make a copy
     *
     * @param toolbarState toolbar state
     */
    fun copySealed(toolbarState: AddTokensToolbarState): AddTokensStateHolder {
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
    data class Loading(override val toolbarState: AddTokensToolbarState) : AddTokensStateHolder

    /**
     * State screen that is available only for read
     *
     * @property toolbarState toolbar state
     * @property tokens       tokens list
     */
    data class ReadAccess(
        override val toolbarState: AddTokensToolbarState,
        override val tokens: List<TokenItemModel>,
    ) : AddTokensStateHolder, TokensListVisibility

    /**
     * State screen that is available for read and edit
     *
     * @property toolbarState      toolbar state
     * @property tokens            tokens list
     * @property onSaveButtonClick callback to be invoked when SaveButton is being clicked
     */
    data class ManageAccess(
        override val toolbarState: AddTokensToolbarState,
        override val tokens: List<TokenItemModel>,
        val onSaveButtonClick: () -> Unit,
    ) : AddTokensStateHolder, TokensListVisibility
}