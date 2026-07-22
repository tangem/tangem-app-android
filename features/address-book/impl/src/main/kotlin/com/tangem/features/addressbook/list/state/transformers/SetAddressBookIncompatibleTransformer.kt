package com.tangem.features.addressbook.list.state.transformers

import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.utils.transformer.Transformer

/**
 * Replaces the whole list state with [AddressBookListUM.Incompatible] — used when a stored book uses a
 * contract version newer than this build supports.
 */
internal class SetAddressBookIncompatibleTransformer : Transformer<AddressBookListUM> {

    override fun transform(prevState: AddressBookListUM): AddressBookListUM = AddressBookListUM.Incompatible
}