package com.tangem.domain.addressbook.model

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/** A named address stored in the user's address book for fast access when sending. */
@Serializable
data class Contact(
    val id: ContactId,
    val walletId: UserWalletId,
    val name: ContactName,
    val addressEntries: List<AddressEntry>,
)