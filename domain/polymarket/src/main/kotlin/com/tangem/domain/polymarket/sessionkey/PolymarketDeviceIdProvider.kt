package com.tangem.domain.polymarket.sessionkey

/** @property deviceName human-readable, 1 to 64 characters */
interface PolymarketDeviceIdProvider {

    val deviceName: String

    /** @return 1 to 128 characters, the same for every wallet on the device */
    suspend fun deviceId(): String
}