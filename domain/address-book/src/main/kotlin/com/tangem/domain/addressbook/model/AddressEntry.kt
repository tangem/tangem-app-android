package com.tangem.domain.addressbook.model

import com.tangem.domain.addressbook.model.serialization.NetworkRawIdAsStringSerializer
import com.tangem.domain.models.network.Network
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A single saved address belonging to a [Contact]. */
@Serializable
data class AddressEntry(
    @SerialName("id")
    val id: AddressEntryId,
    @SerialName("address")
    val address: String,
    @SerialName("networkId")
    @Serializable(with = NetworkRawIdAsStringSerializer::class)
    val networkId: Network.RawID,
    @SerialName("networkName")
    val networkName: String,
    @SerialName("memo")
    val memo: String?,
    @SerialName("signature")
    val signature: String,
)