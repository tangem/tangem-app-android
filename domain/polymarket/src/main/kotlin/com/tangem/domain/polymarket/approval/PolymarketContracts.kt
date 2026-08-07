package com.tangem.domain.polymarket.approval

/**
 * Polygon (chainId 137) Polymarket contract addresses and deposit-wallet CREATE2 constants — single
 * source of truth. Address constants are stored as published by Polymarket; their checksum casing is not
 * significant to consumers (hex parsing and the calldata encoder are both case-insensitive).
 *
 * The deposit-wallet factory deploys **beacon** proxies: it answers the beacon selector `0x49493a4d` with
 * [DW_BEACON], and every deployment it has made carries the ERC-1967 beacon layout. The onboarding spec's
 * UUPS recipe describes a variant the factory no longer emits, so deriving with it yields an address the
 * backend rejects. [DW_BEACON_INIT_PREFIX] / [DW_BEACON_INIT_SUFFIX] are raw init-code bytes used verbatim
 * in the derivation, pinned by known-answer vectors taken from real on-chain deployments.
 */
object PolymarketContracts {

    const val CHAIN_ID: Long = 137

    const val COLLATERAL: String = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB"
    const val CONDITIONAL_TOKENS: String = "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"
    const val CTF_EXCHANGE: String = "0xE111180000d2663C0091e4f400237545B87B996B"
    const val NEG_RISK_CTF_EXCHANGE: String = "0xe2222d279d744050d28e00520010520000310F59"
    const val NEG_RISK_CTF_COLLATERAL_ADAPTER: String = "0xadA2005600Dec949baf300f4C6120000bDB6eAab"

    const val DW_FACTORY: String = "0x00000000000Fb5C9ADea0298D729A0CB3823Cc07"
    const val DW_BEACON: String = "0x7A18EDfe055488A3128f01F563e5B479D92ffc3a"

    /** Creation logic; the third byte carries the 64-byte `args` length, invariant here. */
    const val DW_BEACON_INIT_PREFIX: String = "0x6100923d8160233d3973"

    /** Beacon-slot store plus the 82-byte proxy runtime that the creation logic returns. */
    const val DW_BEACON_INIT_SUFFIX: String = "0x60195155f3" +
        "363d3d373d3d363d602036600436635c60da1b60e01b36527f" +
        "a3f0ad74e5423aebfd80d3ef4346578335a9a72aeaee59ff6cb3582b35133d50" +
        "545afa5036515af43d6000803e604d573d6000fd5b3d6000f3"
}