package com.tangem.tap.domain.tokens.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain

@Deprecated("The class is used only for migration from older versions of the app")
@JsonClass(generateAdapter = true)
data class BlockchainDao(
    @Json(name = "key")
    val name: String,
    @Json(name = "testnet")
    val isTestNet: Boolean,
) {
    @Deprecated("The method is used only for migration from older versions of the app")
    fun toBlockchain(): Blockchain {
        val blockchain = Blockchain.values().find { it.name.lowercase() == name.lowercase() }
            ?: error("Blockchain is null")
        return if (!isTestNet) blockchain else blockchain.getTestnetVersion() ?: error("Invalid BlockchainDao")
    }

    companion object {
        @Deprecated("The method is used only for migration from older versions of the app")
        fun fromBlockchain(blockchain: Blockchain): BlockchainDao {
            val name = blockchain.name.removeSuffix("Testnet").lowercase()
            return BlockchainDao(name, blockchain.isTestnet())
        }
    }
}
