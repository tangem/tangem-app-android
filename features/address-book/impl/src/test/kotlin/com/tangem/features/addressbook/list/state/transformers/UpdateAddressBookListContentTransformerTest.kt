package com.tangem.features.addressbook.list.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.addressbook.model.*
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.route.AddressBookRoute
import org.junit.jupiter.api.Test

internal class UpdateAddressBookListContentTransformerTest {

    private val wallet1 = "00"
    private val wallet2 = "01"

    private val wallets: Map<UserWalletId, UserWallet> = linkedMapOf(
        UserWalletId(stringValue = wallet1) to wallet(wallet1, "Wallet 1"),
        UserWalletId(stringValue = wallet2) to wallet(wallet2, "Wallet 2"),
    )

    @Test
    fun `GIVEN contacts in one wallet WHEN blank query THEN no chips`() {
        // Arrange
        val all = listOf(verified(wallet1, "Alice"), verified(wallet1, "Bob"))

        // Act
        val result = transform(allContacts = all, matchedContacts = all)

        // Assert
        val content = result as AddressBookListUM.Content
        assertThat(content.chips).isEmpty()
        assertThat(content.contacts).hasSize(2)
        assertThat(content.isNothingFound).isFalse()
        // Single-wallet book has no chips, so the wallet name is not shown.
        assertThat(content.contacts.none { it.walletName != null }).isTrue()
    }

    @Test
    fun `GIVEN contacts in two wallets WHEN blank query THEN All plus per-wallet chips with All selected`() {
        // Arrange
        val all = listOf(verified(wallet1, "Alice"), verified(wallet2, "Bob"))

        // Act
        val result = transform(allContacts = all, matchedContacts = all)

        // Assert
        val content = result as AddressBookListUM.Content
        assertThat(content.chips.map { it.id }).containsExactly("all", wallet1, wallet2).inOrder()
        assertThat(content.chips.first().isSelected).isTrue() // All
        assertThat(content.chips.drop(1).none { it.isSelected }).isTrue()
        assertThat(content.contacts).hasSize(2)
        // On the "All" chip of a multi-wallet book each contact shows its wallet name.
        assertThat(content.contacts.map { it.walletName }).containsExactly("Wallet 1", "Wallet 2").inOrder()
    }

    @Test
    fun `GIVEN two wallets WHEN a wallet chip selected THEN list filtered but chips unchanged`() {
        // Arrange
        val all = listOf(verified(wallet1, "Alice"), verified(wallet2, "Bob"))

        // Act
        val result = transform(allContacts = all, matchedContacts = all, selectedWalletId = wallet2)

        // Assert
        val content = result as AddressBookListUM.Content
        assertThat(content.chips.map { it.id }).containsExactly("all", wallet1, wallet2).inOrder()
        assertThat(content.chips.first().isSelected).isFalse() // All not selected
        assertThat(content.contacts.map { it.name }).containsExactly("Bob")
        // A specific wallet is selected, so the (redundant) wallet name is not shown.
        assertThat(content.contacts.single().walletName).isNull()
    }

    @Test
    fun `GIVEN two wallets WHEN query narrows to one wallet THEN chips kept as All plus that wallet`() {
        // Arrange — the book spans two wallets, but the query matched only wallet1 in the domain
        val all = listOf(verified(wallet1, "Antonio"), verified(wallet2, "Bob"))
        val matched = listOf(verified(wallet1, "Antonio"))

        // Act
        val result = transform(allContacts = all, matchedContacts = matched, query = "Anto")

        // Assert
        val content = result as AddressBookListUM.Content
        assertThat(content.chips.map { it.id }).containsExactly("all", wallet1).inOrder()
        assertThat(content.contacts.map { it.name }).containsExactly("Antonio")
        assertThat(content.isNothingFound).isFalse()
    }

    @Test
    fun `GIVEN selected wallet no longer matches query THEN falls back to All`() {
        // Arrange
        val all = listOf(verified(wallet1, "Antonio"), verified(wallet2, "Bob"))
        val matched = listOf(verified(wallet1, "Antonio"))

        // Act — selected wallet2, but query matched only wallet1
        val result = transform(
            allContacts = all,
            matchedContacts = matched,
            selectedWalletId = wallet2,
            query = "Anto",
        )

        // Assert
        val content = result as AddressBookListUM.Content
        assertThat(content.chips.first().isSelected).isTrue() // All selected again
        assertThat(content.contacts.map { it.name }).containsExactly("Antonio")
    }

    @Test
    fun `GIVEN no contacts WHEN blank query THEN Empty`() {
        // Act
        val result = transform(allContacts = emptyList(), matchedContacts = emptyList())

        // Assert
        assertThat(result).isInstanceOf(AddressBookListUM.Empty::class.java)
    }

    @Test
    fun `GIVEN query matches nothing WHEN non-blank query THEN nothing found and chips hidden`() {
        // Arrange
        val all = listOf(verified(wallet1, "Alice"), verified(wallet2, "Bob"))

        // Act
        val result = transform(allContacts = all, matchedContacts = emptyList(), query = "Zzz")

        // Assert
        val content = result as AddressBookListUM.Content
        assertThat(content.isNothingFound).isTrue()
        assertThat(content.contacts).isEmpty()
        assertThat(content.chips).isEmpty()
    }

    private fun transform(
        allContacts: List<VerifiedContact>,
        matchedContacts: List<VerifiedContact>,
        selectedWalletId: String? = null,
        query: String = "",
    ): AddressBookListUM = UpdateAddressBookListContentTransformer(
        allContacts = allContacts,
        matchedContacts = matchedContacts,
        mode = AddressBookRoute.ListMode.Default,
        wallets = wallets,
        selectedWalletId = selectedWalletId,
        query = query,
        onContactClick = {},
        onPickContact = {},
        onQueryChange = {},
        onActiveChange = {},
        onClearQuery = {},
        onChipSelected = {},
        onAddContactClick = {},
    ).transform(prevState = AddressBookListUM.Empty(onAddClick = {}))

    private fun wallet(id: String, name: String): UserWallet =
        MockUserWalletFactory.create().copy(walletId = UserWalletId(stringValue = id), name = name)

    private fun verified(walletId: String, name: String): VerifiedContact = VerifiedContact(
        contact = Contact(
            id = ContactId(name + walletId),
            walletId = UserWalletId(stringValue = walletId),
            name = requireNotNull(ContactName(name).getOrNull()) { "invalid test name" },
            icon = "",
            iconColor = "Azure",
            createdAt = "2026-06-10T14:30:00.000Z",
            updatedAt = "2026-06-10T14:30:00.000Z",
            addressEntries = listOf(
                AddressEntry(
                    id = AddressEntryId(name),
                    address = "addr-$name",
                    networkId = Network.RawID("ethereum"),
                    memo = null,
                    signature = "sig",
                    networkName = "Ethereum",
                ),
            ),
        ),
        invalidEntries = emptyList(),
    )
}