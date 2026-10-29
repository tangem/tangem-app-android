package com.tangem.domain.transaction.error

sealed interface SolanaGaslessError {
    data object Unauthorized : SolanaGaslessError // 401/403, api key rejected — gasless unavailable
    data object Disabled : SolanaGaslessError // 400 UNSUPPORTED_CHAIN
    data object BlockhashExpired : SolanaGaslessError // 410 — rebuild and re-sign
    data class RateLimited(val isPerWallet: Boolean) : SolanaGaslessError // 429
    data class Rejected(val code: String?) : SolanaGaslessError // 413/422: tx cannot be sponsored as built
    // 503/500 without "maybe sent"; isSponsorshipUnavailable: the sponsor cannot pay, so a retry fails the same way
    data class ServiceUnavailable(val code: String?, val isSponsorshipUnavailable: Boolean = false) : SolanaGaslessError
    data class MaybeSent(val signature: String?) : SolanaGaslessError // UNCERTAIN / timeout — never rebuild
    data class Unknown(val message: String?) : SolanaGaslessError
}