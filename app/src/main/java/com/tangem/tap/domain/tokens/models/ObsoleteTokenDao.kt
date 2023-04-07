package com.tangem.tap.domain.tokens.models

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain

@JsonClass(generateAdapter = true)
data class ObsoleteTokenDao(
    val name: String,
    val symbol: String,
    val contractAddress: String,
    val decimalCount: Int,
    val customIconUrl: String?,
) {
    fun toTokenDao(blockchain: Blockchain): TokenDao {
        return TokenDao(
            name = name,
            symbol = symbol,
            contractAddress = contractAddress,
            decimalCount = decimalCount,
            blockchainDao = BlockchainDao.fromBlockchain(blockchain),
            customIconUrl = customIconUrl,
        )
    }
}
