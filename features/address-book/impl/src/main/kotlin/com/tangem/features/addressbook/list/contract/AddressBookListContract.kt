package com.tangem.features.addressbook.list.contract

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface AddressBookListUM {

    data object Empty : AddressBookListUM
}

internal sealed interface AddressBookListEvent {
    data object NewContactClick : AddressBookListEvent
    data class ContactClick(val contactId: String) : AddressBookListEvent
    data class ChipClick(val walletId: String) : AddressBookListEvent
    data class SearchInput(val query: String) : AddressBookListEvent
}