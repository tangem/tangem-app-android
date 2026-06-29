package com.tangem.features.addressbook.list.state.transformers

import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.utils.transformer.Transformer

internal class UpdateAddressBookListQueryTransformer(
    private val query: String,
    private val isActive: Boolean,
) : Transformer<AddressBookListUM> {

    override fun transform(prevState: AddressBookListUM): AddressBookListUM = when (prevState) {
        is AddressBookListUM.Content -> prevState.copy(
            searchBar = prevState.searchBar.copy(query = query, isActive = isActive),
        )
        is AddressBookListUM.Empty -> prevState
    }
}