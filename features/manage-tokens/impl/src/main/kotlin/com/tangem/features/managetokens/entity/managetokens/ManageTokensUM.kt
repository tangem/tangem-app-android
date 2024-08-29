package com.tangem.features.managetokens.entity.managetokens

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class ManageTokensUM {

    abstract val popBack: () -> Unit
    abstract val isInitialBatchLoading: Boolean
    abstract val isNextBatchLoading: Boolean
    abstract val items: ImmutableList<CurrencyItemUM>
    abstract val topBar: ManageTokensTopBarUM
    abstract val search: SearchBarUM
    abstract val loadMore: () -> Boolean

    data class ReadContent(
        override val popBack: () -> Unit,
        override val isInitialBatchLoading: Boolean,
        override val isNextBatchLoading: Boolean,
        override val items: ImmutableList<CurrencyItemUM>,
        override val topBar: ManageTokensTopBarUM,
        override val search: SearchBarUM,
        override val loadMore: () -> Boolean,
    ) : ManageTokensUM()

    data class ManageContent(
        override val popBack: () -> Unit,
        override val isInitialBatchLoading: Boolean,
        override val isNextBatchLoading: Boolean,
        override val items: ImmutableList<CurrencyItemUM>,
        override val topBar: ManageTokensTopBarUM,
        override val search: SearchBarUM,
        override val loadMore: () -> Boolean,
        val saveChanges: () -> Unit,
        val hasChanges: Boolean,
        val isSavingInProgress: Boolean,
    ) : ManageTokensUM()

    fun copySealed(
        search: SearchBarUM = this.search,
        items: ImmutableList<CurrencyItemUM> = this.items,
        hasChanges: Boolean = this is ManageContent && this.hasChanges,
        isInitialBatchLoading: Boolean = this.isInitialBatchLoading,
        isNextBatchLoading: Boolean = this.isNextBatchLoading,
        isSavingInProgress: Boolean = this is ManageContent && this.isSavingInProgress,
    ): ManageTokensUM {
        return when (this) {
            is ManageContent -> copy(
                search = search,
                items = items,
                hasChanges = hasChanges,
                isInitialBatchLoading = isInitialBatchLoading,
                isNextBatchLoading = isNextBatchLoading,
                isSavingInProgress = isSavingInProgress,
            )
            is ReadContent -> copy(
                search = search,
                items = items,
                isInitialBatchLoading = isInitialBatchLoading,
                isNextBatchLoading = isNextBatchLoading,
            )
        }
    }
}