package com.tangem.data.collectibles.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DismissedTxsDTO(
    @SerialName("txIds")
    val txIds: List<String> = emptyList(),
)