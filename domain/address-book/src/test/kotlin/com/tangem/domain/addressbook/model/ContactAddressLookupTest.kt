package com.tangem.domain.addressbook.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Tests for [ContactAddressLookup.resolve] — the address-book matching rules of a transaction-history counterparty:
 * the transaction's own wallet wins, then the newest contact, and a case-insensitive family matches case variants.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactAddressLookupTest {

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ResolveModel) {
        // Arrange
        val lookup = ContactAddressLookup.of(model.contacts)

        // Act
        val actual = lookup.resolve(
            networkId = Network.RawID(model.networkId),
            address = model.address,
            preferredWalletId = model.preferredWalletId,
        )

        // Assert
        assertThat(actual?.name?.value).isEqualTo(model.expectedName)
    }

    @Test
    fun `GIVEN no contacts WHEN of THEN returns the empty lookup`() {
        // Act
        val lookup = ContactAddressLookup.of(contacts = emptyList())

        // Assert
        assertThat(lookup).isEqualTo(ContactAddressLookup.EMPTY)
    }

    @Test
    fun `GIVEN address saved in the given wallet WHEN isSavedIn THEN true`() {
        // Arrange
        val lookup = ContactAddressLookup.of(listOf(contact(name = "Alice", walletId = TX_WALLET)))

        // Act
        val actual = lookup.isSavedIn(
            walletId = TX_WALLET,
            networkId = Network.RawID(ETHEREUM),
            address = ETH_ADDRESS,
        )

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN address saved only in another wallet WHEN isSavedIn THEN false`() {
        // Arrange — the contact of another wallet is displayed by name, but this wallet's book can still take it.
        val lookup = ContactAddressLookup.of(listOf(contact(name = "Bob", walletId = OTHER_WALLET)))

        // Act
        val actual = lookup.isSavedIn(
            walletId = TX_WALLET,
            networkId = Network.RawID(ETHEREUM),
            address = ETH_ADDRESS,
        )

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN EVM address saved in a different case WHEN isSavedIn THEN true`() {
        // Arrange
        val lookup = ContactAddressLookup.of(
            listOf(contact(name = "Alice", walletId = TX_WALLET, address = "0xAbCdEf")),
        )

        // Act
        val actual = lookup.isSavedIn(
            walletId = TX_WALLET,
            networkId = Network.RawID(ETHEREUM),
            address = "0xabcdef",
        )

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN address saved on another network WHEN isSavedIn THEN false`() {
        // Arrange
        val lookup = ContactAddressLookup.of(listOf(contact(name = "Alice", walletId = TX_WALLET)))

        // Act
        val actual = lookup.isSavedIn(
            walletId = TX_WALLET,
            networkId = Network.RawID(SOLANA),
            address = ETH_ADDRESS,
        )

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN contact with several addresses WHEN resolve THEN each address resolves to it`() {
        // Arrange
        val contact = contact(
            name = "Alice",
            walletId = TX_WALLET,
            addresses = listOf(ETH_ADDRESS to ETHEREUM, SOL_ADDRESS to SOLANA),
        )
        val lookup = ContactAddressLookup.of(listOf(contact))

        // Act
        val onEthereum = lookup.resolve(Network.RawID(ETHEREUM), ETH_ADDRESS, TX_WALLET)
        val onSolana = lookup.resolve(Network.RawID(SOLANA), SOL_ADDRESS, TX_WALLET)

        // Assert
        assertThat(onEthereum).isEqualTo(contact)
        assertThat(onSolana).isEqualTo(contact)
    }

    data class ResolveModel(
        val description: String,
        val contacts: List<Contact>,
        val address: String,
        val preferredWalletId: UserWalletId?,
        val expectedName: String?,
        val networkId: String = ETHEREUM,
    ) {

        override fun toString(): String = description
    }

    private fun provideTestModels() = listOf(
        ResolveModel(
            description = "address saved in the transaction's wallet",
            contacts = listOf(contact(name = "Alice", walletId = TX_WALLET)),
            address = ETH_ADDRESS,
            preferredWalletId = TX_WALLET,
            expectedName = "Alice",
        ),
        ResolveModel(
            description = "address saved only in another wallet",
            contacts = listOf(contact(name = "Bob", walletId = OTHER_WALLET)),
            address = ETH_ADDRESS,
            preferredWalletId = TX_WALLET,
            expectedName = "Bob",
        ),
        ResolveModel(
            description = "same address in the transaction's wallet and in a newer contact of another wallet",
            contacts = listOf(
                contact(name = "Bob", walletId = OTHER_WALLET, createdAt = NEWER),
                contact(name = "Alice", walletId = TX_WALLET, createdAt = OLDER),
            ),
            address = ETH_ADDRESS,
            preferredWalletId = TX_WALLET,
            expectedName = "Alice",
        ),
        ResolveModel(
            description = "same address in two contacts of the transaction's wallet",
            contacts = listOf(
                contact(name = "Old", walletId = TX_WALLET, createdAt = OLDER),
                contact(name = "New", walletId = TX_WALLET, createdAt = NEWER),
            ),
            address = ETH_ADDRESS,
            preferredWalletId = TX_WALLET,
            expectedName = "New",
        ),
        ResolveModel(
            description = "same address in two other wallets",
            contacts = listOf(
                contact(name = "Old", walletId = OTHER_WALLET, createdAt = OLDER),
                contact(name = "New", walletId = THIRD_WALLET, createdAt = NEWER),
            ),
            address = ETH_ADDRESS,
            preferredWalletId = TX_WALLET,
            expectedName = "New",
        ),
        ResolveModel(
            description = "no preferred wallet falls back to the newest contact",
            contacts = listOf(
                contact(name = "Old", walletId = TX_WALLET, createdAt = OLDER),
                contact(name = "New", walletId = OTHER_WALLET, createdAt = NEWER),
            ),
            address = ETH_ADDRESS,
            preferredWalletId = null,
            expectedName = "New",
        ),
        ResolveModel(
            description = "unknown address",
            contacts = listOf(contact(name = "Alice", walletId = TX_WALLET)),
            address = "0xbbbbbb",
            preferredWalletId = TX_WALLET,
            expectedName = null,
        ),
        ResolveModel(
            description = "address known on another network",
            contacts = listOf(contact(name = "Alice", walletId = TX_WALLET)),
            address = ETH_ADDRESS,
            preferredWalletId = TX_WALLET,
            expectedName = null,
            networkId = "tron",
        ),
        ResolveModel(
            description = "EVM address differing only in case",
            contacts = listOf(contact(name = "Alice", walletId = TX_WALLET, address = "0xAbCdEf")),
            address = "0xabcdef",
            preferredWalletId = TX_WALLET,
            expectedName = "Alice",
        ),
        ResolveModel(
            description = "non-EVM address differing only in case",
            contacts = listOf(
                contact(name = "Dave", walletId = TX_WALLET, address = SOL_ADDRESS, networkId = SOLANA),
            ),
            address = SOL_ADDRESS.lowercase(),
            preferredWalletId = TX_WALLET,
            expectedName = null,
            networkId = SOLANA,
        ),
        ResolveModel(
            description = "non-EVM address with exact case",
            contacts = listOf(
                contact(name = "Dave", walletId = TX_WALLET, address = SOL_ADDRESS, networkId = SOLANA),
            ),
            address = SOL_ADDRESS,
            preferredWalletId = TX_WALLET,
            expectedName = "Dave",
            networkId = SOLANA,
        ),
        ResolveModel(
            description = "empty address book",
            contacts = emptyList(),
            address = ETH_ADDRESS,
            preferredWalletId = TX_WALLET,
            expectedName = null,
        ),
    )

    private fun contact(
        name: String,
        walletId: UserWalletId,
        address: String = ETH_ADDRESS,
        networkId: String = ETHEREUM,
        createdAt: String = OLDER,
        addresses: List<Pair<String, String>> = listOf(address to networkId),
    ): Contact = Contact(
        id = ContactId("id-$name-${walletId.stringValue}"),
        walletId = walletId,
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = CONTACT_ICON_COLOR,
        createdAt = createdAt,
        updatedAt = createdAt,
        addresses = addresses.mapIndexed { index, (entryAddress, entryNetworkId) ->
            AddressEntry(
                id = AddressEntryId("addr-$name-$index"),
                address = entryAddress,
                networkId = Network.RawID(entryNetworkId),
                memo = null,
                signature = "sig",
            )
        },
    )

    private companion object {
        const val ETHEREUM = "ethereum"
        const val SOLANA = "solana"
        const val ETH_ADDRESS = "0xaaaaaa"
        const val SOL_ADDRESS = "SoLAnaAddr"
        const val CONTACT_ICON_COLOR = "Azure"
        const val OLDER = "2026-01-01T00:00:00.000Z"
        const val NEWER = "2026-06-01T00:00:00.000Z"

        val TX_WALLET = UserWalletId("011")
        val OTHER_WALLET = UserWalletId("022")
        val THIRD_WALLET = UserWalletId("033")
    }
}