package com.tangem.features.addressbook.component

import kotlinx.serialization.Serializable

@Serializable
internal sealed class AddressBookRoute {

    @Serializable
    data object List : AddressBookRoute()

    /**
     * if [contactId] is not null we should fetch existing contact
     */
    @Serializable
    data class EditContact(
        val contactId: String? = null,
    ) : AddressBookRoute()
}