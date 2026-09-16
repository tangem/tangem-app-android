package com.tangem.domain.polymarket.signing

object PolymarketRelayerDeadline {

    private const val MILLIS_IN_SECOND = 1_000L
    private const val WINDOW_SECONDS = 600L

    /**
     * @return the unix-seconds deadline the relayer accepts for a batch built at [nowMillis]
     */
    fun fromNow(nowMillis: Long = System.currentTimeMillis()): String =
        (nowMillis / MILLIS_IN_SECOND + WINDOW_SECONDS).toString()

    /**
     * @return true when [deadline] is not a unix-seconds value or is not strictly later than [nowMillis]
     */
    fun isExpired(deadline: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val deadlineSeconds = deadline.toLongOrNull() ?: return true
        return deadlineSeconds <= nowMillis / MILLIS_IN_SECOND
    }
}