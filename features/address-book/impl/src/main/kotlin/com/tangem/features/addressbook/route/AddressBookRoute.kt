package com.tangem.features.addressbook.route

import kotlinx.serialization.Serializable

@Serializable
internal sealed class AddressBookRoute {

    /**
     * The contacts list. [mode] mirrors the entry point: [ListMode.Default] for plain browsing/management, and
     * [ListMode.Selector] when the list is opened to pick a contact for a given network — a tap then returns the
     * chosen address instead of opening the editor.
     */
    @Serializable
    data class List(val mode: ListMode = ListMode.Default) : AddressBookRoute()

    /**
     * if [contactId] is not null we should fetch existing contact.
     *
     * [predefinedAddress] and [predefinedNetworkId] are set only when the feature is opened in
     * [com.tangem.common.routing.entity.AddressBookOpenMode.WithContactCreation] mode — the address and its
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

    /** How the contacts list is shown — agnostic of which feature opened it. */
    @Serializable
    sealed interface ListMode {

        @Serializable
        data object Default : ListMode

        /** Pick a contact that has an address in [networkId]. */
        @Serializable
        data class Selector(val networkId: String) : ListMode
    }
}