package com.tangem.domain.staking.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toMigratedCoinId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig

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

    /**
     * Represents the network ID associated with the staking integration from provider
     * https://docs.yield.xyz/reference/yieldscontroller_getyields
     */
    val networkId: String

    /**
     * Represents StakeKit staking integrations
     */
    sealed interface StakeKit : StakingIntegrationID {

        /** Represents blockchains whose native coins can be staked via StakeKit */
        enum class Coin : StakeKit {
            Ton {
                override val value: String = "ton-ton-chorus-one-pools-staking"
                override val blockchain: Blockchain = Blockchain.TON
                override val networkId: String = "ton"
            },
            Solana {
                override val value: String = "solana-sol-native-multivalidator-staking"
                override val blockchain: Blockchain = Blockchain.Solana
                override val networkId: String = "solana"
            },
            Cosmos {
                override val value: String = "cosmos-atom-native-staking"
                override val blockchain: Blockchain = Blockchain.Cosmos
                override val networkId: String = "cosmos"
            },
            Tron {
                override val value: String = "tron-trx-native-staking"
                override val blockchain: Blockchain = Blockchain.Tron
                override val networkId: String = "tron"
            },
            BSC {
                override val value: String = "bsc-bnb-native-staking"
                override val blockchain: Blockchain = Blockchain.BSC
                override val networkId: String = "binance"
            },
            Cardano {
                override val value: String = "cardano-ada-native-staking"
                override val blockchain: Blockchain = Blockchain.Cardano
                override val networkId: String = "cardano"
            },
        }

        /**
         * Represents staking integrations for Ethereum-based tokens via StakeKit
         *
         * @property subBlockchain the specific Ethereum-based blockchain associated with the integration
         */
        enum class EthereumToken(val subBlockchain: Blockchain) : StakeKit {
            Polygon(subBlockchain = Blockchain.Polygon) {
                override val value: String = "ethereum-matic-native-staking"
                override val approval: StakingApproval.Needed =
                    StakingApproval.Needed(spenderAddress = "0x5e3Ef299fDDf15eAa0432E6e66473ace8c13D908")
                override val networkId: String = "ethereum"
            },
            ;

            /** Blockchain associated with Ethereum-based staking integrations. */
            override val blockchain: Blockchain = Blockchain.Ethereum
        }

        companion object {
            val entries: List<StakeKit> by lazy {
                Coin.entries + EthereumToken.entries
            }
        }
    }

    /**
     * Represents P2PEthPool staking integration
     */
    object P2PEthPool : StakingIntegrationID {
        override val value: String = "p2p-ethereum-pooled"
        override val blockchain: Blockchain
            get() = if (P2PEthPoolStakingConfig.USE_TESTNET) Blockchain.EthereumTestnet else Blockchain.Ethereum
        override val networkId: String
            get() = P2PEthPoolStakingConfig.activeNetwork.stakingNetworkId
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
        val entries: List<StakingIntegrationID> by lazy {
            StakeKit.Coin.entries + StakeKit.EthereumToken.entries + listOf(P2PEthPool)
        }

        /**
         * Creates a [StakingIntegrationID] for the given cryptocurrency ID
         *
         * @param currencyId the identifier of the cryptocurrency
         *
         * @return a [StakingIntegrationID] if supported, or `null` if not supported.
         */
        fun create(currencyId: CryptoCurrency.ID): StakingIntegrationID? {
            val blockchain = Blockchain.fromId(id = currencyId.rawNetworkId)

            return if (currencyId.contractAddress.isNullOrBlank()) {
                // Order is not important — either P2PEthPool or Stakekit.Coin can be in any order
                if (P2PEthPool.blockchain == blockchain) {
                    P2PEthPool
                } else {
                    StakeKit.Coin.entries.firstOrNull { it.blockchain == blockchain }
                }
            } else {
                StakeKit.EthereumToken.entries.firstOrNull { token ->
                    token.blockchain == blockchain &&
                        token.subBlockchain.toMigratedCoinId() == currencyId.rawCurrencyId?.value
                }
            }
        }
    }
}

val Network.isStakingSupported: Boolean
    get() = this.toBlockchain().isStakingSupported

/**
 * Extension property to check if staking is supported for a blockchain.
 *
 * @return `true` if staking is supported, `false` otherwise.
 */
val Blockchain.isStakingSupported: Boolean
    get() = StakingIntegrationID.entries.any { it.blockchain == this }