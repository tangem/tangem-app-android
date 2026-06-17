package com.tangem.domain.addressbook.model

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * All [Contact]s of a single wallet. This is the plaintext payload that
 * [com.tangem.domain.addressbook.crypto.AddressBookCipher] encrypts into an
 * [AddressBookBlob] and reconstructs on decryption.
 */
@Serializable
data class AddressBook(
    val walletId: UserWalletId,
    val contacts: List<Contact>,
)