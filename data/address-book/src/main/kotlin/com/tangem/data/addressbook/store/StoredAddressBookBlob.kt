package com.tangem.data.addressbook.store

import com.tangem.domain.addressbook.model.AddressBookBlob
import kotlinx.serialization.Serializable

/**
 * [isBESynchronized] tracks whether the blob has already been pushed to the backend. A freshly
 * stored blob is written optimistically with `false`; a future BE-sync service flips it to `true`
 * once the push is confirmed.
 */
@Serializable
internal data class StoredAddressBookBlob(
    val blob: AddressBookBlob,
    val isBESynchronized: Boolean,
)