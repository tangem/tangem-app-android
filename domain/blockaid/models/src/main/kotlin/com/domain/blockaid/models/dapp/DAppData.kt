package com.domain.blockaid.models.dapp

/**
 * Data BlockAid needs to verify DApp domain
 *
 * @property url DApp's domain url
 */
@JvmInline
value class DAppData(
    val url: String,
)