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
     * [predefinedAddress], [predefinedNetworkId] and [predefinedMemo] are set only when the feature is opened in
     * [com.tangem.common.routing.entity.AddressBookOpenMode.WithContactCreation] mode — the address and its
     * network are already known, so the new contact is opened with that address already attached. [predefinedMemo]
     * stays null when the network has no transaction extras or no memo was entered.
     */
    @Serializable
    data class EditContact(
        val contactId: String? = null,
        val predefinedAddress: String? = null,
        val predefinedNetworkId: String? = null,
        val predefinedMemo: String? = null,
    ) : AddressBookRoute()

    /**
     * Address entry screen. [walletId] / [excludeContactId] scope the `network + address` duplicate check to the target
     * wallet (excluding the contact being edited). When [prefillAddress] is set the screen opens pre-filled (edit-address
     * flow): [prefillNetworkIds] restores the previously chosen networks and [prefillMemo] the memo.
     */
    @Serializable
    data class AddAddress(
        val walletId: String? = null,
        val excludeContactId: String? = null,
        val prefillAddress: String? = null,
        val prefillNetworkIds: kotlin.collections.List<String> = emptyList(),
        val prefillMemo: String? = null,
    ) : AddressBookRoute()

    /**
     * Network-selection screen. [matchedNetworkIds] are the networks the entered address already resolved to (computed
     * once on the AddAddress screen and passed in, so this screen never re-validates the address against every chain).
     * [selectedNetworkIds] carries the current selection so it can be restored; empty means nothing is pre-selected.
     */
    @Serializable
    data class SelectNetworks(
        val matchedNetworkIds: kotlin.collections.List<String>,
        val selectedNetworkIds: kotlin.collections.List<String> = emptyList(),
    ) : AddressBookRoute()

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