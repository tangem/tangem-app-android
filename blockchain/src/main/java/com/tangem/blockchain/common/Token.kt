package com.tangem.blockchain.common

data class Token(
        val symbol: String,
        val contractAddress: String,
        val decimals: Int
)