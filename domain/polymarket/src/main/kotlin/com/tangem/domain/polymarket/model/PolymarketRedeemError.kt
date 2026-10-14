package com.tangem.domain.polymarket.model

/**
 * Typed error surface of the three redeem endpoints. A `422` records nothing on the backend, so the market
 * is never blocked by a rejected submission — the recovery is to refresh, re-fetch the nonce and re-sign.
 */
sealed interface PolymarketRedeemError : PolymarketError {

    data class InvalidRequest(val detail: String?) : PolymarketRedeemError

    data object Unauthorized : PolymarketRedeemError

    /** `404` — the id is unknown **or belongs to another wallet**; stop polling, never retry. */
    data object NotFound : PolymarketRedeemError

    /** `409` on the call lookup — the position holds nothing to redeem, or its market's version is unsupported. */
    data class NothingToRedeem(val detail: String?) : PolymarketRedeemError

    /** `422` — the relayer refused: not claimable, bad signature, expired deadline or a reused nonce. */
    data class RelayerRejected(val detail: String?) : PolymarketRedeemError

    data object RelayerUnavailable : PolymarketRedeemError

    data object Network : PolymarketRedeemError

    data class Unexpected(val httpCode: Int?, val detail: String?) : PolymarketRedeemError
}