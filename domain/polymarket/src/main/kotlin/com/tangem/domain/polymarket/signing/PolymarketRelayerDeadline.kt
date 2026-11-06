package com.tangem.domain.polymarket.signing

object PolymarketRelayerDeadline {

    const val WINDOW_SECONDS = 600L

    const val FOLLOW_UP_WINDOW_SECONDS = 1_200L

    private const val MILLIS_IN_SECOND = 1_000L

    /**
     * @return the unix-seconds deadline the relayer accepts for a batch built at [nowMillis]
     */
    fun fromNow(nowMillis: Long = System.currentTimeMillis(), windowSeconds: Long = WINDOW_SECONDS): String =
        (nowMillis / MILLIS_IN_SECOND + windowSeconds).toString()

    /**
     * @return true when [deadline] is not a unix-seconds value or is not strictly later than [nowMillis]
     */
    fun isExpired(deadline: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val deadlineSeconds = deadline.toLongOrNull() ?: return true
        return deadlineSeconds <= nowMillis / MILLIS_IN_SECOND
    }
}