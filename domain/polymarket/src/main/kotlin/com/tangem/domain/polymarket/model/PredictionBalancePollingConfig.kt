package com.tangem.domain.polymarket.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * @property baseInterval pause between balance reads while a Polymarket screen is in the foreground
 * @property fundingInterval pause between balance reads while a funding window is open
 * @property fundingWindow how long a funding window stays open once a Polymarket screen starts watching it
 * @property fundingWindowMaxAge how long after the top-up was sent a funding window may still be opened at all
 */
data class PredictionBalancePollingConfig(
    val baseInterval: Duration = 30.seconds,
    val fundingInterval: Duration = 5.seconds,
    val fundingWindow: Duration = 2.minutes,
    val fundingWindowMaxAge: Duration = 10.minutes,
)