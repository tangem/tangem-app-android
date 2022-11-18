package com.tangem.lib.crypto.models

class NonNativeToken(
    override val id: String,
    override val name: String,
    override val symbol: String,
    override val networkId: String,
    val contractAddress: String,
    val decimalCount: Int,
) : Currency(id, name, symbol, networkId)
