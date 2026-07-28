package com.tangem.features.addressbook.selectnetworks.state.transformers

import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM
import com.tangem.utils.transformer.Transformer

internal class UpdateSelectNetworksSearchBarTransformer(
    private val query: String,
    private val isActive: Boolean,
) : Transformer<SelectNetworksUM> {

    override fun transform(prevState: SelectNetworksUM): SelectNetworksUM {
        return prevState.copy(
            searchBar = prevState.searchBar.copy(query = query, isActive = isActive),
        )
    }
}