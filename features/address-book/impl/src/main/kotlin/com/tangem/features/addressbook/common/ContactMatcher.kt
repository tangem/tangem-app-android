package com.tangem.features.addressbook.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.portfolioIcon
import com.tangem.features.addressbook.MatchedContact
import kotlinx.collections.immutable.toImmutableList

/**
 * Maps contacts to [MatchedContact]s for a given [networkId], keeping only those that have at least one address in that
 * network (with just the matching entries). Name/address query filtering is done upstream by `GetContactsUseCase`.
 */
internal object ContactMatcher {

    fun match(contacts: List<Contact>, networkId: String): List<MatchedContact> {
        return contacts.mapNotNull { contact ->
            val entries = contact.addresses.filter { it.networkId.value == networkId }
            if (entries.isEmpty()) return@mapNotNull null

            MatchedContact(
                contactId = contact.id.value,
                walletId = contact.walletId.stringValue,
                name = contact.name.value,
                icon = CryptoPortfolioIconConverter.convert(contact.portfolioIcon()),
                networkId = networkId,
                entries = entries.map { entry ->
                    MatchedContact.ContactAddress(
                        address = entry.address,
                        memo = entry.memo,
                        networkName = entry.displayNetworkName(),
                    )
                }.toImmutableList(),
            )
        }
    }

    /** Display network name is derived from [AddressEntry.networkId] — it is not stored in the encrypted payload. */
    private fun AddressEntry.displayNetworkName(): String =
        Blockchain.fromNetworkId(networkId.value)?.fullName ?: networkId.value
}