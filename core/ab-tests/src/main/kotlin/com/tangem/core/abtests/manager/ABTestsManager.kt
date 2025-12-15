package com.tangem.core.abtests.manager

interface ABTestsManager {

    fun init()

    fun setUserProperties(userId: String?, batch: String?, productType: String?, firmware: String?)

    fun removeUserProperties()

    fun getValue(key: String, defaultValue: String): String
}