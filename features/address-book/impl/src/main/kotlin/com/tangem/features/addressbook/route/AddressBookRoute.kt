package com.tangem.features.addressbook.route

import kotlinx.serialization.Serializable

@Serializable
internal sealed class AddressBookRoute {

    @Serializable
    data object List : AddressBookRoute()

    /**
     * if [contactId] is not null we should fetch existing contact.
     *
     * [predefinedAddress] and [predefinedNetworkId] are set only when the feature is opened in
     * [com.tangem.features.addressbook.entity.AddressBookOpenMode.WithContactCreation] mode — the address and its
     * network are already known, so the new contact is opened with that address already attached.
     */
    @Serializable
    data class EditContact(
        val contactId: String? = null,
        val predefinedAddress: String? = null,
        val predefinedNetworkId: String? = null,
    ) : AddressBookRoute()

    @Serializable
    data object AddAddress : AddressBookRoute()
}