package com.tangem.domain.addressbook.model

import com.tangem.domain.addressbook.model.serialization.ContactNameAsStringSerializer
import com.tangem.domain.addressbook.model.serialization.UserWalletIdAsStringSerializer
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A named address stored in the user's address book for fast access when sending.
 *

 * `2026-06-10T14:30:00.000Z`. The whole contact (including these fields) is encrypted by
 * [com.tangem.domain.addressbook.crypto.AddressBookCipher] and never leaves the device in clear text.
 */
@Serializable
data class Contact(
    @SerialName("id")
    val id: ContactId,
    @SerialName("walletId")
    @Serializable(with = UserWalletIdAsStringSerializer::class)
    val walletId: UserWalletId,
    @SerialName("name")
    @Serializable(with = ContactNameAsStringSerializer::class)
    val name: ContactName,
    @SerialName("icon")
    val icon: String,
    @SerialName("iconColor")
    val iconColor: String,
    @SerialName("createdAt")
    val createdAt: String,
    @SerialName("updatedAt")
    val updatedAt: String,
    @SerialName("addresses")
    val addresses: List<AddressEntry>,
)