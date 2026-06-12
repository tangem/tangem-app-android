package com.tangem.features.addressbook.editcontact.contract

import com.tangem.domain.models.network.Network

/**
 * A recipient address that has been validated and resolved to a [Network] on the AddAddress screen.
 *
 * This is the in-progress (pre-save) representation accumulated in [EditContactUM]. It is converted to a domain
 * `AddressEntry` only when the contact is persisted, since the entry's id and signature are produced at save time.
 */
data class ValidatedAddress(
    val address: String,
    val network: Network,
)