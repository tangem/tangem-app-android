package com.tangem.domain.managetokens.model

sealed class ManagedCryptoCurrency {

    abstract val id: ID
    abstract val name: String
    abstract val symbol: String

    data class Custom(
        override val id: ID,
        override val name: String,
        override val symbol: String,
        val contractAddress: String?,
        val network: SourceNetwork,
    ) : ManagedCryptoCurrency()

    data class Token(
        override val id: ID,
        override val name: String,
        override val symbol: String,
        val iconUrl: String,
        val availableNetworks: List<SourceNetwork>,
        val addedIn: Set<SourceNetwork.ID>,
    ) : ManagedCryptoCurrency() {

        val isAdded: Boolean = addedIn.isNotEmpty()
    }

    @JvmInline
    value class ID(val value: String) {

        constructor(
            networkId: SourceNetwork.ID,
            contractAddress: String?,
            derivationPath: String?,
        ) : this(
            value = buildString {
                if (contractAddress.isNullOrBlank()) {
                    append(CUSTOM_COIN_PREFIX)
                } else {
                    append(CUSTOM_TOKEN_PREFIX)
                }

                append(DIVIDER)
                append(networkId.value)

                if (!contractAddress.isNullOrBlank()) {
                    append(DIVIDER)
                    append(contractAddress)
                }

                if (!derivationPath.isNullOrBlank()) {
                    append(DIVIDER)
                    append(derivationPath.hashCode())
                }
            },
        )

        companion object {

            const val CUSTOM_TOKEN_PREFIX = "custom_token"
            const val CUSTOM_COIN_PREFIX = "custom_coin"
            const val DIVIDER = "_"
        }
    }

    data class SourceNetwork(
        val id: ID,
        val name: String,
        val iconUrl: String,
        val type: Type,
    ) {

        @JvmInline
        value class ID(val value: String)

        sealed class Type(val name: String?) {

            open val contractAddress: String? = null

            data object MAIN : Type(name = "MAIN")

            data class ERC20(override val contractAddress: String) : Type(name = "ERC20")

            data class TRC20(override val contractAddress: String) : Type(name = "TRC20")

            data class BEP20(override val contractAddress: String) : Type(name = "BEP20")

            data class BEP2(override val contractAddress: String) : Type(name = "BEP2")

            data class Other(override val contractAddress: String) : Type(name = null)
        }
    }
}