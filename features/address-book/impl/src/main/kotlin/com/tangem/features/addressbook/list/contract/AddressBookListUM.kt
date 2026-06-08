package com.tangem.features.addressbook.list.contract

import androidx.compose.runtime.Immutable
import com.tangem.domain.addressbook.model.Contact

@Immutable
internal sealed class AddressBookListUM {

    data object Empty : AddressBookListUM()
    data class AddressList(val contacts: List<Contact>) : AddressBookListUM()
}