package com.tangem.core.abtests.manager

interface ABTestsManager {

    fun init()

    fun setUserProperties(userId: String?, batch: String?, productType: String?, firmware: String?)

    fun removeUserProperties()

    suspend fun getValue(key: String, defaultValue: String): String

    /** The value already on the device: never waits for a fetch and tracks no exposure. Null if there is none yet. */
    fun peekValue(key: String): String?
}