package com.tangem.domain.polymarket.derivation

/**
 * Derives the Polymarket deposit-wallet (DW) address from an owner EOA — CREATE2 (Solady ERC-1967 UUPS,
 * Polygon). The returned address is ERC-55 checksummed.
 */
interface PolymarketDepositWalletDeriver {

    fun deriveDepositWallet(ownerAddress: String): String

    /**
     * The CREATE2 wallet id the deposit wallet is derived from — the owner address left-padded to 32 bytes,
     * as a `0x`-prefixed lowercase hex string. This is the value the BFF expects in `POST /wallet/deploy`,
     * where it re-derives the deposit wallet from it and cross-checks the address we sent.
     */
    fun deriveWalletId(ownerAddress: String): String
}