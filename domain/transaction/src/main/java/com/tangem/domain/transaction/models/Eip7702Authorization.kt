package com.tangem.domain.transaction.models

import java.math.BigInteger

/**
 * EIP-7702 authorization for account abstraction.
 * Used for delegating EOA (Externally Owned Account) control to a smart contract.
 *
 * EIP-7702 allows EOAs to temporarily act as smart contract wallets by delegating
 * their authority to a contract address for a specific transaction.
 *
 * @property chainId blockchain network chain ID
 * @property address contract address to delegate authority to (entry point contract)
 * @property nonce authorization nonce to prevent replay attacks
 * @property yParity recovery ID for signature (0 or 1)
 * @property r ECDSA signature component R
 * @property s ECDSA signature component S
 *
 * @see <a href="https://eips.ethereum.org/EIPS/eip-7702">EIP-7702 Specification</a>
 */
data class Eip7702Authorization(
    val chainId: Int,
    val address: String,
    val nonce: BigInteger,
    val yParity: Int,
    val r: String,
    val s: String,
) {
    init {
        require(chainId > 0) { "Chain ID must be positive" }
        require(address.isNotBlank()) { "Address must not be blank" }
        require(yParity in 0..1) { "yParity must be 0 or 1" }
        require(r.isNotBlank()) { "Signature component R must not be blank" }
        require(s.isNotBlank()) { "Signature component S must not be blank" }
    }
}