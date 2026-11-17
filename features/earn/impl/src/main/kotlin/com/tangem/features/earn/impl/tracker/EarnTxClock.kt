package com.tangem.features.earn.impl.tracker

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

internal class EarnTxClock @Inject constructor() {

    fun now(): Instant = Clock.System.now()
}