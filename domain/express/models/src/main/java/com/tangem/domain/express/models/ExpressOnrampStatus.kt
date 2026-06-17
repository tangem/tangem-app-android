package com.tangem.domain.express.models

/**
 * Express onramp transaction status.
 *
 * [raw] is the exact value received from / persisted by the backend. Responses and the local DB keep the
 * status as a raw string (so a brand-new backend value never breaks parsing); this enum is the typed view
 * used after reading it back.
 */
enum class ExpressOnrampStatus(val raw: String) {
    Created("created"),
    Expired("expired"),
    WaitingForPayment("waiting-for-payment"),
    PaymentProcessing("payment-processing"),
    Verifying("verifying"),
    Failed("failed"),
    Paid("paid"),
    Sending("sending"),
    Finished("finished"),
    Paused("paused"),

    /**
     * Client-side fallback for an unrecognized status. The backend does NOT currently send such a value —
     * it is used by [fromRaw] when the raw string matches none of the known statuses.
     */
    Unknown("unknown"),
    ;

    /**
     * Whether the deal has reached a final state where no further status changes are expected.
     * Terminal: [Expired], [Failed], [Finished], [Paused] (and the client fallback [Unknown]).
     */
    val isTerminal: Boolean
        get() = when (this) {
            Expired,
            Failed,
            Finished,
            Paused,
            Unknown,
            -> true
            Created,
            WaitingForPayment,
            PaymentProcessing,
            Verifying,
            Paid,
            Sending,
            -> false
        }

    companion object {
        fun fromRaw(raw: String): ExpressOnrampStatus = entries.firstOrNull { it.raw == raw } ?: Unknown
    }
}