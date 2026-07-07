package com.tangem.domain.addressbook.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * Locks the JSON shape of the encrypted address-book payload — a cross-platform (iOS) contract. Wallet id,
 * contact name and network id must be bare strings, not `{"field": …}` objects, so a Kotlin type change
 * can't silently break interop.
 */
internal class AddressBookSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GIVEN a contact WHEN serialized THEN wrapper types are plain strings`() {
        // Arrange
        val contact = Contact(
            id = ContactId("contact-1"),
            walletId = UserWalletId("0a0a0a"),
            name = requireNotNull(ContactName("Alice").getOrNull()),
            icon = "",
            iconColor = "TestColor",
            createdAt = "2026-01-01T00:00:00.000Z",
            updatedAt = "2026-05-22T09:00:00.000Z",
            addresses = listOf(
                AddressEntry(
                    id = AddressEntryId("addr-1"),
                    address = "0xabc",
                    networkId = Network.RawID("ethereum"),
                    memo = null,
                    signature = "",
                ),
            ),
        )

        // Act
        val obj = json.parseToJsonElement(json.encodeToString(Contact.serializer(), contact)).jsonObject

        // Assert
        assertThat(obj["id"]).isEqualTo(JsonPrimitive("contact-1"))
        assertThat(obj["walletId"]).isEqualTo(JsonPrimitive("0a0a0a"))
        assertThat(obj["name"]).isEqualTo(JsonPrimitive("Alice"))
        val entry = obj["addresses"]!!.jsonArray.single().jsonObject
        assertThat(entry["id"]).isEqualTo(JsonPrimitive("addr-1"))
        assertThat(entry["networkId"]).isEqualTo(JsonPrimitive("ethereum"))
    }

    @Test
    fun `GIVEN serialized contact WHEN deserialized THEN original is restored`() {
        // Arrange
        val book = AddressBook(
            contacts = listOf(
                Contact(
                    id = ContactId("contact-1"),
                    walletId = UserWalletId("0a0a0a"),
                    name = requireNotNull(ContactName("Alice").getOrNull()),
                    icon = "",
                    iconColor = "TestColor",
                    createdAt = "2026-01-01T00:00:00.000Z",
                    updatedAt = "2026-05-22T09:00:00.000Z",
                    addresses = listOf(
                        AddressEntry(
                            id = AddressEntryId("addr-1"),
                            address = "0xabc",
                            networkId = Network.RawID("ethereum"),
                            memo = "memo",
                            signature = "sig",
                        ),
                    ),
                ),
            ),
        )

        // Act
        val restored = json.decodeFromString(
            AddressBook.serializer(),
            json.encodeToString(AddressBook.serializer(), book),
        )

        // Assert
        assertThat(restored).isEqualTo(book)
    }

    @Test
    fun `GIVEN payload with an invalid contact name WHEN deserialized THEN fails`() {
        // Arrange — empty name violates the ContactName rules
        val payload = """{"contacts":[{"id":"c1","walletId":"0a0a0a","name":"",""" +
            """"icon":"","iconColor":"c","createdAt":"t","updatedAt":"t","addresses":[]}]}"""

        // Act
        val error = runCatching { json.decodeFromString(AddressBook.serializer(), payload) }.exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }

    @Test
    fun `GIVEN entry with null memo WHEN serialized THEN memo key is omitted`() {
        // Arrange — kotlinx omits properties equal to their default, so a null memo must not appear in the JSON,
        // matching iOS's encodeIfPresent for String? optionals.
        val entry = AddressEntry(
            id = AddressEntryId("a1"),
            address = "0xabc",
            networkId = Network.RawID("ethereum"),
            memo = null,
            signature = "sig",
        )

        // Act
        val obj = json.parseToJsonElement(json.encodeToString(AddressEntry.serializer(), entry)).jsonObject

        // Assert
        assertThat(obj.keys).doesNotContain("memo")
    }
}