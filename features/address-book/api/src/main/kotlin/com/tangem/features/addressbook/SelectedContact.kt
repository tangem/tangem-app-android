package com.tangem.features.addressbook

import com.tangem.common.ui.account.AccountIconUM

/**
 * A single resolved address-book pick. Produced once the concrete address within a contact is known — either directly
 * (the contact has a single matching-network address) or after the user chose one in the address selector.
 *
 * Feature-agnostic: any feature that opens the address book for selection receives this result.
 *
 * @property contactId  the id of the source [com.tangem.domain.addressbook.model.Contact]
 * @property name       the contact name to display
 * @property icon       the contact avatar (initials + color), reusing the account icon UI model
 * @property address    the chosen on-chain address
 * @property networkId  raw id of the network the address belongs to
 * @property memo       optional memo/destination tag (only meaningful for networks that support it)
 */
data class SelectedContact(
    val contactId: String,
    val name: String,
    val icon: AccountIconUM.CryptoPortfolio,
    val address: String,
    val networkId: String,
    val memo: String?,
)