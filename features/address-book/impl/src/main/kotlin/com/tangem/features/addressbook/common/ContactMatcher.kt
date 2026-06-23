package com.tangem.features.addressbook.common

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.MatchedContact
import kotlinx.collections.immutable.toImmutableList

/**
 * Maps contacts to [MatchedContact]s for a given [networkId], keeping only those that have at least one address in that
 * network (with just the matching entries). Name/address query filtering is done upstream by `GetContactsUseCase`.
 */
internal object ContactMatcher {

    private val DEFAULT_ICON_COLOR = CryptoPortfolioIcon.Color.Azure

    fun match(contacts: List<Contact>, networkId: String): List<MatchedContact> {
        return contacts.mapNotNull { contact ->
            val entries = contact.addressEntries.filter { it.networkId.value == networkId }
            if (entries.isEmpty()) return@mapNotNull null

            MatchedContact(
                contactId = contact.id.value,
                name = contact.name.value,
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Letter,
                    color = contact.resolveIconColor(),
                ),
                networkId = networkId,
                entries = entries.map { entry ->
                    MatchedContact.ContactAddress(
                        address = entry.address,
                        memo = entry.memo,
                        networkName = entry.networkName,
                    )
                }.toImmutableList(),
            )
        }
    }

    private fun Contact.resolveIconColor(): CryptoPortfolioIcon.Color =
        CryptoPortfolioIcon.Color.entries.firstOrNull { it.name == iconColor } ?: DEFAULT_ICON_COLOR
}