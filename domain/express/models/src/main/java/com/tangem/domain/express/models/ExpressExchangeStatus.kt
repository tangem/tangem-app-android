package com.tangem.domain.express.models

/**
 * Express exchange (swap) transaction status.
 *
 * [raw] is the exact value received from / persisted by the backend. Responses and the local DB keep the
 * status as a raw string (so a brand-new backend value never breaks parsing); this enum is the typed view
 * used after reading it back.
 */
enum class ExpressExchangeStatus(val raw: String) {
    Preview("preview"),
    Created("created"),
    ExchangeTxSent("exchange-tx-sent"),
    Waiting("waiting"),
    WaitingTxHash("waiting-tx-hash"),
    Confirming("confirming"),
    Exchanging("exchanging"),
    Sending("sending"),
    Finished("finished"),
    Failed("failed"),
    TxFailed("tx-failed"),
    Refunded("refunded"),
    Verifying("verifying"),
    Expired("expired"),
    Paused("paused"),
    Unknown("unknown"),
    ;

    /**
     * Whether the deal has reached a final state where no further status changes are expected.
     * Terminal: [Expired], [Unknown], [Refunded], [Finished], [TxFailed], [Paused]. Note that [Failed]
     * (unlike [TxFailed]) is NOT terminal here.
     */
    val isTerminal: Boolean
        get() = when (this) {
            Expired,
            Unknown,
            Refunded,
            Finished,
            TxFailed,
            Paused,
            -> true
            Preview,
            Created,
            ExchangeTxSent,
            Waiting,
            WaitingTxHash,
            Confirming,
            Exchanging,
            Sending,
            Failed,
            Verifying,
            -> false
        }

    companion object {
        fun fromRaw(raw: String): ExpressExchangeStatus = entries.firstOrNull { it.raw == raw } ?: Unknown
    }
}