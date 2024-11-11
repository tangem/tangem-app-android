package com.tangem.features.onramp.selectcountry.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CountryListUM {

    val searchBarUM: SearchBarUM

    data class Loading(
        override val searchBarUM: SearchBarUM,
        val items: ImmutableList<CountryItemState.Loading>,
    ) : CountryListUM

    data class Error(override val searchBarUM: SearchBarUM, val onRetry: () -> Unit) : CountryListUM

    data class Content(
        override val searchBarUM: SearchBarUM,
        val items: ImmutableList<CountryItemState.WithContent>,
    ) : CountryListUM

    fun copySealed(searchBarUM: SearchBarUM = this.searchBarUM): CountryListUM {
        return when (this) {
            is Loading -> this.copy(searchBarUM = searchBarUM)
            is Error -> this.copy(searchBarUM = searchBarUM)
            is Content -> this.copy(searchBarUM = searchBarUM)
        }
    }
}