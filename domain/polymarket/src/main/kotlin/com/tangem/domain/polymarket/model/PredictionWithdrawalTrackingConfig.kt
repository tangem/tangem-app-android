package com.tangem.domain.polymarket.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PredictionWithdrawalTrackingConfig(
    val interval: Duration = 5.seconds,
    val maxAttempts: Int = 360,
)