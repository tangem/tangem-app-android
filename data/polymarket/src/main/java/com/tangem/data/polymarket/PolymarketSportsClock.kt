package com.tangem.data.polymarket

import java.time.Instant
import javax.inject.Inject

internal class PolymarketSportsClock @Inject constructor() {

    fun now(): Instant = Instant.now()
}