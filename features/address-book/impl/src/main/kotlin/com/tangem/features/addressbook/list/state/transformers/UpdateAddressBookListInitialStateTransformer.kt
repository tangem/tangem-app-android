package com.tangem.features.addressbook.list.state.transformers

import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.list.ui.state.ContentMode
import com.tangem.utils.transformer.Transformer

/**
 * Wires the "add contact" callback owned by the container into the initial (empty) list state.
 */
internal class UpdateAddressBookListInitialStateTransformer(
    private val onAddContactClick: () -> Unit,
) : Transformer<AddressBookListUM> {

    override fun transform(prevState: AddressBookListUM): AddressBookListUM {
        return when (prevState) {
            is AddressBookListUM.Empty -> prevState.copy(onAddClick = onAddContactClick)
            is AddressBookListUM.Content -> prevState.copy(
                contentMode = ContentMode.Default(onAddClick = onAddContactClick),
            )
        }
    }
}