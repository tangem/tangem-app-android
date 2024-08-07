package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class ManageTokensUM {

    abstract val popBack: () -> Unit
    abstract val isLoading: Boolean
    abstract val items: ImmutableList<CurrencyItemUM>
    abstract val topBar: ManageTokensTopBarUM
    abstract val search: SearchBarUM

    data class ReadContent(
        override val popBack: () -> Unit,
        override val isLoading: Boolean,
        override val items: ImmutableList<CurrencyItemUM>,
        override val topBar: ManageTokensTopBarUM,
        override val search: SearchBarUM,
    ) : ManageTokensUM()

    data class ManageContent(
        override val popBack: () -> Unit,
        override val isLoading: Boolean,
        override val items: ImmutableList<CurrencyItemUM>,
        override val topBar: ManageTokensTopBarUM,
        override val search: SearchBarUM,
        val onSaveClick: () -> Unit,
        val hasChanges: Boolean,
    ) : ManageTokensUM()

    fun copySealed(
        search: SearchBarUM = this.search,
        items: ImmutableList<CurrencyItemUM> = this.items,
        hasChanges: Boolean = this is ManageContent && this.hasChanges,
    ): ManageTokensUM {
        return when (this) {
            is ManageContent -> copy(search = search, items = items, hasChanges = hasChanges)
            is ReadContent -> copy(search = search, items = items)
        }
    }
}
