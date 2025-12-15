package com.tangem.core.abtests.manager.impl

import com.tangem.core.abtests.manager.ABTestsManager

internal class StubABTestsManager : ABTestsManager {

    override fun init() {
        // intentionally do nothing
    }

    override fun setUserProperties(userId: String?, batch: String?, productType: String?, firmware: String?) {
        // intentionally do nothing
    }

    override fun removeUserProperties() {
        // intentionally do nothing
    }

    override fun getValue(key: String, defaultValue: String): String {
        return defaultValue
    }
}