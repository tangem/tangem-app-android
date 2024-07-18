package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ManageTokensUM(
    val popBack: () -> Unit,
    val items: ImmutableList<CurrencyItemUM>,
    val search: SearchBarUM,
    val hasChanges: Boolean,
    val onAddCustomToken: () -> Unit,
    val onSaveClick: () -> Unit,
)
