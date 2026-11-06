package com.tangem.features.yield.supply.impl.common

import kotlinx.coroutines.Job

/**
 * Orders overlapping fee loads. A load cancels the one in flight only when it takes over, right before it writes
 * the fee: a load that returns early leaves the previous one to finish instead of leaving the fee loading, and a
 * load started earlier never writes after a later one has taken over.
 */
internal class YieldSupplyFeeLoads {

    private var lastStartedLoad = 0
    private var currentLoad: CurrentLoad? = null

    fun start(): Int = ++lastStartedLoad

    fun takeOver(load: Int, job: Job): Boolean {
        val current = currentLoad
        if (current != null && current.load > load) return false

        current?.job?.cancel()
        currentLoad = CurrentLoad(load, job)
        return true
    }

    /** True once a later load has taken over: whatever this load read is stale and must not be written. */
    fun isSuperseded(load: Int): Boolean {
        val current = currentLoad ?: return false
        return current.load > load
    }

    private data class CurrentLoad(val load: Int, val job: Job)
}