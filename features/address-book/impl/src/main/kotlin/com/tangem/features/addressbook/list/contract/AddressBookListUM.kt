package com.tangem.features.addressbook.list.contract

import androidx.compose.runtime.Immutable
import com.tangem.domain.addressbook.model.Contact
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class AddressBookListUM {

    data object Empty : AddressBookListUM()
    data class AddressList(val contacts: ImmutableList<Contact>) : AddressBookListUM()
}