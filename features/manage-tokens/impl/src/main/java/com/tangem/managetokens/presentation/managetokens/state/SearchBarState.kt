package com.tangem.managetokens.presentation.managetokens.state

/**
 * SearchBar state.
 */
internal data class SearchBarState(
    val query: String,
    val onQueryChange: (String) -> Unit,
    val active: Boolean,
    val onActiveChange: (Boolean) -> Unit,
)
