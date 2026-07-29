package com.tangem.common.routing.entity

import kotlinx.serialization.Serializable

/** How the address book is opened — part of the navigation contract, carried by [com.tangem.common.routing.AppRoute.AddressBook]. */
@Serializable
sealed interface AddressBookOpenMode {

    @Serializable
    data object Default : AddressBookOpenMode

    /**
     * Opened to create a contact for an already-known [address] on [networkId]. [memo] carries the destination tag /
     * memo that went with it, when the network supports transaction extras and one was entered.
     */
    @Serializable
    data class WithContactCreation(
        val address: String,
        val networkId: String,
        val memo: String? = null,
    ) : AddressBookOpenMode

    /**
     * Opened from the Send flow to pick a recipient. The list is filtered by [networkId] (the current send network),
     * and the chosen contact's address is delivered back via `ContactSelectionTrigger` rather than navigation.
     */
    @Serializable
    data class ContactSelection(
        val networkId: String,
    ) : AddressBookOpenMode
}