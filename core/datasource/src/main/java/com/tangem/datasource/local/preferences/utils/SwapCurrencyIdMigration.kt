package com.tangem.datasource.local.preferences.utils

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.tangem.datasource.local.preferences.PreferencesKeys

/**
 * Migrates cached CryptoCurrency.ID strings from old blockchain.id format to new networkId format.
 *
 * After refactoring, Network.rawId stores backendId values (e.g. "ethereum") instead of
 * blockchain.id values (e.g. "ETH"). CryptoCurrency.ID body contains this value, so cached IDs
 * like "coin⟨ETH⟩ethereum" must become "coin⟨ethereum⟩ethereum".
 *
 * Affected DataStore keys: [PreferencesKeys.SWAP_TRANSACTIONS_KEY],
 * [PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY].
 */
internal class SwapCurrencyIdMigration : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        return currentData.contains(PreferencesKeys.SWAP_TRANSACTIONS_KEY) ||
            currentData.contains(PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY)
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val mutablePrefs = currentData.toMutablePreferences()

        currentData[PreferencesKeys.SWAP_TRANSACTIONS_KEY]?.let { json ->
            mutablePrefs[PreferencesKeys.SWAP_TRANSACTIONS_KEY] = migrateJson(json)
        }

        currentData[PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY]?.let { json ->
            mutablePrefs[PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY] = migrateJson(json)
        }

        return mutablePrefs.toPreferences()
    }

    override suspend fun cleanUp() {
        // nothing to clean up
    }

    /**
     * Replaces old blockchain.id values with networkId values inside CryptoCurrency.ID strings
     * found anywhere in the JSON. Works by finding all `⟨oldId⟩` and `⟨oldId→` patterns and
     * replacing the old ID with the new one.
     */
    private fun migrateJson(json: String): String {
        var result = json

        for ((oldId, newId) in BLOCKCHAIN_ID_TO_NETWORK_ID) {
            // Body without derivation path: ⟨oldId⟩ → ⟨newId⟩
            result = result.replace("$BODY_START$oldId$BODY_END", "$BODY_START$newId$BODY_END")
            // Body with derivation path: ⟨oldId→ → ⟨newId→
            result = result.replace(
                "$BODY_START$oldId$DERIVATION_DELIMITER",
                "$BODY_START$newId$DERIVATION_DELIMITER",
            )
        }

        return result
    }

    private companion object {
        const val BODY_START = '\u27E8' // ⟨
        const val BODY_END = '\u27E9' // ⟩
        const val DERIVATION_DELIMITER = '\u2192' // →

        /** Mapping of old blockchain.id → new networkId (only entries where values differ). */
        val BLOCKCHAIN_ID_TO_NETWORK_ID = mapOf(
            "ARBITRUM-ONE" to "arbitrum-one",
            "ARBITRUM/test" to "arbitrum-one/test",
            "AVALANCHE" to "avalanche",
            "AVALANCHE/test" to "avalanche/test",
            "BINANCE" to "binancecoin",
            "BINANCE/test" to "binancecoin/test",
            "BSC" to "binance-smart-chain",
            "BSC/test" to "binance-smart-chain/test",
            "BTC" to "bitcoin",
            "BTC/test" to "bitcoin/test",
            "BCH" to "bitcoin-cash",
            "BCH/test" to "bitcoin-cash/test",
            "CARDANO-S" to "cardano",
            "DOGE" to "dogecoin",
            "DUC" to "ducatus",
            "ETH" to "ethereum",
            "ETH/test" to "ethereum/test",
            "ETC" to "ethereum-classic",
            "ETC/test" to "ethereum-classic/test",
            "ETH-Pow" to "ethereum-pow-iou",
            "ETH-Pow/test" to "ethereum-pow-iou/test",
            "FTM" to "fantom",
            "FTM/test" to "fantom/test",
            "GNO" to "xdai",
            "KAS" to "kaspa",
            "KAS/test" to "kaspa/test",
            "KAVA" to "kava",
            "KAVA/test" to "kava/test",
            "Kusama" to "kusama",
            "LTC" to "litecoin",
            "NEAR" to "near-protocol",
            "NEAR/test" to "near-protocol/test",
            "NEXA" to "nexa",
            "NEXA/test" to "nexa/test",
            "OPTIMISM" to "optimistic-ethereum",
            "Polkadot" to "polkadot",
            "POLYGON" to "polygon-pos",
            "POLYGON/test" to "polygon-pos/test",
            "RSK" to "rootstock",
            "SOLANA" to "solana",
            "SOLANA/test" to "solana/test",
            "TELOS" to "telos",
            "TELOS/test" to "telos/test",
            "The-Open-Network" to "the-open-network",
            "The-Open-Network/test" to "the-open-network/test",
            "TRON" to "tron",
            "TRON/test" to "tron/test",
            "XLM" to "stellar",
            "XLM/test" to "stellar/test",
            "XRP" to "xrp",
            "XTZ" to "tezos",
            "DASH" to "dash",
            "xdc" to "xdc-network",
            "xdc/test" to "xdc-network/test",
            "hedera" to "hedera-hashgraph",
            "hedera/test" to "hedera-hashgraph/test",
            "areon" to "areon-network",
            "areon/test" to "areon-network/test",
            "pls" to "pulsechain",
            "pls/test" to "pulsechain/test",
            "zkSyncEra" to "zksync",
            "zkSyncEra/test" to "zksync/test",
            "polygonZkEVM" to "polygon-zkevm",
            "polygonZkEVM/test" to "polygon-zkevm/test",
            "flare" to "flare-network",
            "flare/test" to "flare-network/test",
            "playa3ull" to "playa3ull-games",
            "sei" to "sei-network",
            "sei/test" to "sei-network/test",
            "casper" to "casper-network",
            "casper/test" to "casper-network/test",
            "odyssey" to "dione",
            "odyssey/test" to "dione/test",
            "hyperliquid" to "hyperevm",
            "hyperliquid/test" to "hyperevm/test",
            "quai" to "quai-network",
            "quai/test" to "quai-network/test",
            "manta/test" to "manta-pacific/test",
            "dischain" to "ethereumfair",
        )
    }
}