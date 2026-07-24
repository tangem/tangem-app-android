package com.tangem.features.addressbook.selectnetworks.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class SelectNetworksUM(
    val searchBar: TangemSearch.State,
    val networks: ImmutableList<NetworkItemUM>,
    val selectAllButton: SelectAllButtonUM,
    val doneButton: TangemButtonUM,
    val onBackClick: () -> Unit,
    val onSelectAllClick: () -> Unit,
) {

    @Immutable
    data class NetworkItemUM(
        val id: String,
        val name: String,
        val symbol: String,
        @DrawableRes val iconResId: Int,
        val isSelected: Boolean,
        val onCheckedChange: () -> Unit,
    )

    enum class SelectAllButtonUM(val text: TextReference) {
        SelectAll(text = resourceReference(R.string.address_book_select_all_networks)),
        ClearAll(text = resourceReference(R.string.address_book_clear_all_networks)),
        Empty(text = TextReference.EMPTY),
    }
}