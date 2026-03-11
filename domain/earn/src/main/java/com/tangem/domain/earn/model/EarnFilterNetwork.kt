package com.tangem.domain.earn.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface EarnFilterNetwork {

    val isSelected: Boolean

    @Serializable
    data class AllNetworks(
        override val isSelected: Boolean,
    ) : EarnFilterNetwork

    @Serializable
    data class MyNetworks(
        override val isSelected: Boolean,
    ) : EarnFilterNetwork

    @Serializable
    data class Specific(
        override val isSelected: Boolean,
        val id: String,
        val symbol: String,
        val fullName: String,
    ) : EarnFilterNetwork
}