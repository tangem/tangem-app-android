package com.tangem.tangemtest._arch.structure

/**
[REDACTED_AUTHOR]
 */
typealias Payload = MutableMap<String, Any?>

interface PayloadHolder {
    val payload: Payload

    fun get(key: String): Any? = payload[key]
    fun remove(key: String): Any? = payload.remove(key)
    fun set(key: String, value: Any?) {
        payload[key] = value
    }
}