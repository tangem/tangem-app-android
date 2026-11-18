package com.tangem.domain.polymarket

/**
 * The backend's clock. Crypto windows open and close by it, so countdowns and the order cut-off read the time here,
 * not from the device, whose clock can be set to anything.
 */
fun interface PolymarketServerClock {

    /** Backend time in epoch milliseconds; the device's own time until the first backend answer has been seen. */
    fun nowMillis(): Long
}