package com.tangem.lib.crypto.models

/**
 * Currency data class that can be native blockchain Token
 * or custom Token (used in this lib to replace and divide logic Currency from app module)
 */
sealed class Currency(
    open val id: String,
    open val name: String,
    open val symbol: String,
    open val networkId: String,
)

data class NativeToken(
    override val id: String,
    override val name: String,
    override val symbol: String,
    override val networkId: String,
) : Currency(id, name, symbol, networkId)

class NonNativeToken(
    override val id: String,
    override val name: String,
    override val symbol: String,
    override val networkId: String,
    val contractAddress: String,
    val decimalCount: Int,
) : Currency(id, name, symbol, networkId)
