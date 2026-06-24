package com.tangem.features.addressbook

import com.tangem.common.ui.account.AccountIconUM
import kotlinx.collections.immutable.ImmutableList

/**
 * A contact together with its address entries that match a given network. Emitted when a contact is tapped during
 * selection; the host decides what to do with it:
 * - exactly one [entries] item → build a [SelectedContact] and proceed straight away;
 * - more than one → open the address selector so the user picks a concrete address first.
 */

data class MatchedContact(
    val contactId: String,
    val name: String,
    val icon: AccountIconUM.CryptoPortfolio,
    val networkId: String,
    val entries: ImmutableList<ContactAddress>,
) {

    /** Resolves this contact to a concrete pick using one of its [entries]. */
    fun toSelectedContact(entry: ContactAddress): SelectedContact = SelectedContact(
        contactId = contactId,
        name = name,
        icon = icon,
        address = entry.address,
        networkId = networkId,
        memo = entry.memo,
    )

    /** A single network-matching address of the contact. */
    data class ContactAddress(
        val address: String,
        val memo: String?,
        val networkName: String,
    )
}