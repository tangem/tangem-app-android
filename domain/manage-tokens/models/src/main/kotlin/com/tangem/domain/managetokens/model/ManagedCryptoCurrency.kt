package com.tangem.domain.managetokens.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import kotlinx.serialization.Serializable

@Serializable
sealed class ManagedCryptoCurrency {

    abstract val id: ID
    abstract val name: String
    abstract val symbol: String
    abstract val iconUrl: String?

    @Serializable
    sealed class Custom : ManagedCryptoCurrency() {

        abstract val currencyId: CryptoCurrency.ID
        abstract val network: Network

        override val id: ID
            get() = ID(currencyId.value)

        @Serializable
        data class Token(
            override val currencyId: CryptoCurrency.ID,
            override val name: String,
            override val symbol: String,
            override val iconUrl: String?,
            override val network: Network,
            val contractAddress: String,
            val decimals: Int,
        ) : Custom()

        @Serializable
        data class Coin(
            override val currencyId: CryptoCurrency.ID,
            override val name: String,
            override val symbol: String,
            override val iconUrl: String?,
            override val network: Network,
        ) : Custom()
    }

    @Serializable
    data class Token(
        override val id: ID,
        override val name: String,
        override val symbol: String,
        override val iconUrl: String,
        val availableNetworks: List<SourceNetwork>,
        val addedIn: Set<Network>,
    ) : ManagedCryptoCurrency() {

        val isAdded: Boolean = addedIn.isNotEmpty()
    }

    @JvmInline
    @Serializable
    value class ID(val value: String)

    @Serializable
    sealed class SourceNetwork {

        abstract val network: Network
        abstract val decimals: Int

        val id: Network.ID
            get() = network.id

        val typeName: String
            get() = when (this) {
                is Main -> if (isL2Network) {
                    MAIN_NETWORK_L2_TYPE_NAME
                } else {
                    MAIN_NETWORK_TYPE_NAME
                }
                is Default -> when (network.standardType) {
                    is Network.StandardType.BEP2,
                    is Network.StandardType.BEP20,
                    is Network.StandardType.ERC20,
                    is Network.StandardType.TRC20,
                    -> network.standardType.name
                    is Network.StandardType.Unspecified -> ""
                }
            }

        @Serializable
        data class Main(
            override val network: Network,
            override val decimals: Int,
            val isL2Network: Boolean,
        ) : SourceNetwork()

        @Serializable
        data class Default(
            override val network: Network,
            override val decimals: Int,
            val contractAddress: String,
        ) : SourceNetwork()

        private companion object {
            const val MAIN_NETWORK_TYPE_NAME = "MAIN"
            const val MAIN_NETWORK_L2_TYPE_NAME = "MAIN L2"
        }
    }
}