package com.tangem.domain.staking.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toMigratedCoinId
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Represents a staking integration identifier.
 *
 * Each implementation of this interface provides details about a specific staking integration,
 * including its unique value, associated blockchain, and approval requirements.
 *
[REDACTED_AUTHOR]
 */
sealed interface StakingIntegrationID {

    /** Unique identifier for the staking integration */
    val value: String

    /** Blockchain associated with the staking integration */
    val blockchain: Blockchain

    /** Approval requirements for the staking integration. Defaults to no approval needed */
    val approval: StakingApproval get() = StakingApproval.Empty

    /** Represents blockchains whose native coins can be staked */
    enum class Coin : StakingIntegrationID {
        Ton {
            override val value: String = "ton-ton-chorus-one-pools-staking"
            override val blockchain: Blockchain = Blockchain.TON
        },
        Solana {
            override val value: String = "solana-sol-native-multivalidator-staking"
            override val blockchain: Blockchain = Blockchain.Solana
        },
        Cosmos {
            override val value: String = "cosmos-atom-native-staking"
            override val blockchain: Blockchain = Blockchain.Cosmos
        },
        Tron {
            override val value: String = "tron-trx-native-staking"
            override val blockchain: Blockchain = Blockchain.Tron
        },
        BSC {
            override val value: String = "bsc-bnb-native-staking"
            override val blockchain: Blockchain = Blockchain.BSC
        },
        Cardano {
            override val value: String = "cardano-ada-native-staking"
            override val blockchain: Blockchain = Blockchain.Cardano
        },
    }

    /**
     * Represents staking integrations for Ethereum-based tokens
     *
     * @property subBlockchain the specific Ethereum-based blockchain associated with the integration
     */
    enum class EthereumToken(val subBlockchain: Blockchain) : StakingIntegrationID {
        Polygon(subBlockchain = Blockchain.Polygon) {
            override val value: String = "ethereum-matic-native-staking"
            override val approval: StakingApproval.Needed =
                StakingApproval.Needed(spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908")
        },
        ;

        /** Blockchain associated with Ethereum-based staking integrations. */
        override val blockchain: Blockchain = Blockchain.Ethereum
    }

    // Polkadot {
    //     override val value: String = "polkadot-dot-validator-staking"
    //     override val blockchain: Blockchain = Blockchain.Polkadot
    // },
    // Avalanche {
    //     override val value: String = "avalanche-avax-native-staking"
    //     override val blockchain: Blockchain = Blockchain.Avalanche
    // },
    // Cronos {
    //     override val value: String = "cronos-cro-native-staking"
    //     override val blockchain: Blockchain = Blockchain.Cronos
    // },
    // Kava {
    //     override val value: String = "kava-kava-native-staking"
    //     override val blockchain: Blockchain = Blockchain.Kava
    // },
    // Near {
    //     override val value: String = "near-near-native-staking"
    //     override val blockchain: Blockchain = Blockchain.Near
    // },
    // Tezos {
    //     override val value: String = "tezos-xtz-native-staking"
    //     override val blockchain: Blockchain = Blockchain.Tezos
    // },

    companion object {

        /** List of all native staking integration IDs */
        val entries: List<StakingIntegrationID> by lazy { Coin.entries + EthereumToken.entries }

        /**
         * Creates a [StakingIntegrationID] for the given cryptocurrency ID
         *
         * @param currencyId the identifier of the cryptocurrency
         *
         * @return a [StakingIntegrationID] if supported, or `null` if not supported.
         */
        fun create(currencyId: CryptoCurrency.ID): StakingIntegrationID? {
            val blockchain = Blockchain.fromId(id = currencyId.rawNetworkId)

            val integrationId = blockchain.integrationId ?: return null

            return when (integrationId) {
                is Coin -> integrationId
                is EthereumToken -> {
                    val coinId = integrationId.subBlockchain.toMigratedCoinId()

                    if (coinId == currencyId.rawCurrencyId?.value) {
                        integrationId
                    } else {
                        null
                    }
                }
            }
        }
    }
}

/**
 * Extension property to check if staking is supported for a blockchain.
 *
 * @return `true` if staking is supported, `false` otherwise.
 */
val Blockchain.isStakingSupported: Boolean
    get() = StakingIntegrationID.entries.any { it.blockchain == this }

/**
 * Extension property to retrieve the staking integration ID for a blockchain.
 *
 * @return the [StakingIntegrationID] if available, or `null` if not supported.
 */
val Blockchain.integrationId: StakingIntegrationID?
    get() = StakingIntegrationID.entries.firstOrNull { it.blockchain == this }