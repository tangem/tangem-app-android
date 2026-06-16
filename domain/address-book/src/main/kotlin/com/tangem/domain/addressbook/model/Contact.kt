package com.tangem.domain.addressbook.model

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * A named address stored in the user's address book for fast access when sending.
 *

 * `2026-06-10T14:30:00.000Z`. The whole contact (including these fields) is encrypted by
 * [com.tangem.domain.addressbook.crypto.AddressBookCipher] and never leaves the device in clear text.
 */
@Serializable
data class Contact(
    val id: ContactId,
    val walletId: UserWalletId,
    val name: ContactName,
    val createdAt: String,
    val updatedAt: String,
    val addressEntries: List<AddressEntry>,
)