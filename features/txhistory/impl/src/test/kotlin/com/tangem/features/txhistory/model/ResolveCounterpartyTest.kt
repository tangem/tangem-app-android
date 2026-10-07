package com.tangem.features.txhistory.model

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactAddressLookup
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Unit tests for [resolveCounterparty]: the address-book layer on top of [resolveOwner]. Pins that an own portfolio
 * still wins over a saved contact, that an address is matched whichever side of the transfer it is on, and that the
 * wallet the transaction was made in takes priority over another wallet's address book.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveCounterpartyTest {

    @ParameterizedTest
    @ProvideTestModels
    fun resolveCounterparty(model: Model) {
        // Act
        val result = model.lookup.resolveCounterparty(
            address = model.address,
            networkRawId = model.networkRawId,
        )

        // Assert
        assertThat(result).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<Model> = listOf(
        Model(
            name = "own address that is also a saved contact -> OwnAccount",
            lookup = lookup(
                ownAddresses = mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT),
                contacts = listOf(CONTACT_OF_OWN_ADDRESS),
            ),
            address = OWN_ADDRESS,
            expected = ResolvedOwner.OwnAccount(CRYPTO_ACCOUNT),
        ),
        Model(
            name = "address saved in the viewed wallet -> SavedContact",
            lookup = lookup(contacts = listOf(CONTACT_OF_VIEWED_WALLET)),
            address = EXTERNAL_ADDRESS,
            expected = ResolvedOwner.SavedContact(CONTACT_OF_VIEWED_WALLET, EXTERNAL_ADDRESS),
        ),
        Model(
            name = "address saved only in another wallet -> SavedContact",
            lookup = lookup(contacts = listOf(CONTACT_OF_OTHER_WALLET)),
            address = EXTERNAL_ADDRESS,
            expected = ResolvedOwner.SavedContact(CONTACT_OF_OTHER_WALLET, EXTERNAL_ADDRESS),
        ),
        Model(
            name = "address saved in both wallets -> contact of the viewed wallet",
            lookup = lookup(contacts = listOf(CONTACT_OF_OTHER_WALLET, CONTACT_OF_VIEWED_WALLET)),
            address = EXTERNAL_ADDRESS,
            expected = ResolvedOwner.SavedContact(CONTACT_OF_VIEWED_WALLET, EXTERNAL_ADDRESS),
        ),
        Model(
            name = "incoming from a saved address -> SavedContact",
            lookup = lookup(contacts = listOf(CONTACT_OF_VIEWED_WALLET)),
            address = EXTERNAL_ADDRESS,
            expected = ResolvedOwner.SavedContact(CONTACT_OF_VIEWED_WALLET, EXTERNAL_ADDRESS),
        ),
        Model(
            name = "address in no address book -> External",
            lookup = lookup(contacts = listOf(CONTACT_OF_VIEWED_WALLET)),
            address = UNSAVED_ADDRESS,
            expected = ResolvedOwner.External(UNSAVED_ADDRESS),
        ),
        Model(
            name = "unresolved network -> External",
            lookup = lookup(contacts = listOf(CONTACT_OF_VIEWED_WALLET)),
            address = EXTERNAL_ADDRESS,
            networkRawId = null,
            expected = ResolvedOwner.External(EXTERNAL_ADDRESS),
        ),
        Model(
            name = "address saved on another network -> External",
            lookup = lookup(contacts = listOf(CONTACT_OF_VIEWED_WALLET)),
            address = EXTERNAL_ADDRESS,
            networkRawId = POLYGON,
            expected = ResolvedOwner.External(EXTERNAL_ADDRESS),
        ),
    )

    private fun lookup(
        ownAddresses: Map<String, Account> = emptyMap(),
        contacts: List<Contact> = emptyList(),
    ): TxHistoryLookupContext = TxHistoryLookupContext(
        ownAccountByNetwork = mapOf(ETH to ownAddresses),
        isAccountsModeEnabled = true,
        walletInfoById = mapOf(VIEWED_WALLET_ID to WALLET_INFO),
        contacts = ContactAddressLookup.of(contacts),
        viewedWalletId = VIEWED_WALLET_ID,
    )

    internal data class Model(
        val name: String,
        val lookup: TxHistoryLookupContext,
        val address: String,
        val expected: ResolvedOwner,
        val networkRawId: Network.RawID? = ETH,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val ETH = Network.RawID(value = "ethereum")
        val POLYGON = Network.RawID(value = "polygon")

        val VIEWED_WALLET_ID = MockAccounts.userWalletId
        val OTHER_WALLET_ID = UserWalletId("022")
        val CRYPTO_ACCOUNT = Account.Personal.createMainAccount(VIEWED_WALLET_ID)
        val WALLET_INFO = WalletInfo(
            name = "My Wallet",
            deviceIconUM = DeviceIconUM.Card(mainColor = Color(color = 0xFF1E1E1E), secondColor = null),
        )

        const val OWN_ADDRESS = "0xOwnAddress1234"
        const val EXTERNAL_ADDRESS = "0xExternalAddress5678"
        const val UNSAVED_ADDRESS = "0xUnsavedAddress9012"

        val CONTACT_OF_OWN_ADDRESS = TestContacts.create(
            name = "My other wallet",
            address = OWN_ADDRESS,
            walletId = VIEWED_WALLET_ID,
            networkId = ETH,
        )
        val CONTACT_OF_VIEWED_WALLET = TestContacts.create(
            name = "Binance",
            address = EXTERNAL_ADDRESS,
            walletId = VIEWED_WALLET_ID,
            networkId = ETH,
            createdAt = "2026-01-01T00:00:00.000Z",
        )
        val CONTACT_OF_OTHER_WALLET = TestContacts.create(
            name = "Kraken",
            address = EXTERNAL_ADDRESS,
            walletId = OTHER_WALLET_ID,
            networkId = ETH,
            createdAt = "2026-06-01T00:00:00.000Z",
        )
    }
}