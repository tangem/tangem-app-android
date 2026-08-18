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
    const val NEG_RISK_ADAPTER: String = "0xd91E80cF2E7be2e162c6513ceD06f1dD0dA35296"
    const val CTF_COLLATERAL_ADAPTER: String = "0xAdA100Db00Ca00073811820692005400218FcE1f"
    const val NEG_RISK_CTF_COLLATERAL_ADAPTER: String = "0xadA2005600Dec949baf300f4C6120000bDB6eAab"

    /**
     * Exchange and router of Polymarket's v3 contract line. Both are ERC-1967 proxies (implementations
     * `0x7345c6842b244926125ed4054905cac49620b5dc` and `0x6c405da46fdc4172239e5053189b6577e290e62f` as of
     * 2026-08-11) and neither appears in Polymarket's published contract list. They are here because the
     * backend's approvals validator requires them and because Polymarket's own client grants them the
     * collateral allowance in every current batch on chain — not because their role is documented.
     */
    const val EXCHANGE_V3: String = "0xe3333700cA9d93003F00f0F71f8515005F6c00Aa"
    const val ROUTER_V3: String = "0x12121212006e4CD160D18e3f00711DA5c3372600"

    /**
     * Base-unit exponent the CLOB reports [COLLATERAL] amounts in. It describes that API's wire format, not a
     * currency held in a portfolio: [COLLATERAL] is a different contract from the USDC the app can add on
     * Polygon, so a portfolio token's `decimals` is not a substitute for this value.
     */
    const val COLLATERAL_DECIMALS: Int = 6

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