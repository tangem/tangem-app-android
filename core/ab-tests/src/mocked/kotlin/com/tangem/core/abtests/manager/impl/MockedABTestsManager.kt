package com.tangem.core.abtests.manager.impl

import com.tangem.core.abtests.manager.ABTestsManager

/**
 * Mocked builds have no experiments backend: a UI test assigns a variant by setting the `uitest.abtests.<key>` system
 * property in the app process, and every other key stays unassigned.
 */
internal class MockedABTestsManager : ABTestsManager by StubABTestsManager() {

    override suspend fun getValue(key: String, defaultValue: String): String = peekValue(key) ?: defaultValue

    override fun peekValue(key: String): String? = System.getProperty("$PROPERTY_PREFIX$key")

    private companion object {
        const val PROPERTY_PREFIX = "uitest.abtests."
    }
}