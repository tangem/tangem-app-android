package com.tangem.domain.polymarket.model

/**
 * Onboarding state of a Polymarket deposit wallet, as reported by the BFF `GET /wallet`.
 *
 * @property depositWalletAddress the owner's deposit-wallet address as stored by the BFF, or `null`
 *  when there is no wallet record yet (never deployed via us).
 * @property status current onboarding [PolymarketWalletStatus].
 */
data class PolymarketWalletState(
    val depositWalletAddress: String?,
    val status: PolymarketWalletStatus,
)

/**
 * The deposit-wallet onboarding status machine (BFF contract).
 *
 * [UNKNOWN] is a defensive, forward-compatible value for any status the BFF may add later: callers must
 * treat it as "not ready, keep polling" rather than a breaking change.
 */
enum class PolymarketWalletStatus {
    NOT_CREATED,
    DEPLOYMENT_IN_PROGRESS,
    DEPLOYMENT_FAILED,
    DEPLOYED,
    APPROVALS_IN_PROGRESS,
    APPROVALS_FAILED,
    READY_TO_TRADE,
    UNKNOWN,
    ;

    companion object {

        fun fromRaw(raw: String): PolymarketWalletStatus = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}