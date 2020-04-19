package com.tangem.tangemtest.commons

/**
[REDACTED_AUTHOR]
 */
interface Store<M> {
    fun save(value: M)
    fun restore(): M
}

interface KeyedStore<M> {
    fun save(key: String, value: M)
    fun restore(key: String): M
    fun restoreAll(): MutableMap<String, M>
    fun delete(key: String)
}