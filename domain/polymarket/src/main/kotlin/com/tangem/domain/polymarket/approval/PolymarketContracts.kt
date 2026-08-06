package com.tangem.domain.polymarket.approval

/**
 * Polygon (chainId 137) Polymarket contract addresses and deposit-wallet CREATE2 constants — single
 * source of truth. Address constants are stored as published by Polymarket; their checksum casing is not
 * significant to consumers (hex parsing and the calldata encoder are both case-insensitive).
 * [UUPS_INIT_CONST1] / [UUPS_INIT_CONST2] are raw init-code bytes used verbatim in the deposit-wallet derivation.
 */
object PolymarketContracts {

    const val CHAIN_ID: Long = 137

    const val COLLATERAL: String = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB"
    const val CONDITIONAL_TOKENS: String = "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"
    const val CTF_EXCHANGE: String = "0xE111180000d2663C0091e4f400237545B87B996B"
    const val NEG_RISK_CTF_EXCHANGE: String = "0xe2222d279d744050d28e00520010520000310F59"
    const val NEG_RISK_ADAPTER: String = "0xd91E80cF2E7be2e162c6513ceD06f1dD0dA35296"

    const val DW_FACTORY: String = "0x00000000000Fb5C9ADea0298D729A0CB3823Cc07"
    const val DW_IMPLEMENTATION: String = "0x58CA52ebe0DadfdF531Cde7062e76746de4Db1eB"
    const val UUPS_INIT_CONST1: String = "0xcc3735a920a3ca505d382bbc545af43d6000803e6038573d6000fd5b3d6000f3"
    const val UUPS_INIT_CONST2: String = "0x5155f3363d3d373d3d363d7f360894a13ba1a3210667c828492db98dca3e2076"
}