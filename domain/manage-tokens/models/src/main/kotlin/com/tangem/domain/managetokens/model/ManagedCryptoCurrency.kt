package com.tangem.domain.managetokens.model

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network

sealed class ManagedCryptoCurrency {

    abstract val id: ID
    abstract val name: String
    abstract val symbol: String
    abstract val iconUrl: String?

    sealed class Custom : ManagedCryptoCurrency() {

        abstract val currencyId: CryptoCurrency.ID
        abstract val network: Network

        override val id: ID
            get() = ID(currencyId.value)

        data class Token(
            override val currencyId: CryptoCurrency.ID,
            override val name: String,
            override val symbol: String,
            override val iconUrl: String?,
            override val network: Network,
            val contractAddress: String,
        ) : Custom()

        data class Coin(
            override val currencyId: CryptoCurrency.ID,
            override val name: String,
            override val symbol: String,
            override val iconUrl: String?,
            override val network: Network,
        ) : Custom()
    }

    data class Token(
        override val id: ID,
        override val name: String,
        override val symbol: String,
        override val iconUrl: String,
        val availableNetworks: List<SourceNetwork>,
        val addedIn: Set<Network.ID>,
    ) : ManagedCryptoCurrency() {

        val isAdded: Boolean = addedIn.isNotEmpty()
    }

    @JvmInline
    value class ID(val value: String)

    sealed class SourceNetwork {

        abstract val network: Network

        val id: Network.ID
            get() = network.id

        val typeName: String
            get() = when (this) {
                is Main -> MAIN_NETWORK_TYPE_NAME
                is Default -> when (network.standardType) {
                    is Network.StandardType.BEP2,
                    is Network.StandardType.BEP20,
                    is Network.StandardType.ERC20,
                    is Network.StandardType.TRC20,
                    -> network.standardType.name
                    is Network.StandardType.Unspecified -> ""
                }
            }

        data class Main(
            override val network: Network,
        ) : SourceNetwork()

        data class Default(
            override val network: Network,
            val contractAddress: String,
        ) : SourceNetwork()

        private companion object {
            const val MAIN_NETWORK_TYPE_NAME = "MAIN"
        }
    }
}