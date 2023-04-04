package com.tangem.tap.features.tokens.presentation.states

/** Toolbar state */
sealed interface AddTokensToolbarState {

    /** Callback to be invoked when BackButton is being clicked */
    val onBackButtonClick: () -> Unit

    /** Callback to be invoked when SearchButton is being clicked */
    val onSearchButtonClick: () -> Unit

    /** Toolbar state as title */
    sealed interface Title : AddTokensToolbarState {

        /** Toolbar title id from resources */
        val titleResId: Int

        /**
         * Title state that is available only for read
         *
         * @property titleResId          toolbar title id from resources
         * @property onBackButtonClick   callback to be invoked when BackButton is being clicked
         * @property onSearchButtonClick callback to be invoked when SearchButton is being clicked
         */
        data class ReadAccess(
            override val titleResId: Int,
            override val onBackButtonClick: () -> Unit,
            override val onSearchButtonClick: () -> Unit,
        ) : Title

        /**
         * Title state that is available for read and edit
         *
         * @property titleResId            toolbar title id from resources
         * @property onBackButtonClick     callback to be invoked when BackButton is being clicked
         * @property onSearchButtonClick   callback to be invoked when SearchButton is being clicked
         * @property onAddCustomTokenClick callback to be invoked when AddCustomTokenButton is being clicked
         */
        data class EditAccess(
            override val titleResId: Int,
            override val onBackButtonClick: () -> Unit,
            override val onSearchButtonClick: () -> Unit,
            val onAddCustomTokenClick: () -> Unit,
        ) : Title
    }

    /**
     * Toolbar state as input field
     *
     * @property onBackButtonClick   callback to be invoked when BackButton is being clicked
     * @property onSearchButtonClick callback to be invoked when SearchButton is being clicked
     * @property value               input value
     * @property onValueChange       lambda to be invoked when search value is being changed
     * @property onCleanButtonClick  callback to be invoked when CleanButton is being clicked
     */
    data class SearchInputField(
        override val onBackButtonClick: () -> Unit,
        override val onSearchButtonClick: () -> Unit,
        val value: String,
        val onValueChange: (String) -> Unit,
        val onCleanButtonClick: () -> Unit,
    ) : AddTokensToolbarState
}