package com.tangem.tap.domain.tokens.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain

@JsonClass(generateAdapter = true)
data class BlockchainDao(
    @Json(name = "key")
    val name: String,
    @Json(name = "testnet")
    val isTestNet: Boolean
) {
    fun toBlockchain(): Blockchain {
        val blockchain = Blockchain.values().find { it.name.lowercase() == name.lowercase() }
            ?: throw Exception("Invalid BlockchainDao")
        return if (!isTestNet) blockchain else blockchain.getTestnetVersion()
            ?: throw Exception("Invalid BlockchainDao")
    }

    companion object {
        fun fromBlockchain(blockchain: Blockchain): BlockchainDao {
            val name = blockchain.name.removeSuffix("Testnet").lowercase()
            return BlockchainDao(name, blockchain.isTestnet())
        }
    }
}