package com.tangem.lib.crypto.models

data class NativeToken(
    override val id: String,
    override val name: String,
    override val symbol: String,
    override val networkId: String,
) : Currency(id, name, symbol, networkId)