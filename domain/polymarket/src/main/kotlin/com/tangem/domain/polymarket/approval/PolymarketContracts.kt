package com.tangem.domain.polymarket.approval

/**
 * Polygon (chainId 137) contract addresses used by Polymarket onboarding builders (kb/04, kb/07).
 * Single source of truth — addresses are ERC-55 checksummed; calldata lowercases them.
 */
object PolymarketContracts {

    const val CHAIN_ID: Long = 137

    const val COLLATERAL: String = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB"
    const val CONDITIONAL_TOKENS: String = "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"
    const val CTF_EXCHANGE: String = "0xE111180000d2663C0091e4f400237545B87B996B"
    const val NEG_RISK_CTF_EXCHANGE: String = "0xe2222d279d744050d28e00520010520000310F59"
    const val NEG_RISK_ADAPTER: String = "0xd91E80cF2E7be2e162c6513ceD06f1dD0dA35296"
}