package com.tangem.lib.crypto.models

/**
 * Currency data class that can be native blockchain Token
 * or custom Token (used in this lib to replace and divide logic Currency from app module)
 */
abstract class Currency(
    open val id: String,
    open val name: String,
    open val symbol: String,
    open val networkId: String,
)
