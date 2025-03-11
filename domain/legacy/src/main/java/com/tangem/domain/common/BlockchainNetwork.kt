package com.tangem.domain.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.extensions.calculateHashCode

@JsonClass(generateAdapter = true)
@Deprecated("Use Network model")
data class BlockchainNetwork(
    @Json(name = "blockchain")
    val blockchain: Blockchain,
    @Json(name = "derivationPath")
    val derivationPath: String?,
    @Json(name = "tokens")
    val tokens: List<Token>,
) {

    constructor(blockchain: Blockchain, derivationStyleProvider: DerivationStyleProvider) : this(
        blockchain = blockchain,
        derivationPath = blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())?.rawPath,
        tokens = emptyList(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockchainNetwork

        if (blockchain != other.blockchain) return false
        return derivationPath == other.derivationPath
    }

    override fun hashCode(): Int = calculateHashCode(
        blockchain.hashCode(),
        derivationPath?.hashCode() ?: 0,
    )
}