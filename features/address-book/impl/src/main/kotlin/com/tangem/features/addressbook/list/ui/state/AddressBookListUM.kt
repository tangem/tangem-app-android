package com.tangem.features.addressbook.list.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface AddressBookListUM {

    data class Empty(val onAddClick: () -> Unit) : AddressBookListUM

    data class AddressList(val contacts: ImmutableList<ContactUM>) : AddressBookListUM
}