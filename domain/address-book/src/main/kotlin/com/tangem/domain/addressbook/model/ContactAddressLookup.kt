package com.tangem.domain.addressbook.model

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.lib.crypto.BlockchainUtils

/**
 * Address → [Contact] index over the address books of every wallet on the device, used to show a saved contact's name
 * in place of a raw address.
 *
 * Built once per contacts emission by [of] so that a lookup costs O(1) — a transaction history resolves a counterparty
 * for every visible row.
 */
@ConsistentCopyVisibility
data class ContactAddressLookup private constructor(
    private val networks: Map<Network.RawID, NetworkIndex>,
) {

    /**
     * The contact that holds [address] in [networkId], or `null` when the address is in no address book.
     *
     * An address book belongs to a single wallet, so the same address can be saved under different names in several
     * wallets. The winner is a contact of [preferredWalletId] — the wallet the transaction was made in — and, among

     */
    fun resolve(networkId: Network.RawID, address: String, preferredWalletId: UserWalletId?): Contact? {
        val index = networks[networkId] ?: return null
        val candidates = index.contactsByAddress[index.keyOf(address)] ?: return null

        return candidates
            .filter { it.walletId == preferredWalletId }
            .ifEmpty { candidates }
            .maxByOrNull { it.createdAt }
    }

    /**
     * Whether [address] on [networkId] is already saved in [walletId]'s own address book — the condition that decides
     * whether saving it as a contact is still offered. Scoped to one wallet on purpose: a contact of another wallet is
     * displayed by name, but it is not in this wallet's book, so it can still be added there.
     */
    fun isSavedIn(walletId: UserWalletId, networkId: Network.RawID, address: String): Boolean {
        val index = networks[networkId] ?: return false
        val candidates = index.contactsByAddress[index.keyOf(address)] ?: return false

        return candidates.any { it.walletId == walletId }
    }

    private data class NetworkIndex(
        val isCaseInsensitive: Boolean,
        val contactsByAddress: Map<String, List<Contact>>,
    ) {

        fun keyOf(address: String): String = if (isCaseInsensitive) address.lowercase() else address
    }

    companion object {

        val EMPTY = ContactAddressLookup(networks = emptyMap())

        /**
         * Indexes [contacts] — already signature-verified, and taken from every wallet on the device — by network and
         * address.
         *
         * EVM addresses are keyed lowercased: the same address reaches the app EIP-55 checksummed or lowercased
         * depending on the source. Every other family stays case-sensitive, where a case variant is a different
         * address (the rule the address-book search follows too).
         */
        fun of(contacts: List<Contact>): ContactAddressLookup {
            if (contacts.isEmpty()) return EMPTY

            val caseInsensitivity = mutableMapOf<Network.RawID, Boolean>()
            val index = mutableMapOf<Network.RawID, MutableMap<String, MutableList<Contact>>>()

            contacts.forEach { contact ->
                contact.addresses.forEach { entry ->
                    val networkId = entry.networkId
                    val isCaseInsensitive = caseInsensitivity.getOrPut(networkId) {
                        BlockchainUtils.isCaseInsensitiveContractAddress(networkId.value)
                    }
                    val key = if (isCaseInsensitive) entry.address.lowercase() else entry.address

                    index.getOrPut(networkId) { mutableMapOf() }
                        .getOrPut(key) { mutableListOf() }
                        .add(contact)
                }
            }

            return ContactAddressLookup(
                networks = index.mapValues { (networkId, contactsByAddress) ->
                    NetworkIndex(
                        isCaseInsensitive = caseInsensitivity.getValue(networkId),
                        contactsByAddress = contactsByAddress.mapValues { (_, contacts) -> contacts.toList() },
                    )
                },
            )
        }
    }
}