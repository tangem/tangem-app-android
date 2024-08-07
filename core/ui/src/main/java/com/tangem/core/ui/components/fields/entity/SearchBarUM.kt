package com.tangem.core.ui.components.fields.entity

import com.tangem.core.ui.extensions.TextReference

data class SearchBarUM(
    val placeholderText: TextReference,
    val query: String,
    val onQueryChange: (String) -> Unit,
    val isActive: Boolean,
    val onActiveChange: (Boolean) -> Unit,
)
