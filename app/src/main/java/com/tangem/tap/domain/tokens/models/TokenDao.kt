package com.tangem.tap.domain.tokens.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Token
import com.tangem.tap.domain.extensions.setCustomIconUrl

@JsonClass(generateAdapter = true)
data class TokenDao(
    val name: String,
    val symbol: String,
    val contractAddress: String,
    val decimalCount: Int,
    @Json(name = "blockchain")
    val blockchainDao: BlockchainDao,
    val customIconUrl: String? = null,
    val type: String? = null
) {
    fun toToken(): Token {
        return Token(
            name = name,
            symbol = symbol,
            contractAddress = contractAddress,
            decimals = decimalCount,
        ).apply {
            customIconUrl?.let { this.setCustomIconUrl(it) }
        }
    }
}