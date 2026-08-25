package com.tangem.core.remote.header

/**
 * Supplies the card-authentication [RequestHeader] (`card_id` + `card_public_key`).
 *
 * The implementation lives in the module that owns the card-auth source, so [ApiConfig][com.tangem.core.remote.config.ApiConfig]s
 * declared in other modules can attach the header by injecting this contract instead of depending on
 * that source directly.
 */
fun interface CardAuthHeaderProvider {

    fun get(): RequestHeader
}