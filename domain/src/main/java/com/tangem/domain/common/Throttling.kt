package com.tangem.domain.common

/**
 * Created by Anton Zhilenkov on 24/03/2022.
 */
open class Throttler<T>(
    private val duration: Long,
) : Throttle<T> {

    private val items: MutableMap<T, Long> = mutableMapOf()

    override fun isStillThrottled(item: T): Boolean {
        val inThrottlingUpTo = items[item] ?: return false
        val diff = System.currentTimeMillis() - inThrottlingUpTo
        return diff < 0
    }

    override fun updateThrottlingTo(item: T): T {
        val now = System.currentTimeMillis()
        val throttledUpTo = items[item] ?: 0L
        if (throttledUpTo == 0L || throttledUpTo < now) {
            val newTime = now + duration
            items[item] = newTime
        }
        return item
    }

    open fun clear() {
        items.clear()
    }
}

class ThrottlerWithValues<T, V>(
    duration: Long,
) : Throttler<T>(duration), ValuesHolder<T, V> {

    private val valuesHolder: MutableMap<T, V?> = mutableMapOf()

    override fun setValue(item: T, value: V) {
        valuesHolder[item] = value
    }

    override fun geValue(item: T): V? = valuesHolder[item]

    override fun remove(item: T) {
        valuesHolder.remove(item)
    }

    override fun clear() {
        valuesHolder.clear()
        super.clear()
    }
}

interface Throttle<T> {
    fun isStillThrottled(item: T): Boolean
    fun updateThrottlingTo(item: T): T
}

interface ValuesHolder<K, V> {
    fun setValue(item: K, value: V)
    fun geValue(item: K): V?
    fun remove(item: K)
}
