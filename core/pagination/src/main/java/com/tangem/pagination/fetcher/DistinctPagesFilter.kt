package com.tangem.pagination.fetcher

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Drops items that an earlier page of the same pagination session already returned.
 *
 * Limit/offset and page-number APIs rank items live, so an item whose rank shifts between two requests
 * comes back on both pages. Lists key rows by id, so a repeated item breaks them.
 *
 * Call [startPage] when a request starts — before the network call — and filter its response with the
 * returned [Page]. A first page opens a new session; a reload does not wait for a cancelled request, so a
 * late response of the previous session writes into that session's keys and never hides items of the
 * new one.
 */
class DistinctPagesFilter<T, K : Any>(private val keySelector: (T) -> K) {

    private val sessionKeys = AtomicReference<MutableSet<K>>(ConcurrentHashMap.newKeySet())

    fun startPage(isFirstPage: Boolean): Page<T, K> {
        val keys = if (isFirstPage) {
            ConcurrentHashMap.newKeySet<K>().also(sessionKeys::set)
        } else {
            sessionKeys.get()
        }
        return Page(keys, keySelector)
    }

    class Page<T, K : Any> internal constructor(
        private val sessionKeys: MutableSet<K>,
        private val keySelector: (T) -> K,
    ) {
        /** Keeps the first occurrence of each key, within the page and across the session. */
        fun distinct(items: List<T>): List<T> = items.filter { sessionKeys.add(keySelector(it)) }
    }
}