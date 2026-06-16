package com.tangem.features.addressbook.list.contract

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.domain.addressbook.model.Contact
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class AddressBookListUM {

    data class Empty(
        val tangemButtonUM: TangemButtonUM,
    ) : AddressBookListUM()
    data class AddressList(val contacts: ImmutableList<Contact>) : AddressBookListUM()
}