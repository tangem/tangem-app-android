package com.tangem.domain.addressbook.model

import com.tangem.domain.addressbook.model.serialization.NetworkRawIdAsStringSerializer
import com.tangem.domain.models.network.Network
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddressEntry(
    @SerialName("id")
    val id: AddressEntryId,
    @SerialName("address")
    val address: String,
    @SerialName("networkId")
    @Serializable(with = NetworkRawIdAsStringSerializer::class)
    val networkId: Network.RawID,
    @SerialName("memo")
    val memo: String? = null,
    @SerialName("signature")
    val signature: String,
)