package com.tangem.features.addressbook.common

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.addressbook.model.*
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test

internal class ContactMatcherTest {

    @Test
    fun `GIVEN contacts WHEN match THEN keeps only contacts with an address in the network`() {
        // Arrange
        val ethContact = contact("Binance", entry("0xAAA", ETHEREUM), entry("Trx", TRON))
        val tronOnly = contact("Tron Friend", entry("Trx2", TRON))

        // Act
        val result = ContactMatcher.match(listOf(ethContact, tronOnly), networkId = ETHEREUM)

        // Assert
        assertThat(result.map { it.name }).containsExactly("Binance")
        assertThat(result.single().entries.map { it.address }).containsExactly("0xAAA")
    }

    @Test
    fun `GIVEN no contact in the network WHEN match THEN returns empty`() {
        // Arrange
        val tronOnly = contact("Tron Friend", entry("Trx", TRON))

        // Act
        val result = ContactMatcher.match(listOf(tronOnly), networkId = ETHEREUM)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN contact with multiple addresses in the network WHEN match THEN all those entries are returned`() {
        // Arrange
        val exchange = contact("Exchange", entry("0xAAA", ETHEREUM, memo = "1"), entry("0xBBB", ETHEREUM))

        // Act
        val result = ContactMatcher.match(listOf(exchange), networkId = ETHEREUM)

        // Assert
        val entries = result.single().entries
        assertThat(entries.map { it.address }).containsExactly("0xAAA", "0xBBB")
        assertThat(entries.first { it.address == "0xAAA" }.memo).isEqualTo("1")
    }

    @Test
    fun `GIVEN contact with stored color WHEN match THEN avatar color is taken from the contact`() {
        // Arrange
        val contact = contact("Binance", entry("0xAAA", ETHEREUM), iconColor = "MexicanPink")

        // Act
        val result = ContactMatcher.match(listOf(contact), networkId = ETHEREUM)

        // Assert
        assertThat(result.single().icon.color).isEqualTo(CryptoPortfolioIcon.Color.MexicanPink)
    }

    @Test
    fun `GIVEN contact with unknown color WHEN match THEN avatar color falls back to default`() {
        // Arrange
        val contact = contact("Binance", entry("0xAAA", ETHEREUM), iconColor = "not-a-color")

        // Act
        val result = ContactMatcher.match(listOf(contact), networkId = ETHEREUM)

        // Assert
        assertThat(result.single().icon.color).isEqualTo(CryptoPortfolioIcon.Color.Azure)
    }

    private fun contact(name: String, vararg entries: AddressEntry, iconColor: String = "Azure"): Contact = Contact(
        id = ContactId(name),
        walletId = UserWalletId(stringValue = "0001"),
        name = requireNotNull(ContactName(name).getOrNull()) { "invalid test name" },
        icon = "",
        iconColor = iconColor,
        createdAt = "2026-06-10T14:30:00.000Z",
        updatedAt = "2026-06-10T14:30:00.000Z",
        addressEntries = entries.toList(),
    )

    private fun entry(address: String, networkId: String, memo: String? = null): AddressEntry = AddressEntry(
        id = AddressEntryId(address),
        address = address,
        networkId = Network.RawID(networkId),
        memo = memo,
        signature = "sig",
        networkName = "Ethereum",
    )

    private companion object {
        const val ETHEREUM = "ethereum"
        const val TRON = "tron"
    }
}