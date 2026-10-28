package com.tangem.data.polymarket.sessionkey

import com.tangem.common.services.secure.SecureStorage

internal class InMemorySecureStorage : SecureStorage {

    val entries = mutableMapOf<String, String>()
    var failWrites = false
    var failReads = false
    var failDeletes = false

    override fun store(key: String, value: String) {
        if (failWrites) throw IllegalStateException("Keystore is unavailable")
        entries[key] = value
    }

    override fun store(data: ByteArray, account: String) = store(account, data.decodeToString())

    override fun storeKey(key: ByteArray, account: String) = store(key, account)

    override fun getAsString(key: String): String? {
        if (failReads) throw IllegalStateException("Keystore is locked")
        return entries[key]
    }

    override fun get(account: String): ByteArray? = entries[account]?.encodeToByteArray()

    override fun delete(account: String) {
        if (failDeletes) throw IllegalStateException("Keystore is unavailable")
        entries.remove(account)
    }
}