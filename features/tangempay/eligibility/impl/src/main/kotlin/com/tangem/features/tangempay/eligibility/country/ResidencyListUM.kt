package com.tangem.features.tangempay.eligibility.country

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ResidencyListUM(
    val title: TextReference,
    val search: TangemSearch.State,
    val content: Content,
    val onBackClick: (() -> Unit)?,
    val onCloseClick: () -> Unit,
) {

    @Immutable
    sealed interface Content {

        data object Loading : Content

        data object Empty : Content

        data class Error(val onRetryClick: () -> Unit) : Content

        data class Sections(val sections: ImmutableList<SectionUM>) : Content
    }

    @Immutable
    data class SectionUM(
        val id: String,
        val title: TextReference?,
        val items: ImmutableList<ItemUM>,
    )

    @Immutable
    data class ItemUM(
        val id: String,
        val name: String,
        val flagUrl: String?,
        val isLoading: Boolean,
        val onClick: () -> Unit,
    )
}