package com.tangem.utils.notifications

import java.util.concurrent.atomic.AtomicInteger

object NotificationIdGenerator {

    private val counter = AtomicInteger((System.currentTimeMillis() % Int.MAX_VALUE).toInt())

    fun next(): Int = counter.incrementAndGet()
}