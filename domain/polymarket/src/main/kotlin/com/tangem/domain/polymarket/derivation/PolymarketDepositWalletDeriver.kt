package com.tangem.domain.polymarket.derivation

/**
 * Derives the Polymarket deposit-wallet (DW) address from an owner EOA — CREATE2 (Solady ERC-1967 UUPS,
 * Polygon). The returned address is ERC-55 checksummed.
 */
interface PolymarketDepositWalletDeriver {

    fun deriveDepositWallet(ownerAddress: String): String
}