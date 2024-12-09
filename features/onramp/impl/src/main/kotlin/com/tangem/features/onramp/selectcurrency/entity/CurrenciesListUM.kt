package com.tangem.features.onramp.selectcurrency.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CurrenciesListUM {

    val searchBarUM: SearchBarUM

    data class Loading(
        override val searchBarUM: SearchBarUM,
        val sections: ImmutableList<CurrenciesSection<CurrencyItemState.Loading>>,
    ) : CurrenciesListUM

    data class Error(override val searchBarUM: SearchBarUM, val onRetry: () -> Unit) : CurrenciesListUM

    data class Content(
        override val searchBarUM: SearchBarUM,
        val sections: ImmutableList<CurrenciesSection<CurrencyItemState.Content>>,
    ) : CurrenciesListUM

    fun copySealed(searchBarUM: SearchBarUM = this.searchBarUM): CurrenciesListUM {
        return when (this) {
            is Loading -> this.copy(searchBarUM = searchBarUM)
            is Error -> this.copy(searchBarUM = searchBarUM)
            is Content -> this.copy(searchBarUM = searchBarUM)
        }
    }
}