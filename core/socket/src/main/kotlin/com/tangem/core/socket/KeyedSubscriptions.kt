package com.tangem.core.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * @param newService builds the service of one shard; `openKeys` is meant for [SocketSpec.onOpen]: it returns the
 * shard's full key set and marks it as subscribed, so a reconnect restores every subscription in one frame
 * @param subscribe sends a subscribe frame for keys added since the last frame; false keeps them pending
 * @param unsubscribe sends an unsubscribe frame for keys released longer than [unsubscribeDelay] ago
 * @param maxKeysPerService the server's per-connection limit; a key beyond it goes to a new shard
 * @param batchWindow changes within this window go out as one frame
 * @param unsubscribeDelay keeps a released key subscribed for a while, so scrolling back does not resubscribe
 */
class KeyedSubscriptions<Key : Any, Service : Any>(
    private val scope: CoroutineScope,
    private val newService: (shard: Int, openKeys: () -> Set<Key>) -> Service,
    private val subscribe: Service.(keys: Set<Key>) -> Boolean,
    private val unsubscribe: Service.(keys: Set<Key>) -> Boolean,
    private val maxKeysPerService: Int = Int.MAX_VALUE,
    private val batchWindow: Duration = 100.milliseconds,
    private val unsubscribeDelay: Duration = 2.seconds,
) {

    init {
        require(maxKeysPerService > 0) { "maxKeysPerService must be positive" }
        require(!batchWindow.isNegative()) { "batchWindow must not be negative" }
        require(!unsubscribeDelay.isNegative()) { "unsubscribeDelay must not be negative" }
    }

    private val lock = Any()
    private val shards = mutableListOf<Shard>()

    /** @param keysOf the keys an event belongs to; it is delivered when any of them is observed */
    fun <E> observe(keys: Set<Key>, events: Service.() -> Flow<E>, keysOf: (E) -> Collection<Key>): Flow<E> = flow {
        val services = acquire(keys)
        try {
            emitAll(
                services.map { it.events() }
                    .merge()
                    .filter { event -> keysOf(event).any(keys::contains) },
            )
        } finally {
            release(keys)
        }
    }

    private fun acquire(keys: Set<Key>): List<Service> = synchronized(lock) {
        keys.map { key ->
            val shard = shards.firstOrNull { key in it.refCounts } ?: shardWithRoom()
            shard.removals.remove(key)?.cancel()
            shard.refCounts[key] = (shard.refCounts[key] ?: 0) + 1
            scheduleFlush(shard)
            shard
        }
            .distinct()
            .map { it.service }
    }

    private fun release(keys: Set<Key>) = synchronized(lock) {
        keys.forEach { key ->
            val shard = shards.firstOrNull { key in it.refCounts } ?: return@forEach
            val count = requireNotNull(shard.refCounts[key]) - 1
            shard.refCounts[key] = count
            if (count == 0) scheduleRemoval(shard, key)
        }
    }

    private fun shardWithRoom(): Shard = shards.firstOrNull { it.refCounts.size < maxKeysPerService }
        ?: Shard(index = shards.size).also(shards::add)

    private fun scheduleRemoval(shard: Shard, key: Key) {
        shard.removals.remove(key)?.cancel()
        shard.removals[key] = scope.launch {
            delay(unsubscribeDelay)
            val removal = coroutineContext.job
            synchronized(lock) {
                if (shard.removals[key] !== removal) return@synchronized
                shard.removals.remove(key)
                if (shard.refCounts[key] == 0) {
                    shard.refCounts.remove(key)
                    scheduleFlush(shard)
                }
            }
        }
    }

    private fun scheduleFlush(shard: Shard) {
        if (shard.flush != null) return

        shard.flush = scope.launch {
            delay(batchWindow)
            synchronized(lock) {
                shard.flush = null
                flush(shard)
            }
        }
    }

    private fun flush(shard: Shard) {
        val desired = shard.refCounts.keys
        val added = desired - shard.subscribed
        val removed = shard.subscribed - desired

        if (added.isNotEmpty() && shard.service.subscribe(added)) shard.subscribed += added
        if (removed.isNotEmpty() && shard.service.unsubscribe(removed)) shard.subscribed -= removed
    }

    private fun openKeys(shard: Shard): Set<Key> = synchronized(lock) {
        shard.refCounts.keys.toSet().also { shard.subscribed = it }
    }

    private inner class Shard(index: Int) {

        val refCounts = mutableMapOf<Key, Int>()
        val removals = mutableMapOf<Key, Job>()
        var subscribed: Set<Key> = emptySet()
        var flush: Job? = null
        val service: Service = newService(index) { openKeys(this) }
    }
}