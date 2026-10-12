package com.tangem.features.txhistory.model

import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/** Address-book fixtures for the tests that resolve a transaction counterparty against saved contacts. */
internal object TestContacts {

    fun create(
        name: String,
        address: String,
        walletId: UserWalletId,
        networkId: Network.RawID,
        icon: String = "",
        iconColor: String = "Azure",
        createdAt: String = "2026-01-01T00:00:00.000Z",
    ): Contact = Contact(
        id = ContactId("contact-$name"),
        walletId = walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = icon,
        iconColor = iconColor,
        createdAt = createdAt,
        updatedAt = createdAt,
        addresses = listOf(
            AddressEntry(
                id = AddressEntryId("entry-$name"),
                address = address,
                networkId = networkId,
                memo = null,
                signature = "sig",
            ),
        ),
    )
}