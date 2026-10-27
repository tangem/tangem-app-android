package com.tangem.utils.time

import javax.inject.Inject

/** The wall clock behind an injection point, so that time-dependent state can be driven in tests */
fun interface CurrentTimeProvider {

    fun nowMillis(): Long
}

internal class SystemCurrentTimeProvider @Inject constructor() : CurrentTimeProvider {

    override fun nowMillis(): Long = System.currentTimeMillis()
}