package com.tangem.lib.crypto.models

data class Token(
    val id: String,
    val name: String,
    val symbol: String,
    val networkId: String,
    val contractAddress: String?,
    val decimalCount: Int?,
)