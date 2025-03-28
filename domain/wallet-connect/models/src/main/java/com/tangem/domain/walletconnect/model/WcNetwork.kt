package com.tangem.domain.walletconnect.model

import com.tangem.domain.tokens.model.Network

sealed interface WcNetwork {

    val name: String

    data class Supported(val network: Network) : WcNetwork {
        override val name: String get() = network.name
    }

    data class Unknown(override val name: String) : WcNetwork
}