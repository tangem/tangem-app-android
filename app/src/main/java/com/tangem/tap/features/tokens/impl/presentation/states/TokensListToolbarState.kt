package com.tangem.tap.features.tokens.impl.presentation.states

import androidx.compose.runtime.Immutable

/**
 * Toolbar state.
 * All subclasses is stable, but @Immutable annotation is required to use this sealed class like as
 * field of TokensListStateHolder.
 */
@Immutable
sealed interface TokensListToolbarState {

    /** Callback to be invoked when BackButton is being clicked */
    val onBackButtonClick: () -> Unit

    /** Toolbar state as title */
    sealed interface Title : TokensListToolbarState {

        /** Toolbar title id from resources */
        val titleResId: Int

        /** Callback to be invoked when SearchButton is being clicked */
        val onSearchButtonClick: () -> Unit

        /**
         * Title state that is available only for read
         *
         * @property onBackButtonClick   callback to be invoked when BackButton is being clicked
         * @property titleResId          toolbar title id from resources
         * @property onSearchButtonClick callback to be invoked when SearchButton is being clicked
         */
        data class Read(
            override val onBackButtonClick: () -> Unit,
            override val titleResId: Int,
            override val onSearchButtonClick: () -> Unit,
        ) : Title

        /**
         * Title state that is available for read and manage
         *
         * @property onBackButtonClick     callback to be invoked when BackButton is being clicked
         * @property titleResId            toolbar title id from resources
         * @property onSearchButtonClick   callback to be invoked when SearchButton is being clicked
         * @property onAddCustomTokenClick callback to be invoked when AddCustomTokenButton is being clicked
         */
        data class Manage(
            override val onBackButtonClick: () -> Unit,
            override val titleResId: Int,
            override val onSearchButtonClick: () -> Unit,
            val onAddCustomTokenClick: () -> Unit,
        ) : Title
    }

    /**
     * Toolbar state as input field
     *
     * @property onBackButtonClick  callback to be invoked when BackButton is being clicked
     * @property value              input value
     * @property onValueChange      lambda to be invoked when search value is being changed
     * @property onCleanButtonClick callback to be invoked when CleanButton is being clicked
     */
    data class InputField(
        override val onBackButtonClick: () -> Unit,
        val value: String,
        val onValueChange: (String) -> Unit,
        val onCleanButtonClick: () -> Unit,
    ) : TokensListToolbarState
}