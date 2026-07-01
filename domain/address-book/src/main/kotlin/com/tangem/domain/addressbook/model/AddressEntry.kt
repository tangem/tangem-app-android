package com.tangem.domain.addressbook.model

import com.tangem.domain.models.network.Network
import kotlinx.serialization.Serializable

/** A single saved address belonging to a [Contact]. */
@Serializable
data class AddressEntry(
    val id: AddressEntryId,
    val address: String,
    val networkId: Network.RawID,
    val memo: String?,
    val signature: String,
)