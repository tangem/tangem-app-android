package com.tangem.domain.polymarket.signing

object PolymarketRelayerDeadline {

    private const val MILLIS_IN_SECOND = 1_000L
    private const val WINDOW_SECONDS = 600L

    /**
     * @return the unix-seconds deadline the relayer accepts for a batch built at [nowMillis]
     */
    fun fromNow(nowMillis: Long = System.currentTimeMillis()): String =
        (nowMillis / MILLIS_IN_SECOND + WINDOW_SECONDS).toString()
}