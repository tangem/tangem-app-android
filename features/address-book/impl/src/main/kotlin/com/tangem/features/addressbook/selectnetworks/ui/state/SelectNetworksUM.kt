package com.tangem.features.addressbook.selectnetworks.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.search.TangemSearch
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class SelectNetworksUM(
    val searchBar: TangemSearch.State,
    val networks: ImmutableList<NetworkItemUM>,
    val doneButton: TangemButtonUM,
    val onBackClick: () -> Unit,
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
}